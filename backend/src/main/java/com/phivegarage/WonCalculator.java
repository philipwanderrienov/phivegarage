package com.phivegarage;
import java.math.BigDecimal;
public final class WonCalculator {
 public record Summary(long adminFee,long auctionTax,long auctionAllIn,Long idealAcquisition,long expenseTotal,long totalCost,Long expectedMargin,Long realMargin,String status){}
 public static Summary calculate(long winningBid,Long idealBid,long admin,BigDecimal tax,long expenses,Long target,Long deal){
  long fee=BidCalculator.fee(winningBid,admin,tax),acquisition=Math.addExact(winningBid,fee),total=Math.addExact(acquisition,expenses);
  Long ideal=idealBid==null?null:Math.addExact(idealBid,BidCalculator.fee(idealBid,admin,tax));
  return new Summary(admin,fee-admin,acquisition,ideal,expenses,total,target==null?null:Math.subtractExact(target,total),deal==null?null:Math.subtractExact(deal,total),deal==null?"OWNED":"SOLD");
 }
}
