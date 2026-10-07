package com.phivegarage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import jakarta.validation.Valid;
import java.util.*;
import java.nio.file.*;
import org.apache.pdfbox.Loader;
import static com.phivegarage.Models.*;
@RestController
@RequestMapping("/api")
public class Api {
 final Store store; final Jobs jobs;final AiClient ai;final AuctionHouses houses;
 public Api(Store store,Jobs jobs,AiClient ai,AuctionHouses houses){this.store=store;this.jobs=jobs;this.ai=ai;this.houses=houses;}
 @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP");}
 @GetMapping("/config") public Map<String,Object> config(){return Map.of("aiConfigured",ai.enabled(),"model",ai.model());}
 @GetMapping("/catalogs") public Object catalogs(){return store.list("SELECT c.id,c.name,c.house,c.status,c.progress,c.pages,c.error,c.created_at,c.auction_house_id,c.fee_snapshot,count(l.id) AS total_lots FROM catalogs c LEFT JOIN lots l ON l.catalog_id=c.id GROUP BY c.id ORDER BY c.created_at DESC");}
 @PostMapping("/catalogs") public Object upload(@RequestPart MultipartFile file,@RequestParam UUID houseId)throws Exception{
  if(!ai.enabled())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"OPENAI_API_KEY belum dikonfigurasi");
  if(file.isEmpty()||file.getSize()>20*1024*1024)throw new IllegalArgumentException("PDF maksimal 20 MB");
  byte[] bytes=file.getBytes();PdfFiles.validate(bytes);
  var fee=houses.snapshot(houseId);
  UUID id=UUID.randomUUID();String path=id+".pdf";Files.write(jobs.storage.resolve(path),bytes);
  String name=Optional.ofNullable(file.getOriginalFilename()).orElse("catalog.pdf");
  store.db.update("INSERT INTO catalogs(id,name,house,file_path,status,auction_house_id,fee_snapshot) VALUES(?,?,?,?,'QUEUED',?,?::jsonb)",id,name,fee.get("name"),path,houseId,store.encode(fee));
  try{jobs.submit(()->jobs.extract(id));}catch(Exception e){store.db.update("DELETE FROM catalogs WHERE id=?",id);Files.deleteIfExists(jobs.storage.resolve(path));throw e;}
  return Map.of("id",id);
 }
 @PostMapping("/catalogs/{id}/retry") public Object retry(@PathVariable UUID id){
  if(!ai.enabled())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"OPENAI_API_KEY belum dikonfigurasi");
  var catalog=store.one("SELECT file_path,status FROM catalogs WHERE id=?",id);
  if(!Files.isRegularFile(jobs.storage.resolve(catalog.get("file_path").toString())))throw new ResponseStatusException(HttpStatus.CONFLICT,"PDF sumber tidak ditemukan. Upload ulang katalog.");
  int claimed=store.db.update("UPDATE catalogs SET status='QUEUED',progress=0,error=NULL WHERE id=? AND status='FAILED'",id);
  if(claimed!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Hanya katalog gagal yang dapat dicoba ulang");
  try{jobs.submit(()->jobs.extract(id));}catch(Exception e){store.db.update("UPDATE catalogs SET status='FAILED',error='Antrean penuh; coba kembali nanti.' WHERE id=?",id);throw e;}
  return Map.of("id",id);
 }
 @GetMapping("/catalogs/{id}") public Object catalog(@PathVariable UUID id){return store.one("SELECT id,name,house,status,progress,pages,error,created_at,auction_house_id,fee_snapshot FROM catalogs WHERE id=?",id);}
 @GetMapping("/catalogs/{id}/pdf") public ResponseEntity<FileSystemResource> pdf(@PathVariable UUID id){var c=store.one("SELECT file_path FROM catalogs WHERE id=?",id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Content-Disposition","inline; filename=catalog.pdf").body(new FileSystemResource(jobs.storage.resolve(c.get("file_path").toString())));}
 @GetMapping("/catalogs/{id}/lots") public Object lots(@PathVariable UUID id){var c=store.one("SELECT fee_snapshot FROM catalogs WHERE id=?",id);var rows=store.list("SELECT * FROM lots WHERE catalog_id=? ORDER BY lot_number",id);
  for(var row:rows){var data=(com.fasterxml.jackson.databind.JsonNode)row.get("data");var price=data.path("basePrice");row.put("auctionCost",price.isIntegralNumber()?AuctionHouses.cost(price.asLong(),(com.fasterxml.jackson.databind.JsonNode)c.get("fee_snapshot")):Map.of("available",false));}return rows;
 }
 public record SelectHouse(@jakarta.validation.constraints.NotNull UUID houseId){}
 @PutMapping("/catalogs/{id}/house") public Object selectHouse(@PathVariable UUID id,@Valid @RequestBody SelectHouse body){var fee=houses.snapshot(body.houseId());store.one("SELECT id FROM catalogs WHERE id=?",id);
  store.db.update("UPDATE catalogs SET auction_house_id=?,house=?,fee_snapshot=?::jsonb WHERE id=?",body.houseId(),fee.get("name"),store.encode(fee),id);return Map.of("saved",true);
 }
 public record Preview(@jakarta.validation.constraints.PositiveOrZero @jakarta.validation.constraints.Max(100000000000L) long bid){}
 @PostMapping("/catalogs/{id}/cost-preview") public Object preview(@PathVariable UUID id,@Valid @RequestBody Preview body){var c=store.one("SELECT fee_snapshot FROM catalogs WHERE id=?",id);return AuctionHouses.cost(body.bid(),(com.fasterxml.jackson.databind.JsonNode)c.get("fee_snapshot"));}

 public record EditLot(@jakarta.validation.constraints.NotNull @Valid LotData data,boolean verified){}
 @PutMapping("/lots/{id}") public Object edit(@PathVariable UUID id,@Valid @RequestBody EditLot body){
  var lot=store.one("SELECT l.id,c.pages,c.id AS catalog_id FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE l.id=?",id);
  int active=store.db.queryForObject("SELECT count(*) FROM analyses WHERE catalog_id=? AND status IN ('QUEUED','PROCESSING')",Integer.class,lot.get("catalog_id"));
  if(active>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Tunggu analisis selesai sebelum mengubah lot");
  Jobs.validateLot(body.data(),((Number)lot.get("pages")).intValue());
  store.db.update("UPDATE lots SET data=?::jsonb,lot_number=?,verified=? WHERE id=?",store.encode(body.data()),body.data().lotNumber(),body.verified(),id);return Map.of("saved",true);
 }
 @PostMapping("/catalogs/{id}/analyses") public Object analyze(@PathVariable UUID id,@Valid @RequestBody Criteria c){
  var catalog=store.one("SELECT status,fee_snapshot FROM catalogs WHERE id=?",id);if(!"READY".equals(catalog.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Katalog belum siap");
  if(Math.max(c.capital(),c.maxPerUnit())>100_000_000_000L)throw new IllegalArgumentException("Modal di luar rentang");
  var effective=AuctionHouses.withFees(c,(com.fasterxml.jackson.databind.JsonNode)catalog.get("fee_snapshot"));
  UUID run=UUID.randomUUID();var snapshot=store.list("SELECT id,lot_number,data,verified FROM lots WHERE catalog_id=? ORDER BY lot_number",id);store.db.update("INSERT INTO analyses(id,catalog_id,status,parameters,input_lots) VALUES(?,?,'QUEUED',?::jsonb,?::jsonb)",run,id,store.encode(effective),store.encode(snapshot));
  try{jobs.submit(()->jobs.analyze(run));}catch(Exception e){store.db.update("DELETE FROM analyses WHERE id=?",run);throw e;}return Map.of("id",run);
 }
 @GetMapping("/catalogs/{id}/analyses") public Object history(@PathVariable UUID id){return store.list("SELECT * FROM analyses WHERE catalog_id=? ORDER BY created_at DESC",id);}
 @GetMapping("/analyses/{id}") public Object analysis(@PathVariable UUID id){return store.one("SELECT * FROM analyses WHERE id=?",id);}
 @PatchMapping("/analyses/{id}/watchlist/{lotId}") public Object watch(@PathVariable UUID id,@PathVariable String lotId,@RequestBody Map<String,Boolean> body){
  Boolean desired=body.get("watchlisted");if(desired==null)throw new IllegalArgumentException("watchlisted wajib diisi");
  return jobs.tx.execute(status->{
   var a=store.one("SELECT * FROM analyses WHERE id=? FOR UPDATE",id);
   var results=(com.fasterxml.jackson.databind.node.ArrayNode)a.get("results");boolean found=false;
   for(var item:results)if(lotId.equals(item.path("lotId").asText())){((com.fasterxml.jackson.databind.node.ObjectNode)item).put("watchlisted",desired);found=true;}
   if(!found)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Lot tidak ditemukan pada analisis");
   store.db.update("UPDATE analyses SET results=?::jsonb WHERE id=?",store.encode(results),id);return Map.of("saved",true);
  });
 }
}
