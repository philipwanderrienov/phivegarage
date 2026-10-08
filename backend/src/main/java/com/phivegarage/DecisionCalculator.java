package com.phivegarage;
import java.math.*;
import java.util.*;
public final class DecisionCalculator {
 public record Scenario(long purchasePrice,long acquisition,long costLow,long costHigh,long profitAtLowSale,long profitAtMidSale,long profitAtHighSale,long downsideProfit,boolean withinBudget){}
 public record Report(String recommendation,List<String> reasons,long maxPurchase,long negotiationLow,long negotiationHigh,long acquisitionAtAsk,long costLow,long costHigh,long breakEvenSale,Long desiredSale,long targetGap,BigDecimal capitalPercent,List<Scenario> scenarios,List<String> inspections){}
 static long floorOffer(long amount){return amount>=1_000_000?amount/500_000*500_000:amount;}
 public static Report calculate(DecisionApi.Input d,long admin,BigDecimal tax){
  long nonRepair=Math.addExact(Math.addExact(d.taxCost(),d.otherCosts()),d.riskBuffer());
  long lowExtra=Math.addExact(nonRepair,d.repairLow()),highExtra=Math.addExact(nonRepair,d.repairHigh());
  long safe=Math.min(d.maxPurchase(),BidCalculator.maxBid(d.saleLow(),d.targetProfit(),highExtra,d.capital(),admin,tax));
  long acquisition=d.askPrice()+BidCalculator.fee(d.askPrice(),admin,tax),low=acquisition+lowExtra,high=acquisition+highExtra;
  var reasons=new ArrayList<String>();String verdict;
  if(!"ADA".equals(d.stnk())||d.criticalRisk()){verdict="SKIP";reasons.add(!"ADA".equals(d.stnk())?"STNK belum dinyatakan ADA; jangan lanjut beli":"Risiko berat dilaporkan; jangan lanjut sebelum masalah terselesaikan");}
  else if(safe<=0){verdict="SKIP";reasons.add("Tidak ada harga beli positif yang memenuhi modal dan target laba pada skenario konservatif");}
  else if(!d.evidenceConfirmed()||!d.inspectionConfirmed()||!"ADA".equals(d.bpkb())){verdict="REVIEW";reasons.add("Harga pasar, inspeksi fisik, dan BPKB harus diverifikasi sebelum keputusan beli");}
  else if(d.askPrice()>safe){verdict="NEGOTIATE";reasons.add("Harga penawaran melebihi batas beli yang memenuhi modal, biaya konservatif, dan target laba");}
  else{verdict="BUY_CONDITIONAL";reasons.add("Harga penawaran memenuhi hitungan konservatif dan data telah ditandai terverifikasi oleh user");}
  if(safe>0&&d.askPrice()>safe)reasons.add("Harga penawaran di atas batas beli; negosiasikan atau lewatkan jika seller tidak turun");
  if(high>d.capital())reasons.add("ALL IN skenario biaya tinggi melebihi modal per unit");
  if(d.saleLow()-high<d.targetProfit())reasons.add("Laba pada harga jual rendah belum mencapai target");
  reasons.add("Batas beli memakai harga jual rendah, biaya servis tinggi, dan cadangan risiko; bukan harga pasar yang dicari otomatis");
  var prices=new TreeSet<Long>();prices.add(d.askPrice());if(safe>0){prices.add(safe);prices.add(floorOffer(safe*90/100));prices.add(floorOffer(safe*95/100));}prices.remove(0L);
  var scenarios=new ArrayList<Scenario>();for(long price:prices){long a=price+BidCalculator.fee(price,admin,tax),cLow=a+lowExtra,cHigh=a+highExtra;scenarios.add(new Scenario(price,a,cLow,cHigh,d.saleLow()-cLow,d.saleMid()-cLow,d.saleHigh()-cLow,d.saleLow()-cHigh,cHigh<=d.capital()));}
  return new Report(verdict,reasons,safe,floorOffer(safe*90/100),floorOffer(safe*95/100),acquisition,low,high,high,high+d.targetProfit(),Math.max(0,d.askPrice()-safe),BigDecimal.valueOf(high).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(d.capital()),1,RoundingMode.HALF_UP),scenarios,List.of("Cocokkan STNK/BPKB, identitas kendaraan, dan status pajak","Inspeksi mesin dingin, kebocoran, transmisi, AC, dan kaki-kaki","Periksa kolong, sasis, bekas tabrakan/banjir, dan riwayat servis","Bandingkan tipe, tahun, kondisi, lokasi, dan tanggal pembanding harga"));
 }
}
