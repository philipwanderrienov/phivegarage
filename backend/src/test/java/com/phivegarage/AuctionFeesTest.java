package com.phivegarage;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class AuctionFeesTest {
 final ObjectMapper json=new ObjectMapper();
 @Test void jbaTaxIsOnBidNotAdmin()throws Exception{var c=AuctionHouses.cost(50_000_000,json.readTree("{\"adminFee\":3000000,\"taxPercent\":1.1}"));assertEquals(550_000L,c.get("auctionTax"));assertEquals(53_550_000L,c.get("auctionAllIn"));}
 @Test void fractionalTaxRoundsUp()throws Exception{var c=AuctionHouses.cost(1,json.readTree("{\"adminFee\":3000000,\"taxPercent\":1.1}"));assertEquals(3_000_002L,c.get("auctionAllIn"));}
 @Test void absentLegacyFeeHasNoInventedAllIn(){assertEquals(false,AuctionHouses.cost(50_000_000,null).get("available"));}
 @Test void selectedHouseOverridesClientFeeAndRequiresStnk()throws Exception{var c=new Models.Criteria(65_000_000,65_000_000,60_000_000,5_000_000,0,BigDecimal.ZERO,0,0,0,0,2012,150000,"ALL","RETAIL","FAST",false,true,"");var e=AuctionHouses.withFees(c,json.readTree("{\"adminFee\":3000000,\"taxPercent\":1.1}"));assertEquals(3_000_000,e.auctionFee());assertEquals(new BigDecimal("1.1"),e.auctionFeePercent());assertTrue(e.requireStnk());assertEquals(60_000_000,e.maxBid());}
}
