package com.phivegarage;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WonUnitsTest {
 @Test void reconcilesAuctionExpensesAndMargins(){var s=WonCalculator.calculate(50_000_000L,48_000_000L,3_000_000L,new BigDecimal("1.1"),2_000_000L,65_000_000L,62_000_000L);assertEquals(550_000,s.auctionTax());assertEquals(53_550_000,s.auctionAllIn());assertEquals(51_528_000L,s.idealAcquisition());assertEquals(55_550_000,s.totalCost());assertEquals(9_450_000L,s.expectedMargin());assertEquals(6_450_000L,s.realMargin());assertEquals("SOLD",s.status());}
 @Test void unsoldUnitHasNoRealizedMargin(){var s=WonCalculator.calculate(50_000_000L,null,3_000_000L,new BigDecimal("1.1"),0,null,null);assertNull(s.realMargin());assertNull(s.expectedMargin());assertNull(s.idealAcquisition());assertEquals("OWNED",s.status());}
 @Test void preservesLossAndRoundsFractionalRupiah(){var s=WonCalculator.calculate(101L,null,0,new BigDecimal("1.1"),10,100L,100L);assertEquals(2,s.auctionTax());assertEquals(-13L,s.realMargin());}
 WonApi.UnitData data(Long deal,LocalDate sold){return new WonApi.UnitData("Synthetic car",2017,"1",50_000L,"TEST",40_000_000L,null,50_000_000L,LocalDate.of(2026,10,1),null,null,deal,sold,"","");}
 @Test void saleNeedsBothPriceAndDate(){assertThrows(IllegalArgumentException.class,()->WonApi.validate(data(60_000_000L,null)));assertThrows(IllegalArgumentException.class,()->WonApi.validate(data(null,LocalDate.of(2026,10,2))));}
 @Test void saleCannotPrecedeWinning(){assertThrows(IllegalArgumentException.class,()->WonApi.validate(data(60_000_000L,LocalDate.of(2026,9,30))));assertDoesNotThrow(()->WonApi.validate(data(60_000_000L,LocalDate.of(2026,10,2))));}
 @Test void fundsAccumulateSoldProfitAndLossButSeparateUnsoldCost(){var db=mock(JdbcTemplate.class);// A real Store preserves JDBC row normalization.
  var real=new Store(db,new ObjectMapper());var api=spy(new WonApi(real,mock(AuctionHouses.class),mock(Jobs.class),new ObjectMapper()));
  when(db.queryForObject("SELECT starting_funds FROM garage_settings WHERE id=1",Long.class)).thenReturn(57_000_000L);
  var rows=new ArrayList<Map<String,Object>>();for(int i=0;i<3;i++)rows.add(new HashMap<>(Map.of("id",UUID.randomUUID())));
  when(db.queryForList(eq("SELECT * FROM won_units ORDER BY created_at"),any(Object[].class))).thenReturn(rows);
  doAnswer(inv->{Map<String,Object> row=inv.getArgument(0);int i=rows.indexOf(row);Long profit=null;if(i==0)profit=5_000_000L;else if(i==1)profit=-2_000_000L;row.put("summary",new WonCalculator.Summary(0,0,50_000_000,null,0,50_000_000,null,profit,i==2?"OWNED":"SOLD"));return row;}).when(api).decorate(anyMap());
  var out=(Map<?,?>)api.funds();assertEquals(3_000_000L,out.get("realizedProfit"));assertEquals(60_000_000L,out.get("funds"));assertEquals(50_000_000L,out.get("unsoldCapital"));
 }
 @Test void expenseCannotBeEditedThroughAnotherUnit(){var db=mock(JdbcTemplate.class);var api=new WonApi(new Store(db,new ObjectMapper()),mock(AuctionHouses.class),mock(Jobs.class),new ObjectMapper());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->api.editExpense(UUID.randomUUID(),UUID.randomUUID(),new WonApi.Expense(LocalDate.now(),"Servis","Synthetic expense",1000)));}

 @Test void linkedUnitCopiesCatalogSnapshotInsteadOfCurrentMaster(){var db=mock(JdbcTemplate.class);var json=new ObjectMapper().findAndRegisterModules();var store=spy(new Store(db,json));var houses=mock(AuctionHouses.class);var api=new WonApi(store,houses,mock(Jobs.class),json);var house=UUID.randomUUID();var lot=UUID.randomUUID();var fee=Map.of("name","Synthetic balai","adminFee",3000000,"taxPercent",new BigDecimal("1.1"));
  doReturn(new HashMap<String,Object>(Map.<String,Object>of("auction_house_id",house,"fee_snapshot",json.valueToTree(fee)))).when(store).one(anyString(),eq(lot));
  api.create(new WonApi.SaveUnit(house,lot,data(null,null)));verifyNoInteractions(houses);
  String expectedFee=store.encode(fee);verify(db).update(eq("INSERT INTO won_units(id,source_lot_id,auction_house_id,data,fee_snapshot) VALUES(?,?,?,?::jsonb,?::jsonb)"),any(UUID.class),eq(lot),eq(house),anyString(),eq(expectedFee));
 }
 @Test void existingUnitRejectsHouseOrSourceChanges(){var db=mock(JdbcTemplate.class);var json=new ObjectMapper();var store=spy(new Store(db,json));var api=new WonApi(store,mock(AuctionHouses.class),mock(Jobs.class),json);var house=UUID.randomUUID();var id=UUID.randomUUID();var row=new HashMap<String,Object>();row.put("auction_house_id",house);row.put("source_lot_id",null);doReturn(row).when(store).one(anyString(),eq(id));assertThrows(IllegalArgumentException.class,()->api.update(id,new WonApi.SaveUnit(UUID.randomUUID(),null,data(null,null))));assertThrows(IllegalArgumentException.class,()->api.update(id,new WonApi.SaveUnit(house,UUID.randomUUID(),data(null,null))));verifyNoInteractions(db);
 }
}
