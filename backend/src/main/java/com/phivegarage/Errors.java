package com.phivegarage;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;
@RestControllerAdvice
public class Errors {
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalidBody(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message","Parameter tidak valid: "+e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+" "+x.getDefaultMessage()).toList()));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> tooLarge(){return ResponseEntity.status(413).body(Map.of("message","PDF maksimal 20 MB"));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("message","Nama balai atau nomor lot sudah dipakai, atau data melanggar aturan database"));}
}
