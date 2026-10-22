package org.solarelevation.alarm;
import android.content.*;
import android.net.*;
import android.os.*;
import java.io.*;
public final class SolarAlarmWavPack {
public static final String SOURCE_ID="SolarAlarmWavPack_v20261022T015350Z";
static {System.loadLibrary("solarwavpack");}
static native long nativeOpen(int fd) throws IOException;
static native int[] nativeInfo(long handle);
static native int nativeRead(long handle,float[] buffer,int frames) throws IOException;
static native void nativeClose(long handle);
public static void check(Context c,String uri) throws IOException {try(ParcelFileDescriptor pfd=c.getContentResolver().openFileDescriptor(Uri.parse(uri),"r")){if(pfd==null)throw new IOException("Cannot open WavPack document");long h=nativeOpen(pfd.getFd());try{int[] info=nativeInfo(h);if(info==null||info.length<3||info[0]<8000||info[0]>192000||info[1]<1||info[1]>2)throw new IOException("WavPack must be 8–192 kHz mono or stereo PCM/float");}finally{nativeClose(h);}}catch(LinkageError ex){throw new IOException("Native WavPack decoder unavailable: "+ex.getMessage(),ex);}}
private SolarAlarmWavPack(){}
}
