package org.shortfuse.reactnativepcmaudio;

import android.annotation.TargetApi;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import android.os.Build;

import android.util.Base64;
import android.util.Base64InputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class PcmAudioModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

  public PcmAudioModule(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public void onHostResume() {
  }

  @Override
  public void onHostPause() {
  }

  @Override
  public void onHostDestroy() {
    Iterator it = this.audioTracks.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      ((AudioTrack)entry.getValue()).release();
      it.remove();
    }
  }

  HashMap<Integer, AudioTrack> audioTracks = new HashMap<Integer, AudioTrack>();

  @Override
  public String getName() {
    return "PcmAudio";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    return constants;
  }

  @ReactMethod
  public void build(ReadableMap options, final Callback callback) {
    DecodedOptions decodedOptions = new DecodedOptions(options);
    AudioTrack audioTrack = getAudioTrack(decodedOptions);
    if (options.hasKey("onMarkerReached") || options.hasKey("onPeriodicNotification")) {
      audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
        @Override
        public void onMarkerReached (AudioTrack track) {
          callback.invoke("onMarkerReached");
        }
        
        @Override
        public void onPeriodicNotification (AudioTrack track) {
          callback.invoke("onPeriodicNotification");
        }
      });
    }
    int sessionId = audioTrack.getAudioSessionId();
    this.audioTracks.put(sessionId, audioTrack);
    callback.invoke("onSessionId", sessionId);
  }
  
  @ReactMethod
  public void flush(int sessionId) {
    this.audioTracks.get(sessionId).flush();
  }
  
  @ReactMethod
  public boolean write(int sessionId, String base64Data) {
    InputStream stream = new ByteArrayInputStream(base64Data.getBytes());
    Base64InputStream decoder = new Base64InputStream(stream, Base64.DEFAULT);
    try {
      byte[] buffer = new byte[mBase64BufferSize];
      int len;
      while ((len = decoder.read(buffer)) != -1) {
        this.audioTracks.get(sessionId).write(buffer, 0, len);
      }
      decoder.close();
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      try {
        if (stream != null) stream.close();
      } catch (IOException e) {
      }
    }
  }
  
  @ReactMethod
  public void stop(int sessionId) {
    this.audioTracks.get(sessionId).stop();
  }
  
  @ReactMethod
  public void play(int sessionId) {
    this.audioTracks.get(sessionId).play();
  }
  
  @ReactMethod
  public void release(int sessionId) {
    this.audioTracks.get(sessionId).release();
    this.audioTracks.remove(sessionId);
  }
  
  @ReactMethod
  public int getNotificationMarkerPosition(int sessionId) {
    return this.audioTracks.get(sessionId).getNotificationMarkerPosition();
  }
  
  @ReactMethod
  public int getSampleRate(int sessionId) {
    return this.audioTracks.get(sessionId).getSampleRate();
  }

  @ReactMethod
  public boolean setPositionNotificationPeriod(int sessionId, int periodInFrames) {
    return (this.audioTracks.get(sessionId).setPositionNotificationPeriod(periodInFrames) == 0);
  }

  private static final int mBase64BufferSize = 1024;

  private AudioTrack getAudioTrack(DecodedOptions options) {
    if (Build.VERSION.SDK_INT >= 23) {
      return buildMarshmallow(options);
    }
    if (Build.VERSION.SDK_INT >= 21) {
      return buildLollipop(options);
    }
    return buildPreLollipop(options);
  }

  private class DecodedOptions {
    public int encoding;
    public int streamType;
    public int sampleRateInHz;
    public int channelMask;
    public int audioFormat;
    public int bufferSizeInBytes;
    public int mode;
    public int sessionId;
    public int usage;
    public int contentType;

    public DecodedOptions(ReadableMap options) {
      this.encoding = AudioFormat.ENCODING_DEFAULT;
      this.channelMask = AudioFormat.CHANNEL_OUT_DEFAULT;
      this.mode = AudioTrack.MODE_STATIC;
      this.usage = android.media.AudioAttributes.USAGE_UNKNOWN;
      this.contentType = android.media.AudioAttributes.CONTENT_TYPE_UNKNOWN;
      ReadableMapKeySetIterator iterator = options.keySetIterator();
      while(iterator.hasNextKey()) {
        String key = iterator.nextKey();
        switch(key) {
          case "encoding":
            switch(options.getString(key).toLowerCase()) {
              case "ac3":
              case "ac-3":
                this.encoding = AudioFormat.ENCODING_AC3;
                break;
              case "truehd":
              case "dolbytruehd":
                this.encoding = AudioFormat.ENCODING_DOLBY_TRUEHD;
                break;
              case "dts":
              case "dolbydts":
                this.encoding = AudioFormat.ENCODING_DTS;
                break;
              case "eac3":
              case "eac-3":
              case "e-ac3":
              case "e-ac-3":
                this.encoding = AudioFormat.ENCODING_E_AC3;
                break;
              case "iec61937":
                this.encoding = AudioFormat.ENCODING_IEC61937;
                break;
              case "pcm16bit":
              case "16bit":
              case "16":
                this.encoding = AudioFormat.ENCODING_PCM_16BIT;
                break;
              case "pcm8bit":
              case "8bit":
              case "8":
                this.encoding = AudioFormat.ENCODING_PCM_8BIT;
                break;
              case "pcmfloat":
              case "float":
                this.encoding = AudioFormat.ENCODING_PCM_FLOAT;
                break;
            }
            break;
          case "type":
          case "usage":
          case "streamType":
            switch(options.getString(key).toLowerCase()) {
              case "alarm":
                this.usage = android.media.AudioAttributes.USAGE_ALARM;
                this.streamType = AudioManager.STREAM_ALARM;
                break;
              case "assistanceaccessibility":
              case "accessibility":
                this.usage = android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY;
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_SPEECH;
                this.streamType = AudioManager.STREAM_SYSTEM;
                break;
              case "assistant":
                this.usage = 0x10; //android.media.AudioAttributes.USAGE_ASSISTANT
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_SPEECH;
                this.streamType = AudioManager.STREAM_MUSIC;
                break;
              case "assistancenavigationguidance":
              case "navigationguidance":
                this.usage = android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_SPEECH;
                this.streamType = AudioManager.STREAM_NOTIFICATION;
                break;
              case "assistancesonification":
              case "sonification":
              case "system":
                this.usage = android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION;
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION;
                this.streamType = AudioManager.STREAM_SYSTEM;
                break;
              case "game":
                this.usage = android.media.AudioAttributes.USAGE_GAME;
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION;
                this.streamType = AudioManager.STREAM_MUSIC;
                break;
              case "movie":
                this.usage = android.media.AudioAttributes.USAGE_MEDIA;
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_MOVIE;
                this.streamType = AudioManager.STREAM_MUSIC;
                break;
              case "music":
                this.usage = android.media.AudioAttributes.USAGE_MEDIA;
                this.contentType = android.media.AudioAttributes.CONTENT_TYPE_MUSIC;
                this.streamType = AudioManager.STREAM_MUSIC;
                break;
              case "notification":
                this.usage = android.media.AudioAttributes.USAGE_NOTIFICATION;
                this.streamType = AudioManager.STREAM_NOTIFICATION;
                break;
              case "notificationcommunicationdelayed":
              case "notificationdelayed":
                this.usage = android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED;
                this.streamType = AudioManager.STREAM_NOTIFICATION;
                break;
              case "notificationcommunicationinstant":
              case "notificationinstant":
                this.usage = android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT;
                this.streamType = AudioManager.STREAM_NOTIFICATION;
                break;
              case "notificationcommunicationrequest":
              case "notificationrequest":
                this.usage = android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST;
                this.streamType = AudioManager.STREAM_NOTIFICATION;
                break;
              case "notificationevent":
                this.usage = android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT;
                this.streamType = AudioManager.STREAM_NOTIFICATION;
                break;
              case "notificationringtone":
              case "ring":
              case "ringtone":
                this.usage = android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
                this.streamType = AudioManager.STREAM_RING;
                break;
              case "voice":
              case "voicecommunication":
              case "voicecall":
                this.usage = android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION;
                this.streamType = AudioManager.STREAM_VOICE_CALL;
                break;
              case "voicecommunicationsignalling":
              case "voicesignal":
              case "voicesignalling":
              case "dtmf":
                this.usage = android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING;
                this.streamType = AudioManager.STREAM_DTMF;
                break;
            }
            break;
          case "sampleRate":
          case "sampleRateInHz":
            this.sampleRateInHz = options.getInt(key);
            break;
          case "channels":
            int channels = options.getInt(key);
            if (channels == 1) {
              this.channelMask = AudioFormat.CHANNEL_OUT_MONO;
            } else if (channels == 2) {
              this.channelMask = AudioFormat.CHANNEL_OUT_STEREO;
            } else if (channels > 2) {
              this.channelMask = AudioFormat.CHANNEL_OUT_SURROUND;
            }
            break;
          case "bufferSize":
          case "bufferSizeInBytes":
            this.bufferSizeInBytes = options.getInt(key);
            break;
          case "mode":
            switch(options.getString(key)) {
              case "streaming":
              case "stream":
              case "forever":
                this.mode = AudioTrack.MODE_STREAM;
                break;
              case "playOnce":
              case "static":
              case "oneTime":
              case "once":
                this.mode = AudioTrack.MODE_STATIC;
                break;
            }
            break;
          case "id":
          case "session":
          case "sessionId":
            this.sessionId = options.getInt(key);
            break;
        }
      }
    }
  }

  @TargetApi(23)
  private AudioTrack buildMarshmallow(DecodedOptions options) {
    int bufferSize;
    if (options.bufferSizeInBytes != 0) {
      bufferSize = options.bufferSizeInBytes;
    } else {
      bufferSize = AudioTrack.getMinBufferSize(
        options.sampleRateInHz,
        options.channelMask,
        options.encoding);
    }
    return new AudioTrack.Builder()
      .setAudioAttributes(new android.media.AudioAttributes.Builder()
        .setUsage(options.usage)
        .setContentType(options.contentType)
        .build())
      .setAudioFormat(new AudioFormat.Builder()
        .setEncoding(options.encoding)
        .setSampleRate(options.sampleRateInHz)
        .setChannelMask(options.channelMask)
        .build())
      .setBufferSizeInBytes(bufferSize)
      .setTransferMode(options.mode)
      .build();
  }

  @TargetApi(21)
  private AudioTrack buildLollipop(DecodedOptions options) {
    int bufferSize;
    if (options.bufferSizeInBytes != 0) {
      bufferSize = options.bufferSizeInBytes;
    } else {
      bufferSize = AudioTrack.getMinBufferSize(
        options.sampleRateInHz,
        options.channelMask,
        options.encoding);
    }
    return new AudioTrack(
      new android.media.AudioAttributes.Builder()
        .setUsage(options.usage)
        .setContentType(options.contentType)
        .build(),
      new AudioFormat.Builder()
        .setEncoding(options.encoding)
        .setSampleRate(options.sampleRateInHz)
        .setChannelMask(options.channelMask)
        .build(),
      bufferSize,
      options.mode,
      AudioManager.AUDIO_SESSION_ID_GENERATE);
  }

  @Deprecated
  private AudioTrack buildPreLollipop(DecodedOptions options) {
    int bufferSize;
    if (options.bufferSizeInBytes != 0) {
      bufferSize = options.bufferSizeInBytes;
    } else {
      bufferSize = AudioTrack.getMinBufferSize(
        options.sampleRateInHz,
        options.channelMask,
        options.encoding);
    }
    return new AudioTrack(
      options.streamType,
      options.sampleRateInHz,
      options.channelMask,
      bufferSize,
      options.mode,
      0);
  }
}
