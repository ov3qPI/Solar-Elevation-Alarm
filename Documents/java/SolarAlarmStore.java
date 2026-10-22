package org.solarelevation.alarm;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;
public final class SolarAlarmStore extends SQLiteOpenHelper {
public static final String SOURCE_ID="SolarAlarmStore_v20261022T031609Z";
public static final class Event {public long id,next,gen;public String name="",angle="0",uri="",file="",status="Not scheduled",result="Never fired";public int direction=1,enabled=1,nextDirection=1;}
public SolarAlarmStore(Context c){super(c,"solar_alarm.db",null,2);}
public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,angle TEXT NOT NULL,direction INTEGER NOT NULL,uri TEXT NOT NULL,file TEXT NOT NULL,enabled INTEGER NOT NULL,next INTEGER NOT NULL DEFAULT 0,gen INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'Not scheduled',claimed_gen INTEGER NOT NULL DEFAULT -1,last_result TEXT NOT NULL DEFAULT 'Never fired',next_direction INTEGER NOT NULL DEFAULT 1)");}
public void onUpgrade(SQLiteDatabase db,int old,int current){if(old<2)db.execSQL("ALTER TABLE events ADD COLUMN next_direction INTEGER NOT NULL DEFAULT 1");}
private Event read(Cursor c){Event e=new Event();e.id=c.getLong(0);e.name=c.getString(1);e.angle=c.getString(2);e.direction=c.getInt(3);e.uri=c.getString(4);e.file=c.getString(5);e.enabled=c.getInt(6);e.next=c.getLong(7);e.gen=c.getLong(8);e.status=c.getString(9);e.result=c.getString(10);e.nextDirection=c.getInt(11);return e;}
private static final String COLUMNS="id,name,angle,direction,uri,file,enabled,next,gen,status,last_result,next_direction";
public synchronized List<Event> all(){ArrayList<Event> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT "+COLUMNS+" FROM events ORDER BY id ASC",null)){while(c.moveToNext())out.add(read(c));}return out;}
public synchronized Event get(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT "+COLUMNS+" FROM events WHERE id=?",new String[]{Long.toString(id)})){return c.moveToFirst()?read(c):null;}}
public synchronized long save(Event e){ContentValues v=new ContentValues();v.put("name",e.name);v.put("angle",e.angle);v.put("direction",e.direction);v.put("uri",e.uri);v.put("file",e.file);v.put("enabled",e.enabled);v.put("next",0);v.put("status",e.enabled==1?"Computing":"Disabled");v.put("gen",e.gen+1);v.put("next_direction",e.direction==-1?-1:1);if(e.id==0)return getWritableDatabase().insertOrThrow("events",null,v);getWritableDatabase().update("events",v,"id=?",new String[]{Long.toString(e.id)});return e.id;}
public synchronized long setSchedule(long id,long next,String status){return setSchedule(id,next,status,1);}
public synchronized long setSchedule(long id,long next,String status,int resolvedDirection){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long gen=0;try(Cursor c=db.rawQuery("SELECT gen FROM events WHERE id=?",new String[]{Long.toString(id)})){if(c.moveToFirst())gen=c.getLong(0)+1;else return -1;}ContentValues v=new ContentValues();v.put("next",next);v.put("status",status);v.put("gen",gen);v.put("next_direction",resolvedDirection<0?-1:1);db.update("events",v,"id=?",new String[]{Long.toString(id)});db.setTransactionSuccessful();return gen;}finally{db.endTransaction();}}
public synchronized boolean setEnabled(long id,boolean enabled){SQLiteDatabase db=getWritableDatabase();db.execSQL("UPDATE events SET enabled=?,next=0,status=?,gen=gen+1 WHERE id=?",new Object[]{enabled?1:0,enabled?"Computing":"Disabled",id});return get(id)!=null;}
public synchronized boolean claim(long id,long gen,long next){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("status","Triggered");v.put("claimed_gen",gen);return db.update("events",v,"id=? AND gen=? AND next=? AND enabled=1 AND claimed_gen<>?",new String[]{Long.toString(id),Long.toString(gen),Long.toString(next),Long.toString(gen)})==1;}
public synchronized void status(long id,String message){ContentValues v=new ContentValues();v.put("last_result",message);getWritableDatabase().update("events",v,"id=?",new String[]{Long.toString(id)});}
public synchronized void delete(long id){getWritableDatabase().delete("events","id=?",new String[]{Long.toString(id)});}
}
