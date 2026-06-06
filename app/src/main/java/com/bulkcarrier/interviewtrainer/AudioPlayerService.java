package com.bulkcarrier.interviewtrainer;
import android.app.*; import android.content.*; import android.os.*; import android.speech.tts.*; import java.util.*;
public class AudioPlayerService extends Service implements TextToSpeech.OnInitListener{
 public static final String ACTION_PLAY_ALL="PLAY_ALL",ACTION_PLAY_FROM="PLAY_FROM",ACTION_PAUSE="PAUSE",ACTION_RESUME="RESUME",ACTION_STOP="STOP";
 public static final String EXTRA_START_ID="START_ID",EXTRA_SECTION="SECTION",EXTRA_READ_RU="READ_RU";
 private static final String CHANNEL="bulk_audio"; private TextToSpeech tts; private ArrayList<Question> all=new ArrayList<>(), list=new ArrayList<>();
 private int idx=0, part=0; private boolean playing=false, readRu=true; private PowerManager.WakeLock wl;
 public void onCreate(){super.onCreate(); channel(); all=QuestionRepository.load(this); tts=new TextToSpeech(this,this); PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE); wl=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"BulkInterview:Audio"); wl.setReferenceCounted(false);}
 public void onInit(int s){ if(s==TextToSpeech.SUCCESS&&tts!=null){ tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){ public void onStart(String u){} public void onDone(String u){ if(playing)new Handler(Looper.getMainLooper()).postDelayed(()->{advance(); speak();},500);} public void onError(String u){ if(playing){advance(); speak();}} });}}
 public int onStartCommand(Intent in,int flags,int startId){ if(in==null)return START_STICKY; String a=in.getAction();
  if(ACTION_PLAY_ALL.equals(a)||ACTION_PLAY_FROM.equals(a)){ readRu=in.getBooleanExtra(EXTRA_READ_RU,true); build(in.getStringExtra(EXTRA_SECTION)); int sid=in.getIntExtra(EXTRA_START_ID,-1); idx=0; if(sid>0)for(int i=0;i<list.size();i++)if(list.get(i).id==sid){idx=i;break;} part=0; playing=true; acquire(); startForeground(1, notif("Starting...")); speak(); }
  else if(ACTION_PAUSE.equals(a)){ if(tts!=null)tts.stop(); playing=false; update("Paused. Resume repeats current block.");}
  else if(ACTION_RESUME.equals(a)){ if(!list.isEmpty()){playing=true; acquire(); startForeground(1,notif("Resuming...")); speak();}}
  else if(ACTION_STOP.equals(a)){ stopPlay(); }
  return START_STICKY;
 }
 private void build(String sec){ list.clear(); if(sec==null)sec="All sections"; for(Question q:all)if(sec.equals("All sections")||q.section.equals(sec))list.add(q); }
 private void speak(){ if(!playing||idx>=list.size()){stopPlay();return;} Question q=list.get(idx); String text,status; Locale loc;
  if(part==0){text="Question "+q.id+". "+q.q; loc=Locale.US; status="Question "+q.id+" / "+list.size();}
  else if(part==1){text="Answer. "+q.a; loc=Locale.US; status="Answer "+q.id+" / "+list.size();}
  else { if(!readRu){advance(); speak(); return;} text="Объяснение. "+q.ru; loc=new Locale("ru","RU"); status="RU explanation "+q.id+" / "+list.size(); }
  update(status+" — "+q.section); if(tts==null)return; tts.setLanguage(loc); tts.setSpeechRate(loc.getLanguage().equals("ru")?0.95f:0.90f); Bundle b=new Bundle(); String uid="u"+q.id+"_"+part; b.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,uid); tts.speak(text,TextToSpeech.QUEUE_FLUSH,b,uid);
 }
 private void advance(){part++; if(part>2){part=0; idx++;}}
 private void stopPlay(){playing=false; if(tts!=null)tts.stop(); release(); stopForeground(true); stopSelf();}
 private void acquire(){try{if(wl!=null&&!wl.isHeld())wl.acquire(8*60*60*1000L);}catch(Exception e){}}
 private void release(){try{if(wl!=null&&wl.isHeld())wl.release();}catch(Exception e){}}
 private void update(String t){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); if(nm!=null)nm.notify(1,notif(t));}
 private Notification notif(String t){ PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent pause=PendingIntent.getService(this,2,new Intent(this,AudioPlayerService.class).setAction(ACTION_PAUSE),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent resume=PendingIntent.getService(this,3,new Intent(this,AudioPlayerService.class).setAction(ACTION_RESUME),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,4,new Intent(this,AudioPlayerService.class).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,CHANNEL).setContentTitle("Bulk Carrier Interview Trainer").setContentText(t).setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_media_pause,"Pause",pause).addAction(android.R.drawable.ic_media_play,"Resume",resume).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Stop",stop).build();}
 private void channel(){ if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Interview Audio",NotificationManager.IMPORTANCE_LOW); NotificationManager nm=getSystemService(NotificationManager.class); if(nm!=null)nm.createNotificationChannel(ch);}}
 public void onDestroy(){ if(tts!=null){tts.stop();tts.shutdown();} release(); super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
