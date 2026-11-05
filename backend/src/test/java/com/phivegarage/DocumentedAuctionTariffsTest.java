package com.phivegarage;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DocumentedAuctionTariffsTest {
 final ObjectMapper mapper=new ObjectMapper();
 JsonNode steps(String limitsAndAdmins)throws Exception{return mapper.readTree("{\"feeRule\":\"STEP_TIERS\",\"taxPercent\":1.1,\"taxPolicy\":\"CONDITIONAL_MAX\",\"stepSize\":200000000,\"stepAdminIncrease\":2000000,\"tiers\":"+limitsAndAdmins+"}");}
 @Test void tribikNovember2026AdminSchedule()throws Exception{
  var fee=steps("[{\"upToBid\":300000000,\"adminFee\":3000000},{\"upToBid\":500000000,\"adminFee\":4000000},{\"upToBid\":600000000,\"adminFee\":5000000},{\"upToBid\":700000000,\"adminFee\":8000000},{\"upToBid\":900000000,\"adminFee\":9000000},{\"upToBid\":1000000000,\"adminFee\":10000000}]");
  assertEquals(3000000,AuctionFeeSchedule.cost(50000000,fee).adminFee());
  assertEquals(4000000,AuctionFeeSchedule.cost(300000001,fee).adminFee());
  assertEquals(5000000,AuctionFeeSchedule.cost(500000001,fee).adminFee());
  assertEquals(8000000,AuctionFeeSchedule.cost(600000001,fee).adminFee());
  assertEquals(9000000,AuctionFeeSchedule.cost(700000001,fee).adminFee());
  assertEquals(10000000,AuctionFeeSchedule.cost(900000001,fee).adminFee());
  assertEquals(12000000,AuctionFeeSchedule.cost(1000000001,fee).adminFee());
 }
 @Test void otomasFullDocumentedSchedule()throws Exception{
  var fee=steps("[{\"upToBid\":400000000,\"adminFee\":3500000},{\"upToBid\":600000000,\"adminFee\":5500000},{\"upToBid\":800000000,\"adminFee\":8500000}]");
  assertEquals(3500000,AuctionFeeSchedule.cost(400000000,fee).adminFee());
  assertEquals(5500000,AuctionFeeSchedule.cost(400000001,fee).adminFee());
  assertEquals(8500000,AuctionFeeSchedule.cost(600000001,fee).adminFee());
  assertEquals(10500000,AuctionFeeSchedule.cost(800000001,fee).adminFee());
 }
 @Test void careadyFullDocumentedSchedule()throws Exception{
  var fee=steps("[{\"upToBid\":400000000,\"adminFee\":3000000},{\"upToBid\":600000000,\"adminFee\":5000000},{\"upToBid\":800000000,\"adminFee\":8000000},{\"upToBid\":1000000000,\"adminFee\":10000000},{\"upToBid\":1200000000,\"adminFee\":12000000}]");
  assertEquals(3000000,AuctionFeeSchedule.cost(57500000,fee).adminFee());
  assertEquals(5000000,AuctionFeeSchedule.cost(400000001,fee).adminFee());
  assertEquals(8000000,AuctionFeeSchedule.cost(600000001,fee).adminFee());
  assertEquals(12000000,AuctionFeeSchedule.cost(1100000000,fee).adminFee());
  assertEquals(14000000,AuctionFeeSchedule.cost(1200000001,fee).adminFee());
  var preview=AuctionFeeSchedule.preview(57500000,fee);
  assertEquals(true,preview.get("taxIsConservativeReserve"));
  assertEquals("CONDITIONAL_MAX",preview.get("taxPolicy"));
 }
 @Test void existingSmartbidRuleRemainsTierAware()throws Exception{
  var fee=mapper.readTree("{\"feeRule\":\"SMARTBID_2026\",\"taxPercent\":1.1}");
  assertEquals(3500000,AuctionFeeSchedule.cost(350000000,fee).adminFee());
  assertEquals(3500001,AuctionFeeSchedule.cost(350000001,fee).adminFee());
 }
}
