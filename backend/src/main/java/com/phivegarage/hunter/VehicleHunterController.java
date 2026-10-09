package com.phivegarage.hunter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hunter")
public class VehicleHunterController {
 private final JdbcTemplate db;
 public VehicleHunterController(JdbcTemplate db) { this.db=db; }
 public record ListingInput(String source,String sourceUrl,String title,String brand,String model,Integer year,
     Long askingPrice,Integer kilometer,String location,String transmission,String stnk,String bpkb,String notes) {}
 public record ComparableInput(String sourceUrl,Long price,Boolean soldConfirmed) {}
 public record Strategy(Long budget,Long targetProfit,Long repairCost,Long taxCost,Long transportCost,
     Long riskBuffer,String buyer,Double discountPercent) {}
 private long positive(Long value,long fallback) { return value==null?fallback:Math.max(0,value); }
 private String status(String v) { return Set.of("ADA","TIDAK_ADA").contains(v) ? v:"UNKNOWN"; }
 private String source(String value) {
   return Set.of("FACEBOOK_MANUAL","OLX_MANUAL","AUCTION_MANUAL","OTHER_MANUAL","LICENSED_FEED").contains(value)
     ? value:"OTHER_MANUAL";
 }
 @GetMapping("/listings")
 public List<Map<String,Object>> list(@RequestParam(required=false) String brand,@RequestParam(required=false) Long maxPrice) {
  StringBuilder sql=new StringBuilder("SELECT * FROM hunter_listings WHERE 1=1");
  List<Object> args=new ArrayList<>();
  if(brand!=null && !brand.isBlank()){sql.append(" AND lower(brand)=lower(?)");args.add(brand);}
  if(maxPrice!=null){sql.append(" AND asking_price<=?");args.add(maxPrice);}
  sql.append(" ORDER BY imported_at DESC LIMIT 500");
  return db.queryForList(sql.toString(),args.toArray());
 }
 @PostMapping("/listings")
 public Map<String,Object> add(@RequestBody ListingInput input) {
  if(input.title()==null||input.title().isBlank()||input.brand()==null||input.brand().isBlank()
     ||input.model()==null||input.model().isBlank()||input.askingPrice()==null||input.askingPrice()<=0)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nama, merek, model, dan harga positif wajib diisi");
  if(input.sourceUrl()!=null && !input.sourceUrl().isBlank() && !input.sourceUrl().matches("https?://.*"))
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Link sumber harus HTTP(S)");
  UUID id=UUID.randomUUID();
  db.update("INSERT INTO hunter_listings(id,source,source_url,title,brand,model,manufacture_year,asking_price,kilometer,location,transmission,stnk_status,bpkb_status,notes) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    id,source(input.source()),input.sourceUrl(),input.title().trim(),input.brand().trim(),input.model().trim(),
    input.year(),input.askingPrice(),input.kilometer(),input.location(),input.transmission(),status(input.stnk()),
    status(input.bpkb()),Objects.toString(input.notes(),""));
  return Map.of("id",id,"status","CREATED");
 }
 @PostMapping("/listings/{id}/comparables")
 public Map<String,Object> comparable(@PathVariable UUID id,@RequestBody ComparableInput input) {
  ensure(id);
  if(input.price()==null||input.price()<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Harga pembanding harus positif");
  UUID compId=UUID.randomUUID();
  db.update("INSERT INTO hunter_comparables(id,listing_id,source_url,price,sold_confirmed) VALUES(?,?,?,?,?)",
    compId,id,input.sourceUrl(),input.price(),Boolean.TRUE.equals(input.soldConfirmed()));
  return Map.of("id",compId);
 }
 private Map<String,Object> ensure(UUID id) {
  List<Map<String,Object>> rows=db.queryForList("SELECT * FROM hunter_listings WHERE id=?",id);
  if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Listing tidak ditemukan");
  return rows.get(0);
 }
 @GetMapping("/listings/{id}/evaluate")
 public Map<String,Object> evaluate(@PathVariable UUID id,
    @RequestParam long budget,
    @RequestParam long targetProfit,
    @RequestParam long repairCost,
    @RequestParam long taxCost,
    @RequestParam long transportCost,
    @RequestParam long riskBuffer,
    @RequestParam(defaultValue="RETAIL") String buyer) {
  Map<String,Object> l=ensure(id);
  if(budget<=0||targetProfit<0||repairCost<0||taxCost<0||transportCost<0||riskBuffer<0)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Budget harus lebih besar dari nol; target profit dan biaya tidak boleh negatif");
  long ask=((Number)l.get("asking_price")).longValue();
  List<Long> comps=db.query("SELECT price FROM hunter_comparables WHERE listing_id=? ORDER BY price",
    (rs,row)->rs.getLong(1),id);
  long costs;
  try { costs=Math.addExact(Math.addExact(repairCost,taxCost),Math.addExact(transportCost,riskBuffer)); }
  catch(ArithmeticException e) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Biaya terlalu besar");}
  boolean hasComps=!comps.isEmpty();
  long median=hasComps?comps.get(comps.size()/2):0;
  double factor="DEALER".equalsIgnoreCase(buyer)?0.85:0.95;
  long quick=hasComps?BigDecimal.valueOf(comps.get(0)).multiply(BigDecimal.valueOf(factor)).setScale(0,RoundingMode.FLOOR).longValue():0;
  long maxBuy=hasComps?Math.max(0,Math.min(budget-costs,quick-targetProfit-costs)):0;
  long potential=hasComps?quick-ask-costs:0;
  boolean docs="ADA".equals(l.get("stnk_status")) && "ADA".equals(l.get("bpkb_status"));
  boolean inactive=Set.of("SOLD","REMOVED").contains(l.get("listing_status"));
  String decision=inactive|| (hasComps && (maxBuy<=0 || ask>budget))?"SKIP":
    !docs||!hasComps?"REVIEW":ask<=maxBuy?"BUY":"NEGO";
  List<String> reasons=new ArrayList<>();
  if(!hasComps) reasons.add("Belum ada data harga pembanding; harga pasar tidak boleh ditebak.");
  if(!docs) reasons.add("STNK dan BPKB harus dikonfirmasi ADA sebelum membeli.");
  if(inactive) reasons.add("Listing sudah SOLD/REMOVED; jangan dianggap tersedia.");
  if(hasComps && ask>maxBuy) reasons.add("Harga iklan melebihi harga beli maksimal untuk target laba.");
  reasons.add("Harga jual cepat adalah heuristik dari harga iklan terendah, bukan transaksi terkonfirmasi.");
  Map<String,Object> output=new LinkedHashMap<>();
  output.put("listing",l);output.put("decision",decision);output.put("askingPrice",ask);
  output.put("comparableCount",comps.size());output.put("medianAskingPrice",hasComps?median:null);
  output.put("quickSaleEstimate",hasComps?quick:null);output.put("maxBuyPrice",hasComps?maxBuy:null);
  output.put("estimatedProfitAtAsk",hasComps?potential:null);output.put("otherCosts",costs);
  output.put("targetProfit",targetProfit);output.put("budget",budget);output.put("reasons",reasons);
  output.put("confidence",!hasComps?"INSUFFICIENT":comps.size()<3?"LOW":"INDICATIVE");
  return output;
 }
}
