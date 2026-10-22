package org.solarelevation.alarm;
import android.content.*;
public final class SolarAlarmSystemReceiver extends BroadcastReceiver {
public static final String SOURCE_ID="SolarAlarmSystemReceiver_v20261022T151029Z";
public void onReceive(Context context,Intent intent){PendingResult result=goAsync();Context c=context.getApplicationContext();new Thread(()->{try{String action=intent==null?"":intent.getAction();if((Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_USER_UNLOCKED.equals(action))&&!SolarAlarmPlaybackService.isPlaybackActive())c.getSharedPreferences("playback",0).edit().putLong("active_id",-1).putString("state","idle").putLong("updated",System.currentTimeMillis()).commit();SolarAlarmMaintenanceJob.ensure(c);SolarAlarmSchedule.restore(c);}finally{result.finish();}},"solar-alarm-restore").start();}
}
