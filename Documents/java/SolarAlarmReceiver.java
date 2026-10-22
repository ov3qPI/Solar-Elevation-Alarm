package org.solarelevation.alarm;
import android.content.*;
import android.os.*;
public final class SolarAlarmReceiver extends BroadcastReceiver {
public static final String SOURCE_ID="SolarAlarmReceiver_v20261022T151029Z";
public void onReceive(Context context,Intent intent){PendingResult result=goAsync();Context c=context.getApplicationContext();new Thread(()->{try{if(!"org.solarelevation.alarm.FIRE".equals(intent.getAction()))return;long id=intent.getLongExtra(SolarAlarmSchedule.EXTRA_ID,-1),gen=intent.getLongExtra(SolarAlarmSchedule.EXTRA_GEN,-1),when=intent.getLongExtra(SolarAlarmSchedule.EXTRA_WHEN,-1);SolarAlarmStore store=new SolarAlarmStore(c);boolean valid=store.claim(id,gen,when);if(valid){store.status(id,"Triggered at "+System.currentTimeMillis());Intent play=new Intent(c,SolarAlarmPlaybackService.class).setAction("ALARM").putExtra("event_id",id);try{c.startForegroundService(play);}catch(Exception ex){store.status(id,"Playback start blocked: "+ex.getMessage());}SolarAlarmSchedule.arm(c,id);}store.close();}finally{result.finish();}},"solar-alarm-trigger").start();}
}
