package com.phivegarage.hunter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;

class HuntingKeywordsTest {
 @Test void matchesRequestedSignalsIgnoringCase() {
  var matches=HuntingKeywords.match("JUAL CEPAT, BU, BUTUH UANG. Pemakaian pribadi, atas nama pribadi.");
  assertTrue(matches.stream().anyMatch(x->x.phrase().equals("BU")));
  assertTrue(matches.stream().anyMatch(x->x.phrase().equals("Jual cepat")));
  assertTrue(matches.stream().anyMatch(x->x.phrase().equals("Pemakaian pribadi")));
  assertEquals(3,HuntingKeywords.priority(matches));
 }
 @Test void buMustBeStandaloneToken() {
  assertTrue(HuntingKeywords.match("Toyota bukan mobil baru untuk kebutuhan keluarga").stream().noneMatch(x->x.phrase().equals("BU")));
  assertTrue(HuntingKeywords.match("BU! Jual rugi").stream().anyMatch(x->x.phrase().equals("BU")));
 }
 @Test void handlesNullAndUnrelatedAds() {
  assertTrue(HuntingKeywords.match(null).isEmpty());
  assertTrue(HuntingKeywords.match("Toyota Avanza 2014 automatic").isEmpty());
 }
 @Test void recognizesAllSevenTerms() {
  String input="BU Butuh Uang Butuh dana cepat Jual cepat Jual rugi Pemakaian pribadi Atas nama pribadi";
  assertEquals(7,HuntingKeywords.match(input).size());
 }
}
