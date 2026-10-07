package com.phivegarage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.*;
import java.math.BigDecimal;
import static com.phivegarage.Models.*;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationTest {
 @TempDir Path dir;
 ObjectMapper json=new ObjectMapper();Jobs jobs;
 @BeforeEach void setup()throws Exception{jobs=new Jobs(null,null,json,null,dir.toString());}
 @AfterEach void close(){jobs.close();}
 Criteria criteria(){return new Criteria(65_000_000,65_000_000,5_000_000,2_000_000,BigDecimal.ZERO,3_000_000,2_000_000,1_000_000,2_000_000,2012,150000,"ALL","RETAIL","FAST",true,true,"");}
 Assessment assessment(){return new Assessment("id",90,90,80,1_000_000,100_000_000L,7,21,List.of("Test"),List.of());}
 Map<String,Object> lot(boolean verified,long base,List<ComparablePrice> comps){
  var d=new LotData("001","Test Car",2016,"MT",80000L,base,"ADA","ADA",null,"",1,"Test source",comps,null,null,null);
  return Map.of("id","id","verified",verified,"data",json.valueToTree(d));
 }
 List<ComparablePrice> comps(){return List.of(new ComparablePrice("https://example.com/unit",80_000_000,"2026-10-07","Fixture"));}
 @Test void aiPriceAloneCannotAuthorizeBid()throws Exception{
  var r=jobs.evaluate(lot(true,50_000_000,List.of()),criteria(),assessment());assertEquals("REVIEW",r.recommendation());assertNull(r.maxBid());assertNull(r.sellPrice());
 }
 @Test void unverifiedCatalogCannotAuthorizeBid()throws Exception{
  var r=jobs.evaluate(lot(false,50_000_000,comps()),criteria(),assessment());assertEquals("REVIEW",r.recommendation());
 }
 @Test void knownDealHasBudgetLimitedMaxBid()throws Exception{
  var r=jobs.evaluate(lot(true,50_000_000,comps()),criteria(),assessment());assertEquals("BID",r.recommendation());assertEquals(76_000_000L,r.sellPrice());assertEquals(55_000_000L,r.maxBid());assertEquals(60_000_000L,r.totalCost());assertEquals(16_000_000L,r.profit());
 }
 @Test void baseOverLimitIsSkipped()throws Exception{
  var r=jobs.evaluate(lot(true,56_000_000,comps()),criteria(),assessment());assertEquals("SKIP",r.recommendation());
 }
 @Test void highRepairRiskReducesBidLimit()throws Exception{
  var a=new Assessment("id",90,90,50,15_000_000,100_000_000L,7,21,List.of(),List.of("Indikasi perbaikan besar"));
  var r=jobs.evaluate(lot(true,50_000_000,comps()),criteria(),a);assertEquals(43_000_000L,r.maxBid());assertEquals("SKIP",r.recommendation());
 }
}
