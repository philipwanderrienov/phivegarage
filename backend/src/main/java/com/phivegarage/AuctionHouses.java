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
 public record Input(@NotBlank @Size(max=120) String name,@PositiveOrZero @Max(100000000000L) long adminFee,
  @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=4) BigDecimal taxPercent,boolean active) {}
 Map<String,Object> snapshot(UUID id){var h=store.one("SELECT * FROM auction_houses WHERE id=?",id);
  if(!Boolean.TRUE.equals(h.get("active")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Balai tidak aktif");
  return Map.of("name",h.get("name"),"adminFee",h.get("admin_fee"),"taxPercent",h.get("tax_percent"),"taxBasis","BID");
 }
 static Criteria withFees(Criteria c,JsonNode fee){
  if(fee==null||!fee.has("adminFee")||!fee.has("taxPercent"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Pilih balai lelang untuk katalog ini terlebih dahulu");
  return new Criteria(c.capital(),c.maxPerUnit(),c.maxBid(),c.targetProfit(),fee.path("adminFee").asLong(),fee.path("taxPercent").decimalValue(),
   c.repairBuffer(),c.taxBuffer(),c.otherCosts(),c.riskBuffer(),c.minYear(),c.maxKilometer(),c.transmission(),c.buyer(),c.strategy(),true,c.requireBpkb(),c.instructions());
 }
 static Map<String,Object> cost(long bid,JsonNode fee){
  if(fee==null)return Map.of("available",false);
  long fixed=fee.path("adminFee").asLong();BigDecimal rate=fee.path("taxPercent").decimalValue();
  long totalFee=BidCalculator.fee(bid,fixed,rate);
  return Map.of("available",true,"bid",bid,"adminFee",fixed,"taxPercent",rate,"auctionTax",totalFee-fixed,"auctionAllIn",Math.addExact(bid,totalFee));
 }
}
@RestController
@RequestMapping("/api/auction-houses")
class HouseApi {
 final Store store;
 HouseApi(Store store){this.store=store;}
 @GetMapping Object list(){return store.list("SELECT * FROM auction_houses ORDER BY lower(name)");}
 @PostMapping Object create(@Valid @RequestBody AuctionHouses.Input input){UUID id=UUID.randomUUID();store.db.update("INSERT INTO auction_houses(id,name,admin_fee,tax_percent,active) VALUES(?,?,?,?,?)",id,input.name().trim(),input.adminFee(),input.taxPercent(),input.active());return Map.of("id",id);}
 @PutMapping("/{id}") Object update(@PathVariable UUID id,@Valid @RequestBody AuctionHouses.Input input){store.one("SELECT id FROM auction_houses WHERE id=?",id);store.db.update("UPDATE auction_houses SET name=?,admin_fee=?,tax_percent=?,active=?,updated_at=now() WHERE id=?",input.name().trim(),input.adminFee(),input.taxPercent(),input.active(),id);return Map.of("saved",true);}
 @DeleteMapping("/{id}") Object delete(@PathVariable UUID id){store.one("SELECT id FROM auction_houses WHERE id=?",id);
  try{store.db.update("DELETE FROM auction_houses WHERE id=?",id);}catch(org.springframework.dao.DataIntegrityViolationException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Balai sudah dipakai katalog atau unit dimenangkan. Nonaktifkan balai agar histori tetap tersedia.");}return Map.of("deleted",true);
 }
}
