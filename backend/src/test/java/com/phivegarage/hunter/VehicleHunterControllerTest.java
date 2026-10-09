package com.phivegarage.hunter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class VehicleHunterControllerTest {
 private JdbcTemplate db;
 private VehicleHunterController controller;
 private final UUID id=UUID.randomUUID();
 @BeforeEach void setup() {
  db=mock(JdbcTemplate.class);
  controller=new VehicleHunterController(db);
  Map<String,Object> listing=new HashMap<>();
  listing.put("asking_price",43_000_000L);
  listing.put("stnk_status","ADA");
  listing.put("bpkb_status","ADA");
  listing.put("listing_status","ACTIVE");
  when(db.queryForList(eq("SELECT * FROM hunter_listings WHERE id=?"),eq(id))).thenReturn(List.of(listing));
 }
 @SuppressWarnings({"rawtypes","unchecked"})
 private void comparablePrices(Long... prices) {
  when(db.query(eq("SELECT price FROM hunter_comparables WHERE listing_id=? ORDER BY price"),any(RowMapper.class),eq(id)))
    .thenReturn(Arrays.asList(prices));
 }
 @Test void budgetAndProfitAreDynamic() {
  comparablePrices(50_000_000L,54_000_000L);
  var first=controller.evaluate(id,50_000_000L,8_000_000L,1_000_000L,1_000_000L,500_000L,500_000L,"RETAIL");
  var second=controller.evaluate(id,65_000_000L,2_000_000L,1_000_000L,1_000_000L,500_000L,500_000L,"RETAIL");
  assertEquals(36_500_000L,first.get("maxBuyPrice"));
  assertEquals(42_500_000L,second.get("maxBuyPrice"));
  assertEquals("NEGO",first.get("decision"));
 }
 @Test void higherRepairCostReducesMaxBuyPrice() {
  comparablePrices(60_000_000L);
  var low=controller.evaluate(id,65_000_000L,5_000_000L,1_000_000L,0,0,0,"RETAIL");
  var high=controller.evaluate(id,65_000_000L,5_000_000L,5_000_000L,0,0,0,"RETAIL");
  assertEquals(4_000_000L,(long)low.get("maxBuyPrice")-(long)high.get("maxBuyPrice"));
 }
 @Test void noComparableDoesNotInventMarketPrice() {
  comparablePrices();
  var result=controller.evaluate(id,50_000_000L,8_000_000L,0,0,0,0,"RETAIL");
  assertEquals("REVIEW",result.get("decision"));
  assertNull(result.get("quickSaleEstimate"));
  assertNull(result.get("maxBuyPrice"));
 }
 @Test void unverifiedListingRequiresReview() {
  comparablePrices(60_000_000L);
  var unverified=new HashMap<String,Object>();
  unverified.put("asking_price",43_000_000L);
  unverified.put("stnk_status","ADA");
  unverified.put("bpkb_status","ADA");
  unverified.put("listing_status","UNVERIFIED");
  when(db.queryForList(eq("SELECT * FROM hunter_listings WHERE id=?"),eq(id))).thenReturn(List.of(unverified));
  var result=controller.evaluate(id,65_000_000L,5_000_000L,0,0,0,0,"RETAIL");
  assertEquals("REVIEW",result.get("decision"));
 }
 @Test void invalidFinancialParametersAreRejected() {
  assertThrows(ResponseStatusException.class,()->controller.evaluate(id,0,8_000_000L,0,0,0,0,"RETAIL"));
  assertThrows(ResponseStatusException.class,()->controller.evaluate(id,65_000_000L,-1,0,0,0,0,"RETAIL"));
  assertThrows(ResponseStatusException.class,()->controller.evaluate(id,65_000_000L,0,-1,0,0,0,"RETAIL"));
 }
}
