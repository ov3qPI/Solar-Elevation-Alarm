package org.solarelevation.alarm;
import android.content.*;
public final class SolarAlarmSystemReceiver extends BroadcastReceiver {
public static final String SOURCE_ID="SolarAlarmSystemReceiver_v20261022T025042Z";
public void onReceive(Context context,Intent intent){PendingResult result=goAsync();Context c=context.getApplicationContext();new Thread(()->{try{SolarAlarmMaintenanceJob.ensure(c);SolarAlarmSchedule.restore(c);}finally{result.finish();}},"solar-alarm-restore").start();}
}
