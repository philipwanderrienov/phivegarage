package com.phivegarage;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.*;
import java.util.*;
@Repository
public class Store {
 final JdbcTemplate db; final ObjectMapper json;
 public Store(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("Data tidak valid");}}
 <T>T decode(String value,Class<T> type){try{return json.readValue(value,type);}catch(Exception e){throw new IllegalStateException("Data tersimpan tidak valid",e);}}
 Map<String,Object> normalize(Map<String,Object> row){
  for(String k:List.of("data","parameters","results","usage","input_lots","fee_snapshot","ai")) if(row.containsKey(k)&&row.get(k)!=null) try{row.put(k,json.readTree(row.get(k).toString()));}catch(Exception e){throw new IllegalStateException(e);}
  return row;
 }
 List<Map<String,Object>> list(String sql,Object...args){return db.queryForList(sql,args).stream().map(this::normalize).toList();}
 Map<String,Object> one(String sql,Object...args){var rows=list(sql,args);if(rows.isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Data tidak ditemukan");return rows.get(0);}
}
