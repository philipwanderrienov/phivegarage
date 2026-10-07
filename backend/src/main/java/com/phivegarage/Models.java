package com.phivegarage;
import java.util.*;
import jakarta.validation.constraints.*;
public final class Models {
 public record LotData(@NotBlank String lotNumber,@NotBlank String vehicle,Integer year,String transmission,Long kilometer,
  Long basePrice,String stnk,String bpkb,String taxExpiry,String notes,Integer sourcePage,String sourceQuote,
  List<ComparablePrice> comparables,Long repairCost,Long taxCost,Long otherCost) {}
 public record ComparablePrice(@NotBlank String url,@Positive long price,@NotBlank String observedDate,String notes) {}
 public record Criteria(@Positive @Max(100000000000L) long capital,@Positive @Max(100000000000L) long maxPerUnit,@PositiveOrZero @Max(100000000000L) long targetProfit,
  @PositiveOrZero @Max(100000000000L) long auctionFee,@NotNull @DecimalMin("0") @DecimalMax("100") java.math.BigDecimal auctionFeePercent,
  @PositiveOrZero @Max(100000000000L) long repairBuffer,@PositiveOrZero @Max(100000000000L) long taxBuffer,@PositiveOrZero @Max(100000000000L) long otherCosts,
  @PositiveOrZero @Max(100000000000L) long riskBuffer,@Min(1950) @Max(2100) int minYear,@Positive long maxKilometer,
  @NotNull @Pattern(regexp="ALL|MT|AT") String transmission,@NotNull @Pattern(regexp="RETAIL|DEALER") String buyer,
  @NotNull @Pattern(regexp="FAST|BALANCED|MARGIN") String strategy,boolean requireStnk,boolean requireBpkb,
  @Size(max=3000) String instructions) {}
 public record Assessment(String lotId,int demandScore,int liquidityScore,int conditionScore,long repairEstimate,
  Long indicativeSellPrice,Integer daysMin,Integer daysMax,List<String> reasons,List<String> risks) {}
 public record Result(String lotId,String lotNumber,String vehicle,String recommendation,int score,
  Long basePrice,Long sellPrice,Long maxBid,Long totalCost,Long profit,List<String> blockers,Assessment ai,boolean watchlisted) {}
}
