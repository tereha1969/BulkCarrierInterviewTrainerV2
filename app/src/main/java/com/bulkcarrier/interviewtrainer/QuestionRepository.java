package com.bulkcarrier.interviewtrainer;
import android.content.Context; import org.json.*; import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*;
public class QuestionRepository {
 public static ArrayList<Question> load(Context c){
  ArrayList<Question> list=new ArrayList<>();
  try{
   InputStream is=c.getAssets().open("questions.json"); ByteArrayOutputStream bos=new ByteArrayOutputStream();
   byte[] buf=new byte[4096]; int n; while((n=is.read(buf))>0) bos.write(buf,0,n); is.close();
   JSONArray arr=new JSONArray(new String(bos.toByteArray(), StandardCharsets.UTF_8));
   for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i); list.add(new Question(o.getInt("id"),o.getString("section"),o.getString("q"),o.getString("a"),o.getString("ru")));}
  }catch(Exception e){e.printStackTrace();}
  return list;
 }
}
