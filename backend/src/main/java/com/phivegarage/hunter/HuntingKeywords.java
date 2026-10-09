package com.phivegarage.hunter;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Search signals, not evidence of a good deal or seller authenticity. */
public final class HuntingKeywords {
 private HuntingKeywords() {}
 public record Signal(String phrase,String category,int priority) {}
 private static final List<Signal> SIGNALS=List.of(
  new Signal("BU","URGENCY",3),
  new Signal("Butuh Uang","URGENCY",3),
  new Signal("Butuh dana cepat","URGENCY",3),
  new Signal("Jual cepat","URGENCY",3),
  new Signal("Jual rugi","PRICE_CLAIM",2),
  new Signal("Pemakaian pribadi","OWNERSHIP_CLAIM",1),
  new Signal("Atas nama pribadi","OWNERSHIP_CLAIM",1)
 );
 public static List<Signal> all(){return SIGNALS;}
 public static List<Signal> match(String text) {
  String normalized=normalize(text);
  List<Signal> result=new ArrayList<>();
  for(Signal signal:SIGNALS){
   String phrase=Pattern.quote(normalize(signal.phrase()));
   if(Pattern.compile("(?<![\\p{L}\\p{N}])"+phrase+"(?![\\p{L}\\p{N}])",Pattern.UNICODE_CHARACTER_CLASS)
       .matcher(normalized).find())result.add(signal);
  }
  return result;
 }
 private static String normalize(String text) {
  if(text==null)return "";
  return Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
    .replaceAll("\\s+"," ").trim();
 }
 public static int priority(List<Signal> matches) {
  return matches.stream().mapToInt(Signal::priority).max().orElse(0);
 }
}
