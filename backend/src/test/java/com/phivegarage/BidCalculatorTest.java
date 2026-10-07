package com.phivegarage;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class BidCalculatorTest {
 @Test void respectsSaleAndBudget(){assertEquals(54_000_000L,BidCalculator.maxBid(72_000_000,5_000_000,9_000_000,65_000_000,2_000_000,BigDecimal.ZERO));}
 @Test void feeIsRoundedUp(){assertEquals(2_000_001L,BidCalculator.fee(1,2_000_000,new BigDecimal("1")));}
 @Test void percentFeeCannotExceedBudget(){long bid=BidCalculator.maxBid(78_000_000,7_000_000,5_000_000,65_000_000,2_000_000,new BigDecimal("2"));assertTrue(bid+BidCalculator.fee(bid,2_000_000,new BigDecimal("2"))+5_000_000<=65_000_000);assertTrue((bid+1)+BidCalculator.fee(bid+1,2_000_000,new BigDecimal("2"))+5_000_000>65_000_000);}
 @Test void impossibleDealHasZeroLimit(){assertEquals(0,BidCalculator.maxBid(10,5,10,10,0,BigDecimal.ZERO));}
}
