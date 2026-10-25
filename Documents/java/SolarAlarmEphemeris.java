package org.solarelevation.alarm;
import java.io.*;
import java.nio.*;
import java.security.*;
import java.net.*;
import java.util.*;
import android.content.*;
public final class SolarAlarmEphemeris implements AutoCloseable {
public static final String SOURCE_ID="SolarAlarmEphemeris_v20261025T010732Z";
public static final String KERNEL="de442s.bsp",URL=SolarAlarmDatasets.url(SolarAlarmDatasets.SPK);
private final RandomAccessFile data;
private SolarAlarmTiming timing;
private final Map<Integer,Segment> segments=new HashMap<>();
private static final class Segment { int target,center,frame,type,start,end,count,size; double begin,finish,init,interval; int cached=-1; double[] terms; }
private double number(long offset) throws IOException { byte[] b=new byte[8];data.seek(offset);data.readFully(b);return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble(); }
private int integer(long offset) throws IOException { byte[] b=new byte[4];data.seek(offset);data.readFully(b);return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt(); }
public SolarAlarmEphemeris(File file) throws IOException {data=new RandomAccessFile(file,"r");try {byte[] header=new byte[96];data.readFully(header);String id=new String(header,0,7,"US-ASCII");String endian=new String(header,88,8,"US-ASCII");int nd=integer(8),ni=integer(12),record=integer(76);if(!id.equals("DAF/SPK")||!endian.startsWith("LTL-IEEE")||nd!=2||ni!=6||record<2)throw new IOException("Unsupported or invalid JPL SPK header");int visited=0;while(record!=0&&visited++<1000){long base=(long)(record-1)*1024;int next=(int)Math.round(number(base)),n=(int)Math.round(number(base+16));if(n<0||n>25)throw new IOException("Malformed SPK summary");for(int i=0;i<n;i++){long at=base+24L+i*40;Segment s=new Segment();s.begin=number(at);s.finish=number(at+8);s.target=integer(at+16);s.center=integer(at+20);s.frame=integer(at+24);s.type=integer(at+28);s.start=integer(at+32);s.end=integer(at+36);if(s.type==2&&s.frame==1&&((s.target==10&&s.center==0)||(s.target==3&&s.center==0)||(s.target==399&&s.center==3))){long tail=((long)s.end-4)*8;s.init=number(tail);s.interval=number(tail+8);s.size=(int)Math.round(number(tail+16));s.count=(int)Math.round(number(tail+24));if(s.size>=5&&s.size<=256&&s.count>0&&s.interval>0&&s.end<=data.length()/8)segments.put(s.target,s);}}record=next;}if(!segments.containsKey(10)||!segments.containsKey(3)||!segments.containsKey(399))throw new IOException("Kernel lacks required Sun, Earth-Moon barycenter or Earth SPK records");}catch(IOException|RuntimeException e){data.close();throw e;}}
public SolarAlarmEphemeris(File file,Context context) throws IOException {this(file);try{timing=SolarAlarmTiming.get(context);}catch(IOException ex){close();throw ex;}}
public SolarAlarmTiming timing() throws IOException {if(timing==null)throw new IOException("Timing datasets not loaded");return timing;}
private synchronized double[] vector(int body,double et) throws IOException {Segment s=segments.get(body);if(s==null||et<s.begin||et>s.finish)throw new IOException("Requested time outside DE440s coverage");int idx=(int)Math.floor((et-s.init)/s.interval);idx=Math.max(0,Math.min(s.count-1,idx));if(idx!=s.cached){long offset=((long)s.start-1L+(long)idx*s.size)*8;byte[] raw=new byte[s.size*8];data.seek(offset);data.readFully(raw);ByteBuffer buffer=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);s.terms=new double[s.size];for(int i=0;i<s.size;i++)s.terms[i]=buffer.getDouble();s.cached=idx;}double u=(et-s.terms[0])/s.terms[1];if(Math.abs(u)>1.000001)throw new IOException("Invalid SPK Chebyshev interval");int nc=(s.size-2)/3;double[] result=new double[3];for(int k=0;k<3;k++){double b0=0,b1=0,b2;for(int j=nc-1;j>=1;j--){b2=b1;b1=b0;b0=2*u*b1-b2+s.terms[2+k*nc+j];}result[k]=u*b0-b1+s.terms[2+k*nc];}return result;}
public double[] earthSun(double et) throws IOException {double[] emb=vector(3,et), earthRel=vector(399,et),sun=vector(10,et-499.0);for(int i=0;i<3;i++)sun[i]-=emb[i]+earthRel[i];return sun;}
public void close() throws IOException {data.close();}
public static File path(Context c){return SolarAlarmDatasets.file(c,SolarAlarmDatasets.SPK);}
public static boolean ready(Context c){return SolarAlarmDatasets.ready(c,SolarAlarmDatasets.SPK);}
public static String hash(File file) throws Exception {return SolarAlarmDatasets.hash(file);}
public static void verify(Context c) throws Exception {SolarAlarmDatasets.verify(c,SolarAlarmDatasets.SPK);}
public interface Progress {void update(long bytes,long total);}
public static String download(Context c,Progress listener) throws Exception {return SolarAlarmDatasets.download(c,SolarAlarmDatasets.SPK);}
}
