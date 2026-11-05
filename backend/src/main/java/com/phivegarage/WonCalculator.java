package com.phivegarage;
import java.math.BigDecimal;
public final class WonCalculator {
 public record Summary(long adminFee,long auctionTax,long auctionAllIn,Long idealAcquisition,long expenseTotal,long totalCost,Long expectedMargin,Long realMargin,String status){}
 public static Summary calculate(long winningBid,Long idealBid,long admin,BigDecimal tax,long expenses,Long target,Long deal){
  return calculate(winningBid,idealBid,admin,tax,expenses,target,deal,null);
 }
 public static Summary calculate(long winningBid,Long idealBid,long admin,BigDecimal tax,long expenses,Long target,Long deal,com.fasterxml.jackson.databind.JsonNode snapshot){
  long fee=snapshot==null?BidCalculator.fee(winningBid,admin,tax):AuctionFeeSchedule.cost(winningBid,snapshot).totalFee();
  long appliedAdmin=snapshot==null?admin:AuctionFeeSchedule.cost(winningBid,snapshot).adminFee();
  long acquisition=Math.addExact(winningBid,fee),total=Math.addExact(acquisition,expenses);
  Long ideal=idealBid==null?null:Math.addExact(idealBid,snapshot==null?BidCalculator.fee(idealBid,admin,tax):AuctionFeeSchedule.cost(idealBid,snapshot).totalFee());
  return new Summary(appliedAdmin,fee-appliedAdmin,acquisition,ideal,expenses,total,target==null?null:Math.subtractExact(target,total),deal==null?null:Math.subtractExact(deal,total),deal==null?"OWNED":"SOLD");
 }
}
