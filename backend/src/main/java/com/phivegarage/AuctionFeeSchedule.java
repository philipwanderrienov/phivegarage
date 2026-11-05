package com.phivegarage;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.*;
import java.util.*;

/** Immutable auction-house fee snapshots, including historical fixed and tiered schedules. */
public final class AuctionFeeSchedule {
 private AuctionFeeSchedule(){}
 public record Cost(long adminFee,long auctionTax,long totalFee,long auctionAllIn){}
 public static Cost cost(long bid,JsonNode snapshot){
  if(bid<0||bid>100_000_000_000L)throw new IllegalArgumentException("Harga bid tidak valid");
  if(snapshot==null||snapshot.isNull()||!snapshot.hasNonNull("taxPercent"))throw new IllegalArgumentException("Tarif pajak lelang belum tersedia");
  long admin;
  String rule=snapshot.path("feeRule").asText("FIXED");
  switch(rule){
   case "FIXED" -> {if(!snapshot.hasNonNull("adminFee"))throw new IllegalArgumentException("Admin lelang belum tersedia");admin=snapshot.path("adminFee").asLong();}
   case "CARSOME_2026" -> admin=bid<100_000_000L?500_000L:bid<=200_000_000L?750_000L:1_000_000L;
   case "OTOMAS_2026" -> {if(bid>600_000_000L)throw new IllegalArgumentException("Tarif Otomas di atas Rp600 juta belum tersedia");admin=bid<=400_000_000L?3_500_000L:5_500_000L;}
   case "SMARTBID_2026" -> admin=bid<=350_000_000L?3_500_000L:percent(bid,new BigDecimal("1"));
   case "CUSTOM_TIERS" -> admin=customAdmin(bid,snapshot.path("tiers"));
   default -> throw new IllegalArgumentException("Aturan tarif lelang belum dikenal: "+rule);
  }
  long tax=percent(bid,snapshot.path("taxPercent").decimalValue());
  long total=Math.addExact(admin,tax);
  return new Cost(admin,tax,total,Math.addExact(bid,total));
 }
 static long customAdmin(long bid,JsonNode tiers){
  if(!tiers.isArray()||tiers.isEmpty())throw new IllegalArgumentException("Tier admin tidak valid");
  long prev=0;
  for(int i=0;i<tiers.size();i++){
   var tier=tiers.get(i);
   long upper=tier.path("upToBid").isNull()?100_000_000_000L:tier.path("upToBid").asLong(-1);
   if(upper<=prev)throw new IllegalArgumentException("Tier admin tidak berurutan");
   boolean fixed=tier.path("adminFee").isIntegralNumber(),pct=tier.path("adminPercent").isNumber();
   if(fixed==pct)throw new IllegalArgumentException("Aturan tier ambigu");
   if(bid<=upper){
    long amount=fixed?tier.path("adminFee").asLong():percent(bid,tier.path("adminPercent").decimalValue());
    if(amount<0)throw new IllegalArgumentException("Biaya admin negatif");
    return amount;
   }
   prev=upper;
  }
  throw new IllegalArgumentException("Tidak ada tier berlaku untuk harga bid");
 }
 static long percent(long bid,BigDecimal pct){return BigDecimal.valueOf(bid).multiply(pct).divide(BigDecimal.valueOf(100),0,RoundingMode.CEILING).longValueExact();}
 public static long maxBid(long sale,long target,long extra,long capital,JsonNode fee){
  long cap=Math.min(Math.subtractExact(sale,target),capital)-extra;
  if(cap<=0)return 0;
  long lo=0,hi=Math.min(cap,100_000_000_000L);
  // Fee schedules above are monotonic on their supported ranges.
  if("OTOMAS_2026".equals(fee.path("feeRule").asText()))hi=Math.min(hi,600_000_000L);
  while(lo<hi){long mid=lo+(hi-lo+1)/2;long allIn=cost(mid,fee).auctionAllIn();if(allIn<=cap)lo=mid;else hi=mid-1;}
  return lo;
 }
 public static Map<String,Object> preview(long bid,JsonNode fee){
  if(fee==null||fee.isNull())return Map.of("available",false);
  var c=cost(bid,fee);
  return Map.of("available",true,"bid",bid,"adminFee",c.adminFee(),"taxPercent",fee.path("taxPercent").decimalValue(),"auctionTax",c.auctionTax(),"auctionAllIn",c.auctionAllIn(),"feeRule",fee.path("feeRule").asText("FIXED"));
 }
}
