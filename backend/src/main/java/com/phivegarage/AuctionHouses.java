package com.phivegarage;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import static com.phivegarage.Models.*;
@Service
public class AuctionHouses {
 final Store store;
 public AuctionHouses(Store store){this.store=store;}
 public record Input(@NotBlank @Size(max=120) String name,@PositiveOrZero @Max(100000000000L) Long adminFee,
  @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=4) BigDecimal taxPercent,boolean active) {}
 Map<String,Object> snapshot(UUID id){var h=store.one("SELECT * FROM auction_houses WHERE id=?",id);
  if(!Boolean.TRUE.equals(h.get("active")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Balai belum aktif: periksa tarif admin dan pajak");
  String name=h.get("name").toString();String rule=feeRule(name);
  if(h.get("tax_percent")==null||("FIXED".equals(rule)&&h.get("admin_fee")==null))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Tarif balai belum lengkap");
  var fee=new LinkedHashMap<String,Object>();
  fee.put("name",name);fee.put("adminFee",h.get("admin_fee")==null?0:h.get("admin_fee"));
  fee.put("taxPercent",h.get("tax_percent"));fee.put("taxBasis","BID");fee.put("feeRule",rule);
  if(h.get("tariff_rule")!=null){var stored=(JsonNode)h.get("tariff_rule");fee.put("feeRule",stored.path("feeRule").asText("FIXED"));if("CUSTOM_TIERS".equals(stored.path("feeRule").asText())&&stored.path("tiers").isArray())fee.put("tiers",store.json.convertValue(stored.path("tiers"),List.class));}
  return fee;
 }
 static String feeRule(String name){return switch(name.trim().toLowerCase(java.util.Locale.ROOT)){
  case "carsome" -> "CARSOME_2026";
  case "otomas" -> "OTOMAS_2026";
  case "smartbid" -> "SMARTBID_2026";
  default -> "FIXED";
 };}
 static Criteria withFees(Criteria c,JsonNode fee){
  if(fee==null||!fee.has("adminFee")||!fee.has("taxPercent"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Pilih balai lelang untuk katalog ini terlebih dahulu");
  return new Criteria(c.capital(),c.maxPerUnit(),c.maxBid(),c.targetProfit(),fee.path("adminFee").asLong(),fee.path("taxPercent").decimalValue(),
   c.repairBuffer(),c.taxBuffer(),c.otherCosts(),c.riskBuffer(),c.minYear(),c.maxKilometer(),c.transmission(),c.buyer(),c.strategy(),true,c.requireBpkb(),c.instructions());
 }
 static Map<String,Object> cost(long bid,JsonNode fee){return AuctionFeeSchedule.preview(bid,fee);}

}
@RestController
@RequestMapping("/api/auction-houses")
class HouseApi {
 final Store store; final Jobs jobs;
 HouseApi(Store store,Jobs jobs){this.store=store;this.jobs=jobs;}
 static void checkTariff(AuctionHouses.Input input){if(input.active()&&(input.adminFee()==null||input.taxPercent()==null))throw new IllegalArgumentException("Isi tarif admin dan pajak sebelum mengaktifkan balai. Nol hanya jika biaya benar-benar gratis.");}
 @GetMapping Object list(){return store.list("SELECT * FROM auction_houses ORDER BY lower(name)");}
 @PostMapping Object create(@Valid @RequestBody AuctionHouses.Input input){checkTariff(input);UUID id=UUID.randomUUID();store.db.update("INSERT INTO auction_houses(id,name,admin_fee,tax_percent,active) VALUES(?,?,?,?,?)",id,input.name().trim(),input.adminFee(),input.taxPercent(),input.active());return Map.of("id",id);}
 @PutMapping("/{id}") Object update(@PathVariable UUID id,@Valid @RequestBody AuctionHouses.Input input){checkTariff(input);store.one("SELECT id FROM auction_houses WHERE id=?",id);store.db.update("UPDATE auction_houses SET name=?,admin_fee=?,tax_percent=?,active=?,tariff_rule=CASE WHEN admin_fee IS DISTINCT FROM ? OR tax_percent IS DISTINCT FROM ? THEN NULL ELSE tariff_rule END,updated_at=now() WHERE id=?",input.name().trim(),input.adminFee(),input.taxPercent(),input.active(),input.adminFee(),input.taxPercent(),id);return Map.of("saved",true);}
 public record DeletePreview(String name,long catalogs,long lots,long analyses,long decisions,long wonUnits){}
 @GetMapping("/{id}/delete-preview") Object deletePreview(@PathVariable UUID id){
  var house=store.one("SELECT name FROM auction_houses WHERE id=?",id);
  return preview(id,house.get("name").toString());
 }
 DeletePreview preview(UUID id,String name){
  Long catalogs=store.db.queryForObject("SELECT count(*) FROM catalogs WHERE auction_house_id=?",Long.class,id);
  Long lots=store.db.queryForObject("SELECT count(*) FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE c.auction_house_id=?",Long.class,id);
  Long analyses=store.db.queryForObject("SELECT count(*) FROM analyses a JOIN catalogs c ON c.id=a.catalog_id WHERE c.auction_house_id=?",Long.class,id);
  Long decisions=store.db.queryForObject("SELECT count(*) FROM purchase_decisions WHERE auction_house_id=?",Long.class,id);
  Long won=store.db.queryForObject("SELECT count(*) FROM won_units WHERE auction_house_id=?",Long.class,id);
  return new DeletePreview(name,catalogs,lots,analyses,decisions,won);
 }
 @DeleteMapping("/{id}") Object delete(@PathVariable UUID id){
  var pdfPaths=new ArrayList<java.nio.file.Path>();
  var report=jobs.tx.execute(status->{
   var house=store.one("SELECT name FROM auction_houses WHERE id=? FOR UPDATE",id);
   String name=house.get("name").toString();
   var impact=preview(id,name);
   var sourceFiles=store.list("SELECT file_path FROM catalogs WHERE auction_house_id=? FOR UPDATE",id);
   for(var file:sourceFiles)pdfPaths.add(jobs.storage.resolve(file.get("file_path").toString()).normalize());
   // A catalog is derived data and can be removed; accounting is not.
   // Keep actual acquired units and expense history with their immutable fee snapshots.
   // The source lot / decision references must be detached before cascade deleting lots.
   store.db.update("UPDATE won_units SET source_lot_id=NULL WHERE source_lot_id IN (SELECT l.id FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE c.auction_house_id=?)",id);
   store.db.update("UPDATE purchase_decisions SET source_lot_id=NULL WHERE source_lot_id IN (SELECT l.id FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE c.auction_house_id=?)",id);
   store.db.update("UPDATE won_units SET auction_house_id=NULL WHERE auction_house_id=?",id);
   store.db.update("UPDATE purchase_decisions SET auction_house_id=NULL WHERE auction_house_id=?",id);
   store.db.update("DELETE FROM catalogs WHERE auction_house_id=?",id);
   store.db.update("DELETE FROM auction_houses WHERE id=?",id);
   return impact;
  });
  // Files are removed only after a successful database commit.
  int removedFiles=0;
  for(var path:pdfPaths)try{
   if(path.startsWith(jobs.storage)&&java.nio.file.Files.deleteIfExists(path))removedFiles++;
  }catch(java.io.IOException ex){
   // Keep the API successful; orphaned files can be cleaned later.
  }
  return Map.of("deleted",true,"name",report.name(),"catalogsDeleted",report.catalogs(),
   "lotsDeleted",report.lots(),"analysesDeleted",report.analyses(),
   "decisionsPreserved",report.decisions(),"wonUnitsPreserved",report.wonUnits(),
   "pdfFilesDeleted",removedFiles);
 }
}
