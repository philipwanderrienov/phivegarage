package com.phivegarage;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class TieredAuctionFeeTest {
 final ObjectMapper json=new ObjectMapper();
 JsonNode fee(String rule,String tax)throws Exception{return json.readTree("{\"feeRule\":\""+rule+"\",\"adminFee\":500000,\"taxPercent\":"+tax+"}");}
 @Test void carsomeHistoricalBoundaries()throws Exception{
  var f=fee("CARSOME_2026","0");
  assertEquals(500000,AuctionFeeSchedule.cost(57_500_000,f).adminFee());
  assertEquals(500000,AuctionFeeSchedule.cost(99_999_999,f).adminFee());
  assertEquals(750000,AuctionFeeSchedule.cost(100_000_000,f).adminFee());
  assertEquals(750000,AuctionFeeSchedule.cost(200_000_000,f).adminFee());
  assertEquals(1_000_000,AuctionFeeSchedule.cost(200_000_001,f).adminFee());
  assertEquals(58_000_000,AuctionFeeSchedule.cost(57_500_000,f).auctionAllIn());
 }
 @Test void smartbidAndOtomasBoundaryRules()throws Exception{
  var sb=fee("SMARTBID_2026","1.2");
  assertEquals(3_500_000,AuctionFeeSchedule.cost(350_000_000,sb).adminFee());
  assertEquals(3_500_001,AuctionFeeSchedule.cost(350_000_001,sb).adminFee());
  var otomas=fee("OTOMAS_2026","0.1");
  assertEquals(3_500_000,AuctionFeeSchedule.cost(400_000_000,otomas).adminFee());
  assertEquals(5_500_000,AuctionFeeSchedule.cost(400_000_001,otomas).adminFee());
  assertThrows(IllegalArgumentException.class,()->AuctionFeeSchedule.cost(600_000_001,otomas));
 }
 @Test void maxBidRespectsCapitalAcrossTierJump()throws Exception{
  var f=fee("CARSOME_2026","0");
  long max=AuctionFeeSchedule.maxBid(115_000_000,5_000_000,5_000_000,105_600_000,f);
  assertTrue(AuctionFeeSchedule.cost(max,f).auctionAllIn()+5_000_000<=105_600_000);
  assertTrue(AuctionFeeSchedule.cost(max+1,f).auctionAllIn()+5_000_000>105_600_000);
 }
 @Test void fixedHistoricalSnapshotUnchanged()throws Exception{
  var f=json.readTree("{\"adminFee\":0,\"taxPercent\":0}");
  assertEquals(57_500_000,AuctionFeeSchedule.cost(57_500_000,f).auctionAllIn());
  var jba=json.readTree("{\"adminFee\":3000000,\"taxPercent\":1.1}");
  assertEquals(53_550_000,AuctionFeeSchedule.cost(50_000_000,jba).auctionAllIn());
 }
 @Test void missingTariffMustNotBeInterpretedAsFree()throws Exception{
  assertThrows(IllegalArgumentException.class,()->AuctionFeeSchedule.cost(50_000_000,json.readTree("{\"adminFee\":null,\"taxPercent\":null}")));
 }
}
