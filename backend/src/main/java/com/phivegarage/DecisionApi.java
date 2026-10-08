package com.phivegarage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.nio.file.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/purchase-decisions")
public class DecisionApi {
 final Store store;final AuctionHouses houses;final Jobs jobs;final AiClient ai;final DecisionJobs decisionJobs;
 public DecisionApi(Store store,AuctionHouses houses,Jobs jobs,AiClient ai,DecisionJobs decisionJobs){this.store=store;this.houses=houses;this.jobs=jobs;this.ai=ai;this.decisionJobs=decisionJobs;}
 public record Input(@NotBlank @Size(max=300) String vehicle,@Min(1950) @Max(2100) Integer year,@PositiveOrZero @Max(100000000000L) Long kilometer,
  @Pattern(regexp="MT|AT|UNKNOWN") @NotNull String transmission,@Pattern(regexp="ADA|TIDAK_ADA|UNKNOWN") @NotNull String stnk,@Pattern(regexp="ADA|TIDAK_ADA|UNKNOWN") @NotNull String bpkb,
  @Positive @Max(100000000000L) long askPrice,@Positive @Max(100000000000L) long capital,@Positive @Max(100000000000L) long maxPurchase,
  @PositiveOrZero @Max(100000000000L) long targetProfit,@PositiveOrZero @Max(100000000000L) long repairLow,@PositiveOrZero @Max(100000000000L) long repairHigh,
  @PositiveOrZero @Max(100000000000L) long taxCost,@PositiveOrZero @Max(100000000000L) long otherCosts,@PositiveOrZero @Max(100000000000L) long riskBuffer,
  @Positive @Max(100000000000L) long saleLow,@Positive @Max(100000000000L) long saleMid,@Positive @Max(100000000000L) long saleHigh,
  boolean evidenceConfirmed,boolean inspectionConfirmed,boolean criticalRisk,@Size(max=5000) String notes,@Size(max=5000) String marketEvidence,
  @Pattern(regexp="DIRECT|AUCTION") @NotNull String purchaseType,UUID houseId,UUID sourceLotId){}
 public record Acquire(@Positive @Max(100000000000L) long price,@NotNull LocalDate date){}
 static void validate(Input d){if(d.evidenceConfirmed()&&(d.marketEvidence()==null||d.marketEvidence().isBlank()))throw new IllegalArgumentException("Rincian pembanding harga wajib diisi saat ditandai terverifikasi");if(d.repairHigh()<d.repairLow()||d.saleLow()>d.saleMid()||d.saleMid()>d.saleHigh())throw new IllegalArgumentException("Rentang biaya dan harga jual harus berurutan dari rendah ke tinggi");if("DIRECT".equals(d.purchaseType())&&(d.houseId()!=null||d.sourceLotId()!=null))throw new IllegalArgumentException("Pembelian langsung tidak memakai balai atau lot lelang");if("AUCTION".equals(d.purchaseType())&&d.houseId()==null)throw new IllegalArgumentException("Pilih balai untuk unit lelang");}
 @GetMapping Object list(){return store.list("SELECT p.*, (SELECT id FROM won_units w WHERE w.source_decision_id=p.id) AS acquired_unit_id FROM purchase_decisions p ORDER BY created_at DESC");}
 @GetMapping("/{id}") Object get(@PathVariable UUID id){var row=store.one("SELECT p.*, (SELECT id FROM won_units w WHERE w.source_decision_id=p.id) AS acquired_unit_id FROM purchase_decisions p WHERE p.id=?",id);row.put("photos",store.list("SELECT id,mime_type FROM decision_photos WHERE decision_id=?",id));return row;}
 @PostMapping Object create(@Valid @RequestPart("input") Input body,@RequestPart(value="photos",required=false) List<MultipartFile> photos)throws Exception{
  validate(body);if(photos!=null&&photos.size()>6)throw new IllegalArgumentException("Maksimal 6 foto");
  var images=new ArrayList<DecisionJobs.Photo>();if(photos!=null)for(var photo:photos){if(photo.isEmpty()||photo.getSize()>3*1024*1024)throw new IllegalArgumentException("Foto JPG/PNG maksimal 3 MB per file");images.add(DecisionJobs.photo(photo.getBytes()));}
  Map<String,Object> fee;
  if("DIRECT".equals(body.purchaseType()))fee=Map.of("name","Seller langsung","adminFee",0,"taxPercent",0);
  else if(body.sourceLotId()!=null){var source=store.one("SELECT c.auction_house_id,c.fee_snapshot FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE l.id=?",body.sourceLotId());if(!body.houseId().equals(source.get("auction_house_id")))throw new IllegalArgumentException("Balai harus sesuai katalog");if(source.get("fee_snapshot")==null)throw new IllegalArgumentException("Tarif katalog belum tersedia");fee=store.json.convertValue(source.get("fee_snapshot"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}
  else fee=houses.snapshot(body.houseId());
  var feeNode=store.json.valueToTree(fee);var report=DecisionCalculator.calculate(body,feeNode.path("adminFee").asLong(),feeNode.path("taxPercent").decimalValue());UUID id=UUID.randomUUID();var paths=new ArrayList<Path>();
  try{jobs.tx.executeWithoutResult(tx->{store.db.update("INSERT INTO purchase_decisions(id,source_lot_id,auction_house_id,data,fee_snapshot,results) VALUES(?,?,?,?::jsonb,?::jsonb,?::jsonb)",id,body.sourceLotId(),body.houseId(),store.encode(body),store.encode(fee),store.encode(report));for(var photo:images){var photoId=UUID.randomUUID();String file="decision-"+photoId+photo.extension();Path path=jobs.storage.resolve(file);paths.add(path);try{Files.write(path,photo.bytes());}catch(Exception e){throw new IllegalStateException("Foto gagal disimpan");}store.db.update("INSERT INTO decision_photos(id,decision_id,file_path,mime_type) VALUES(?,?,?,?)",photoId,id,file,photo.mime());}});}catch(Exception e){for(var path:paths)Files.deleteIfExists(path);throw e;}return Map.of("id",id);
 }
 @GetMapping("/{id}/photos/{photoId}") Object photo(@PathVariable UUID id,@PathVariable UUID photoId){var row=store.one("SELECT file_path,mime_type FROM decision_photos WHERE decision_id=? AND id=?",id,photoId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(row.get("mime_type").toString())).header("Content-Disposition","inline; filename=unit-photo").body(new FileSystemResource(jobs.storage.resolve(row.get("file_path").toString())));}
 @PostMapping("/{id}/ai") Object analyze(@PathVariable UUID id){if(!ai.enabled())throw new IllegalArgumentException("OPENAI_API_KEY belum dikonfigurasi");var row=store.one("SELECT data,results FROM purchase_decisions WHERE id=?",id);var d=store.json.convertValue(row.get("data"),Input.class);if(!"ADA".equals(d.stnk())||d.criticalRisk()||"SKIP".equals(((JsonNode)row.get("results")).path("recommendation").asText()))throw new IllegalArgumentException("Unit berstatus Lewati tidak dikirim ke AI; periksa STNK, risiko, modal dan parameter harga");if(store.db.update("UPDATE purchase_decisions SET ai_status='QUEUED',error=NULL,ai=NULL,usage='{}' WHERE id=? AND ai_status NOT IN ('QUEUED','PROCESSING')",id)!=1)throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"Analisis AI masih berjalan");try{jobs.submit(()->decisionJobs.analyze(id));}catch(Exception e){store.db.update("UPDATE purchase_decisions SET ai_status='FAILED',error='Antrean penuh; coba lagi.' WHERE id=?",id);throw e;}return Map.of("id",id);}
 @PostMapping("/{id}/acquire") Object acquire(@PathVariable UUID id,@Valid @RequestBody Acquire body){var row=store.one("SELECT * FROM purchase_decisions WHERE id=?",id);var d=store.json.convertValue(row.get("data"),Input.class);UUID unit=UUID.randomUUID();String lotNumber=d.sourceLotId()==null?"":store.one("SELECT lot_number FROM lots WHERE id=?",d.sourceLotId()).get("lot_number").toString();var report=store.json.convertValue(row.get("results"),DecisionCalculator.Report.class);
  var data=new WonApi.UnitData(d.vehicle(),d.year(),lotNumber,d.kilometer(),"",d.askPrice(),report.maxPurchase()>0?report.maxPurchase():null,body.price(),body.date(),d.saleMid(),d.saleMid(),null,null,d.notes(),"");
  store.db.update("INSERT INTO won_units(id,source_lot_id,source_decision_id,auction_house_id,data,fee_snapshot) VALUES(?,?,?,?,?::jsonb,?::jsonb)",unit,d.sourceLotId(),id,d.houseId(),store.encode(data),row.get("fee_snapshot").toString());return Map.of("id",unit);
 }
}
