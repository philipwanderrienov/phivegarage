package com.phivegarage;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/won-units")
public class WonApi {
 final Store store;final AuctionHouses houses;final Jobs jobs;final ObjectMapper json;
 public WonApi(Store store,AuctionHouses houses,Jobs jobs,ObjectMapper json){this.store=store;this.houses=houses;this.jobs=jobs;this.json=json;}
 public record UnitData(@NotBlank @Size(max=300) String vehicle,@Min(1950) @Max(2100) Integer year,String lotNumber,
  @PositiveOrZero Long kilometer,String plate,@PositiveOrZero @Max(100000000000L) Long basePrice,
  @Positive @Max(100000000000L) Long idealBid,@Positive @Max(100000000000L) long winningBid,@NotNull LocalDate wonDate,
  @Positive @Max(100000000000L) Long medianPrice,@Positive @Max(100000000000L) Long targetPrice,
  @Positive @Max(100000000000L) Long dealPrice,LocalDate soldDate,@Size(max=5000) String notes,String link){}
 public record SaveUnit(UUID houseId,UUID sourceLotId,@NotNull @Valid UnitData data){}
 public record Expense(@NotNull LocalDate date,@NotBlank @Size(max=60) String category,@NotBlank @Size(max=1000) String description,@Positive @Max(100000000000L) long amount){}
 public record Funds(@PositiveOrZero @Max(100000000000L) long startingFunds){}
 static void validate(UnitData d){if((d.dealPrice()==null)!=(d.soldDate()==null))throw new IllegalArgumentException("Harga deal dan tanggal terjual harus diisi bersama");if(d.soldDate()!=null&&d.soldDate().isBefore(d.wonDate()))throw new IllegalArgumentException("Tanggal jual tidak boleh sebelum tanggal menang");}
 Map<String,Object> decorate(Map<String,Object> row){
  var d=store.decode(row.get("data").toString(),UnitData.class);var fee=(JsonNode)row.get("fee_snapshot");
  long expense=store.db.queryForObject("SELECT COALESCE(sum(amount),0) FROM unit_expenses WHERE unit_id=?",Long.class,row.get("id"));
  row.put("summary",WonCalculator.calculate(d.winningBid(),d.idealBid(),fee.path("adminFee").asLong(),fee.path("taxPercent").decimalValue(),expense,d.targetPrice(),d.dealPrice()));return row;
 }
 @GetMapping Object list(){return store.list("SELECT * FROM won_units ORDER BY created_at DESC").stream().map(this::decorate).toList();}
 @GetMapping("/funds") Object funds(){long start=store.db.queryForObject("SELECT starting_funds FROM garage_settings WHERE id=1",Long.class),profit=0,held=0;
  var rows=store.list("SELECT * FROM won_units ORDER BY created_at");for(var row:rows){var summary=(WonCalculator.Summary)decorate(row).get("summary");if(summary.realMargin()!=null)profit=Math.addExact(profit,summary.realMargin());else held=Math.addExact(held,summary.totalCost());}
  return Map.of("startingFunds",start,"realizedProfit",profit,"funds",Math.addExact(start,profit),"unsoldCapital",held);
 }
 @PutMapping("/funds") Object funds(@Valid @RequestBody Funds body){store.db.update("UPDATE garage_settings SET starting_funds=? WHERE id=1",body.startingFunds());return Map.of("saved",true);}
 @GetMapping("/{id}") Object get(@PathVariable UUID id){var row=decorate(store.one("SELECT * FROM won_units WHERE id=?",id));row.put("expenses",store.list("SELECT * FROM unit_expenses WHERE unit_id=? ORDER BY expense_date,id",id));return row;}
 @PostMapping Object create(@Valid @RequestBody SaveUnit body){validate(body.data());if(body.houseId()==null)throw new IllegalArgumentException("Pilih balai; unit seller langsung dicatat dari menu Keputusan beli");Map<String,Object> fee;
  if(body.sourceLotId()!=null){var source=store.one("SELECT c.auction_house_id,c.fee_snapshot FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE l.id=?",body.sourceLotId());if(!body.houseId().equals(source.get("auction_house_id")))throw new IllegalArgumentException("Balai harus sesuai dengan katalog sumber");var node=(JsonNode)source.get("fee_snapshot");if(node==null)throw new IllegalArgumentException("Pilih tarif balai pada katalog sumber terlebih dahulu");fee=json.convertValue(node,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});}else fee=houses.snapshot(body.houseId());
  UUID id=UUID.randomUUID();store.db.update("INSERT INTO won_units(id,source_lot_id,auction_house_id,data,fee_snapshot) VALUES(?,?,?,?::jsonb,?::jsonb)",id,body.sourceLotId(),body.houseId(),store.encode(body.data()),store.encode(fee));return Map.of("id",id);
 }
 @PutMapping("/{id}") Object update(@PathVariable UUID id,@Valid @RequestBody SaveUnit body){validate(body.data());var row=store.one("SELECT * FROM won_units WHERE id=?",id);
  if(!Objects.equals(body.houseId(),row.get("auction_house_id"))||!Objects.equals(body.sourceLotId(),row.get("source_lot_id")))throw new IllegalArgumentException("Balai/sumber unit tidak dapat diubah; histori tarif harus tetap tersimpan");
  store.db.update("UPDATE won_units SET data=?::jsonb WHERE id=?",store.encode(body.data()),id);return Map.of("saved",true);
 }
 @DeleteMapping("/{id}") Object delete(@PathVariable UUID id){store.one("SELECT id FROM won_units WHERE id=?",id);store.db.update("DELETE FROM won_units WHERE id=?",id);return Map.of("deleted",true);}
 @PostMapping("/{id}/expenses") Object addExpense(@PathVariable UUID id,@Valid @RequestBody Expense body){store.one("SELECT id FROM won_units WHERE id=?",id);UUID expense=UUID.randomUUID();store.db.update("INSERT INTO unit_expenses(id,unit_id,expense_date,category,description,amount) VALUES(?,?,?,?,?,?)",expense,id,body.date(),body.category(),body.description(),body.amount());return Map.of("id",expense);}
 @PutMapping("/{id}/expenses/{expenseId}") Object editExpense(@PathVariable UUID id,@PathVariable UUID expenseId,@Valid @RequestBody Expense body){int changed=store.db.update("UPDATE unit_expenses SET expense_date=?,category=?,description=?,amount=? WHERE id=? AND unit_id=?",body.date(),body.category(),body.description(),body.amount(),expenseId,id);if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Pengeluaran tidak ditemukan");return Map.of("saved",true);}
 @DeleteMapping("/{id}/expenses/{expenseId}") Object deleteExpense(@PathVariable UUID id,@PathVariable UUID expenseId){if(store.db.update("DELETE FROM unit_expenses WHERE id=? AND unit_id=?",expenseId,id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Pengeluaran tidak ditemukan");return Map.of("deleted",true);}
}
