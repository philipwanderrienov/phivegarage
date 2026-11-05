package com.phivegarage;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CatalogHouseDiscoveryTest {
 final ObjectMapper json=new ObjectMapper();
 JsonNode example(String fields)throws Exception{return json.readTree(fields);}
 @Test void normalizesAliasesButKeepsTriBidDistinctFromTribik(){
  assertEquals(CatalogHouseDiscovery.canonical("SmartBid"),CatalogHouseDiscovery.canonical("PT Balai Lelang SmartBID"));
  assertEquals("carsome",CatalogHouseDiscovery.canonical("CARSOME Dealer"));
  assertNotEquals(CatalogHouseDiscovery.canonical("TriBid"),CatalogHouseDiscovery.canonical("Tribik"));
 }
 @Test void acceptsPrintedShortNameAndGeneralFixedTariff()throws Exception{
  String source="JBA Katalog lelang biaya admin Rp 3.000.000 dan pajak lelang 1,1% dari harga ketok";
  var data=example("""
   {"houseName":"JBA","nameEvidence":"JBA","pricingModel":"FIXED","feeScope":"GENERAL",
    "adminEvidence":"biaya admin Rp 3.000.000",
    "taxEvidence":"pajak lelang 1,1% dari harga ketok",
    "adminFee":3000000,"taxPercent":1.1,"notes":null,"tiers":[]}
   """);
  var valid=CatalogHouseDiscovery.validated(data,source);
  assertNotNull(valid);
  var fee=CatalogHouseDiscovery.confirmedFee(valid,source);
  assertNotNull(fee);
  assertEquals(3_000_000L,fee.get("adminFee"));
  assertEquals(53_550_000L,AuctionFeeSchedule.cost(50_000_000,json.valueToTree(fee)).auctionAllIn());
 }
 @Test void noExplicitTaxMustNeverDefaultToZero()throws Exception{
  String source="NewAuction Biaya admin Rp 500.000; pajak tidak tercantum";
  var data=example("""
    {"houseName":"NewAuction","nameEvidence":"NewAuction","adminEvidence":"Biaya admin Rp 500.000",
     "taxEvidence":null,"pricingModel":"FIXED","feeScope":"GENERAL","adminFee":500000,"taxPercent":null,
     "notes":null,"tiers":[]}
   """);
  assertNull(CatalogHouseDiscovery.confirmedFee(CatalogHouseDiscovery.validated(data,source),source));
 }
 @Test void oneTimePromotionsAreNotStoredAsGeneralMasterTariffs()throws Exception{
  String source="Carsome Dealer Promo admin Rp 0 hanya pembelian pertama. Pajak lelang 0%.";
  var data=example("""
    {"houseName":"Carsome Dealer","nameEvidence":"Carsome Dealer","adminEvidence":"Promo admin Rp 0",
     "taxEvidence":"Pajak lelang 0%","pricingModel":"FIXED","feeScope":"PROMO","adminFee":0,"taxPercent":0,
     "notes":"Hanya unit perdana","tiers":[]}
   """);
  assertNull(CatalogHouseDiscovery.confirmedFee(CatalogHouseDiscovery.validated(data,source),source));
 }
 @Test void supportsCompleteTextBackedCustomTiers()throws Exception{
  String source="ReadyAuction Admin s/d Rp100jt: Rp500rb, admin di atas Rp100jt: 1% bid. Pajak lelang 0%.";
  var data=example("""
    {"houseName":"ReadyAuction","nameEvidence":"ReadyAuction",
     "adminEvidence":"Admin s/d Rp100jt: Rp500rb, admin di atas Rp100jt: 1% bid",
     "taxEvidence":"Pajak lelang 0%","pricingModel":"TIERED","feeScope":"GENERAL",
     "adminFee":null,"taxPercent":0,"notes":null,
     "tiers":[{"upToBid":100000000,"adminFee":500000,"adminPercent":null,"evidence":"Admin s/d Rp100jt: Rp500rb"},
              {"upToBid":null,"adminFee":null,"adminPercent":1,"evidence":"admin di atas Rp100jt: 1% bid"}]}
   """);
  var fee=CatalogHouseDiscovery.confirmedFee(CatalogHouseDiscovery.validated(data,source),source);
  assertNotNull(fee);
  var schedule=json.valueToTree(fee);
  assertEquals(500000,AuctionFeeSchedule.cost(99_000_000,schedule).adminFee());
  assertEquals(1_500_000,AuctionFeeSchedule.cost(150_000_000,schedule).adminFee());
  long max=AuctionFeeSchedule.maxBid(175_000_000,5_000_000,5_000_000,160_000_000,schedule);
  assertTrue(AuctionFeeSchedule.cost(max,schedule).auctionAllIn()+5_000_000<=160_000_000);
  assertTrue(AuctionFeeSchedule.cost(max+1,schedule).auctionAllIn()+5_000_000>160_000_000);
 }
}
