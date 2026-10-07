package com.phivegarage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
final class PdfFiles {
 static int validate(byte[] bytes){
  if(bytes.length==0||bytes.length>20*1024*1024)throw new IllegalArgumentException("PDF harus berisi data dan maksimal 20 MB");
  if(bytes.length<5||!new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))throw new IllegalArgumentException("File harus PDF valid");
  try(var doc=Loader.loadPDF(bytes)){
   if(doc.isEncrypted())throw new IllegalArgumentException("PDF dilindungi password. Upload salinan tanpa password.");
   int pages=doc.getNumberOfPages();if(pages<1||pages>150)throw new IllegalArgumentException("PDF harus berisi 1–150 halaman");return pages;
  }catch(InvalidPasswordException e){throw new IllegalArgumentException("PDF dilindungi password. Upload salinan tanpa password.");}
  catch(IOException e){throw new IllegalArgumentException("PDF rusak atau tidak dapat dibaca. Ekspor ulang PDF lalu coba kembali.");}
 }
}
