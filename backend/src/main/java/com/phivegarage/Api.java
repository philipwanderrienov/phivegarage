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
 final Store store; final Jobs jobs;final AiClient ai;
 public Api(Store store,Jobs jobs,AiClient ai){this.store=store;this.jobs=jobs;this.ai=ai;}
 @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP");}
 @GetMapping("/config") public Map<String,Object> config(){return Map.of("aiConfigured",ai.enabled(),"model",ai.model());}
 @GetMapping("/catalogs") public Object catalogs(){return store.list("SELECT c.id,c.name,c.house,c.status,c.progress,c.pages,c.error,c.created_at,count(l.id) AS total_lots FROM catalogs c LEFT JOIN lots l ON l.catalog_id=c.id GROUP BY c.id ORDER BY c.created_at DESC");}
 @PostMapping("/catalogs") public Object upload(@RequestPart MultipartFile file,@RequestParam(defaultValue="Lelang") String house)throws Exception{
  if(!ai.enabled())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"OPENAI_API_KEY belum dikonfigurasi");
  if(file.isEmpty()||file.getSize()>20*1024*1024)throw new IllegalArgumentException("PDF maksimal 20 MB");
  byte[] bytes=file.getBytes();if(bytes.length<5||!new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))throw new IllegalArgumentException("File harus PDF valid");
  try(var doc=Loader.loadPDF(bytes)){if(doc.isEncrypted()||doc.getNumberOfPages()==0||doc.getNumberOfPages()>150)throw new IllegalArgumentException("PDF tidak boleh terenkripsi dan maksimal 150 halaman");}
  UUID id=UUID.randomUUID();String path=id+".pdf";Files.write(jobs.storage.resolve(path),bytes);
  String name=Optional.ofNullable(file.getOriginalFilename()).orElse("catalog.pdf");
  store.db.update("INSERT INTO catalogs(id,name,house,file_path,status) VALUES(?,?,?,?,'QUEUED')",id,name,house,path);
  try{jobs.submit(()->jobs.extract(id));}catch(Exception e){store.db.update("DELETE FROM catalogs WHERE id=?",id);Files.deleteIfExists(jobs.storage.resolve(path));throw e;}
  return Map.of("id",id);
 }
 @GetMapping("/catalogs/{id}") public Object catalog(@PathVariable UUID id){return store.one("SELECT id,name,house,status,progress,pages,error,created_at FROM catalogs WHERE id=?",id);}
 @GetMapping("/catalogs/{id}/pdf") public ResponseEntity<FileSystemResource> pdf(@PathVariable UUID id){var c=store.one("SELECT file_path FROM catalogs WHERE id=?",id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Content-Disposition","inline; filename=catalog.pdf").body(new FileSystemResource(jobs.storage.resolve(c.get("file_path").toString())));}
 @GetMapping("/catalogs/{id}/lots") public Object lots(@PathVariable UUID id){store.one("SELECT id FROM catalogs WHERE id=?",id);return store.list("SELECT * FROM lots WHERE catalog_id=? ORDER BY lot_number",id);}
 public record EditLot(@jakarta.validation.constraints.NotNull @Valid LotData data,boolean verified){}
 @PutMapping("/lots/{id}") public Object edit(@PathVariable UUID id,@Valid @RequestBody EditLot body){
  var lot=store.one("SELECT l.id,c.pages,c.id AS catalog_id FROM lots l JOIN catalogs c ON c.id=l.catalog_id WHERE l.id=?",id);
  int active=store.db.queryForObject("SELECT count(*) FROM analyses WHERE catalog_id=? AND status IN ('QUEUED','PROCESSING')",Integer.class,lot.get("catalog_id"));
  if(active>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Tunggu analisis selesai sebelum mengubah lot");
  Jobs.validateLot(body.data(),((Number)lot.get("pages")).intValue());
  store.db.update("UPDATE lots SET data=?::jsonb,lot_number=?,verified=? WHERE id=?",store.encode(body.data()),body.data().lotNumber(),body.verified(),id);return Map.of("saved",true);
 }
 @PostMapping("/catalogs/{id}/analyses") public Object analyze(@PathVariable UUID id,@Valid @RequestBody Criteria c){
  var catalog=store.one("SELECT status FROM catalogs WHERE id=?",id);if(!"READY".equals(catalog.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Katalog belum siap");
  if(Math.max(c.capital(),c.maxPerUnit())>100_000_000_000L)throw new IllegalArgumentException("Modal di luar rentang");
  UUID run=UUID.randomUUID();var snapshot=store.list("SELECT id,lot_number,data,verified FROM lots WHERE catalog_id=? ORDER BY lot_number",id);store.db.update("INSERT INTO analyses(id,catalog_id,status,parameters,input_lots) VALUES(?,?,'QUEUED',?::jsonb,?::jsonb)",run,id,store.encode(c),store.encode(snapshot));
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
