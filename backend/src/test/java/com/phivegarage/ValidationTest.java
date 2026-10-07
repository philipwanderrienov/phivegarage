package com.phivegarage;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.*;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
class ValidationTest {
 final ObjectMapper json=new ObjectMapper();
 ObjectNode assessment()throws Exception{return (ObjectNode)json.readTree("""
 {"lotId":"lot1","demandScore":80,"liquidityScore":75,"conditionScore":70,"repairEstimate":2000000,"indicativeSellPrice":null,"daysMin":null,"daysMax":null,"reasons":["Periksa unit"],"risks":[]}
 """);}
 @Test void missingScoreCannotSilentlyBecomeZero()throws Exception{var n=assessment();n.remove("demandScore");assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void textNumberCannotBeCoerced()throws Exception{var n=assessment();n.put("repairEstimate","2000000");assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void nullScoreRejected()throws Exception{var n=assessment();n.putNull("conditionScore");assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void reversedDaysRejected()throws Exception{var n=assessment();n.put("daysMin",21);n.put("daysMax",7);assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void partialDaysRejected()throws Exception{var n=assessment();n.put("daysMin",7);assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void nullMarketEstimateAccepted()throws Exception{var a=Jobs.readAssessment(json,assessment());assertNull(a.indicativeSellPrice());assertNull(a.daysMin());}
 @Test void outOfRangeScoreRejected()throws Exception{var n=assessment();n.put("liquidityScore",101);assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void nullReasonRejected()throws Exception{var n=assessment();n.putArray("reasons").addNull();assertThrows(IllegalStateException.class,()->Jobs.readAssessment(json,n));}
 @Test void corruptPdfHasActionableMessage(){var e=assertThrows(IllegalArgumentException.class,()->PdfFiles.validate("%PDF-not-a-document".getBytes()));assertTrue(e.getMessage().contains("Ekspor ulang"));}
 @Test void validPdfAccepted()throws Exception{try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){doc.addPage(new PDPage());doc.save(out);assertEquals(1,PdfFiles.validate(out.toByteArray()));}}
 @Test void passwordPdfHasActionableMessage()throws Exception{try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){doc.addPage(new PDPage());doc.protect(new StandardProtectionPolicy("owner-password","user-password",new AccessPermission()));doc.save(out);var e=assertThrows(IllegalArgumentException.class,()->PdfFiles.validate(out.toByteArray()));assertTrue(e.getMessage().contains("tanpa password"));}}
}
