package com.phivegarage;
import java.math.*;
public final class BidCalculator {
 private BidCalculator() {}
 public static long fee(long bid,long fixed,BigDecimal percent) {
  return Math.addExact(fixed,BigDecimal.valueOf(bid).multiply(percent).divide(BigDecimal.valueOf(100),0,RoundingMode.CEILING).longValueExact());
 }
 public static long maxBid(long sale,long target,long extra,long capital,long fixed,BigDecimal percent) {
  long available=Math.min(Math.subtractExact(sale,target),capital)-extra-fixed;
  if(available<=0) return 0;
  return BigDecimal.valueOf(available).divide(BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100))),0,RoundingMode.FLOOR).longValueExact();
 }
}
