#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <errno.h>
#include <math.h>
#include <string.h>
#include "wavpack.h"
#define FRAMES 4096
#define CHANNELS 2
typedef struct {int fd;int64_t pos;int64_t length;WavpackContext *ctx;int channels;int bits;int mode;int errors;} Decoder;
static void error(JNIEnv *env,const char *msg){jclass cls=(*env)->FindClass(env,"java/io/IOException");if(cls)(*env)->ThrowNew(env,cls,msg);}
static int32_t cb_read(void *id,void *dst,int32_t amount){Decoder *d=(Decoder*)id;if(amount<0)return 0;ssize_t n=pread(d->fd,dst,(size_t)amount,(off_t)d->pos);if(n>0){d->pos+=n;return (int32_t)n;}return 0;}
static int32_t cb_write(void *id,void *src,int32_t amount){(void)id;(void)src;(void)amount;return 0;}
static int64_t cb_get_pos(void *id){return ((Decoder*)id)->pos;}
static int cb_abs(void *id,int64_t pos){Decoder *d=(Decoder*)id;if(pos<0||pos>d->length)return -1;d->pos=pos;return 0;}
static int cb_rel(void *id,int64_t offset,int mode){Decoder *d=(Decoder*)id;int64_t base=mode==SEEK_SET?0:mode==SEEK_CUR?d->pos:mode==SEEK_END?d->length:-1;if(base<0||offset>INT64_MAX-base||offset< -base)return -1;return cb_abs(id,base+offset);}
static int cb_push(void *id,int value){Decoder *d=(Decoder*)id;if(d->pos<=0)return EOF;d->pos--;return value;}
static int64_t cb_length(void *id){return ((Decoder*)id)->length;}
static int cb_can_seek(void *id){(void)id;return 1;}
static int cb_truncate(void *id){(void)id;return -1;}
static int cb_close(void *id){(void)id;return 0;}
static WavpackStreamReader64 reader={cb_read,cb_write,cb_get_pos,cb_abs,cb_rel,cb_push,cb_length,cb_can_seek,cb_truncate,cb_close};
JNIEXPORT jlong JNICALL Java_org_solarelevation_alarm_SolarAlarmWavPack_nativeOpen(JNIEnv *env,jclass cls,jint fd){(void)cls;int own=dup(fd);if(own<0){error(env,"Cannot duplicate WavPack file descriptor");return 0;}struct stat statbuf;if(fstat(own,&statbuf)||statbuf.st_size<=0||lseek(own,0,SEEK_CUR)<0){close(own);error(env,"WavPack document must be locally stored and seekable");return 0;}Decoder *d=(Decoder*)calloc(1,sizeof(Decoder));if(!d){close(own);error(env,"Decoder memory exhausted");return 0;}d->fd=own;d->length=statbuf.st_size;char message[128]={0};d->ctx=WavpackOpenFileInputEx64(&reader,d,NULL,message,OPEN_NORMALIZE,0);if(!d->ctx){close(own);free(d);error(env,*message?message:"WavPack header rejected");return 0;}d->channels=WavpackGetNumChannels(d->ctx);d->bits=WavpackGetBitsPerSample(d->ctx);d->mode=WavpackGetMode(d->ctx);if(d->channels<1||d->channels>CHANNELS||d->bits<1||d->bits>32||WavpackGetQualifyMode(d->ctx)&QMODE_DSD_AUDIO){WavpackCloseFile(d->ctx);close(own);free(d);error(env,"Unsupported WavPack channels, sample format or DSD audio");return 0;}return (jlong)(intptr_t)d;}
JNIEXPORT jintArray JNICALL Java_org_solarelevation_alarm_SolarAlarmWavPack_nativeInfo(JNIEnv *env,jclass cls,jlong handle){(void)cls;Decoder *d=(Decoder*)(intptr_t)handle;if(!d)return NULL;jint v[]={WavpackGetSampleRate(d->ctx),d->channels,d->bits};jintArray out=(*env)->NewIntArray(env,3);if(out)(*env)->SetIntArrayRegion(env,out,0,3,v);return out;}
JNIEXPORT jint JNICALL Java_org_solarelevation_alarm_SolarAlarmWavPack_nativeRead(JNIEnv *env,jclass cls,jlong handle,jfloatArray out,jint frames){(void)cls;Decoder *d=(Decoder*)(intptr_t)handle;if(!d||!out||frames<=0||frames>FRAMES||(*env)->GetArrayLength(env,out)<frames*d->channels){error(env,"Invalid WavPack read buffer");return -1;}int32_t samples[FRAMES*CHANNELS];float result[FRAMES*CHANNELS];uint32_t count=WavpackUnpackSamples(d->ctx,samples,(uint32_t)frames);int errors=WavpackGetNumErrors(d->ctx);if(errors>d->errors){d->errors=errors;error(env,"WavPack checksum/decoding error");return -1;}if(!count)return 0;int limit=(int)count*d->channels;if(d->mode&MODE_FLOAT){for(int i=0;i<limit;i++){float value;memcpy(&value,samples+i,sizeof(value));result[i]=isfinite(value)?fmaxf(-1.0f,fminf(1.0f,value)):0.0f;}}else{float scale=ldexpf(1.0f,d->bits-1);for(int i=0;i<limit;i++)result[i]=fmaxf(-1.0f,fminf(1.0f,(float)samples[i]/scale));}(*env)->SetFloatArrayRegion(env,out,0,limit,result);return (jint)count;}
JNIEXPORT void JNICALL Java_org_solarelevation_alarm_SolarAlarmWavPack_nativeClose(JNIEnv *env,jclass cls,jlong handle){(void)env;(void)cls;Decoder *d=(Decoder*)(intptr_t)handle;if(!d)return;if(d->ctx)WavpackCloseFile(d->ctx);close(d->fd);free(d);}
