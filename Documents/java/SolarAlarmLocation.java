package org.solarelevation.alarm;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import java.util.*;
public final class SolarAlarmLocation {
public static final String SOURCE_ID="SolarAlarmLocation_v20261022T025042Z";
public static boolean foreground(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
public static boolean background(Context c){return foreground(c)&&c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;}
public static synchronized int save(Context context,Location fix){if(fix==null)return 0;double lat=fix.getLatitude(),lon=fix.getLongitude();if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat < -90||lat > 90||lon < -180||lon > 180)return 0;Context c=context.getApplicationContext();SharedPreferences p=c.getSharedPreferences("position",0);long previousTime=p.getLong("time",0),timestamp=fix.getTime();if(timestamp<=0||timestamp>System.currentTimeMillis()+120000L)return 0;if(p.contains("lat")&&timestamp<previousTime)return 0;double previousLat=Double.longBitsToDouble(p.getLong("lat",0)),previousLon=Double.longBitsToDouble(p.getLong("lon",0));boolean changed=!p.contains("lat")||Math.abs(lat-previousLat)>0.0005||Math.abs(lon-previousLon)>0.0005;boolean saved=p.edit().putLong("lat",Double.doubleToLongBits(lat)).putLong("lon",Double.doubleToLongBits(lon)).putLong("height",Double.doubleToLongBits(fix.hasAltitude()?fix.getAltitude():0)).putLong("time",timestamp).putString("provider",fix.getProvider()==null?"unknown":fix.getProvider()).putFloat("accuracy",fix.hasAccuracy()?fix.getAccuracy():-1f).commit();return saved?(changed?2:1):0;}
public static void rearm(Context c){SolarAlarmStore db=new SolarAlarmStore(c);try{for(SolarAlarmStore.Event e:db.all()){if(Thread.currentThread().isInterrupted())return;if(e.enabled==1)SolarAlarmSchedule.arm(c,e.id);}}finally{db.close();}}
private SolarAlarmLocation(){}
}
