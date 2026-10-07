package com.phivegarage;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Service
public class AiClient {
 private final ObjectMapper json; private final String key,model;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
 public AiClient(ObjectMapper json,@Value("${app.ai-key}") String key,@Value("${app.ai-model}") String model){this.json=json;this.key=key;this.model=model;}
 public boolean enabled(){return !key.isBlank();}
 public String model(){return model;}
 public record Answer(JsonNode data,JsonNode usage) {}
 public Answer request(String instructions,List<Map<String,Object>> content,Map<String,Object> schema) throws Exception {
  if(!enabled())throw new IllegalStateException("OPENAI_API_KEY belum dikonfigurasi di server");
  var body=Map.of("model",model,"store",false,"instructions",instructions,"max_output_tokens",12000,
   "input",List.of(Map.of("role","user","content",content)),"text",Map.of("format",Map.of("type","json_schema","name","auction_data","strict",true,"schema",schema)));
  var req=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses")).timeout(Duration.ofMinutes(3))
   .header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
  var res=http.send(req,HttpResponse.BodyHandlers.ofString());
  if(res.statusCode()!=200)throw new IllegalStateException("OpenAI HTTP "+res.statusCode()+". Periksa model, kuota, dan API key di server.");
  var root=json.readTree(res.body());
  if(!"completed".equals(root.path("status").asText()))throw new IllegalStateException("Output AI belum lengkap. Kecilkan jumlah halaman per batch.");
  for(var output:root.path("output"))for(var c:output.path("content"))if("output_text".equals(c.path("type").asText()))return new Answer(json.readTree(c.path("text").asText()),root.path("usage"));
  throw new IllegalStateException("AI tidak memberikan hasil terstruktur");
 }
 static Map<String,Object> field(String type,boolean nullable){return Map.of("type",nullable?List.of(type,"null"):type);}
 static Map<String,Object> object(Map<String,Object> fields){return Map.of("type","object","properties",fields,"required",new ArrayList<>(fields.keySet()),"additionalProperties",false);}
 static Map<String,Object> array(Object items){return Map.of("type","array","items",items);}
 static Map<String,Object> extractionSchema(){
  var f=new LinkedHashMap<String,Object>();
  for(String s:List.of("lotNumber","vehicle"))f.put(s,field("string",false));
  for(String s:List.of("transmission","stnk","bpkb","taxExpiry","notes","sourceQuote"))f.put(s,field("string",true));
  for(String s:List.of("year","kilometer","basePrice","sourcePage"))f.put(s,field("integer",true));
  return object(Map.of("lots",array(object(f))));
 }
 static Map<String,Object> analysisSchema(){
  var f=new LinkedHashMap<String,Object>();f.put("lotId",field("string",false));
  for(String s:List.of("demandScore","liquidityScore","conditionScore","repairEstimate"))f.put(s,field("integer",false));
  for(String s:List.of("indicativeSellPrice","daysMin","daysMax"))f.put(s,field("integer",true));
  f.put("reasons",array(field("string",false)));f.put("risks",array(field("string",false)));
  return object(Map.of("assessments",array(object(f))));
 }
}
