# react-native-pcm-audio
Plays and record PCM audio

## Usage

### Build Session
`NativeModules.PcmAudio.build(pcmOptions, (event, data) => {...});`

**pcmOptions.encoding:**
 * `ac3 | ac-3` - AC3 Encoding
 * `truehd | dolbytruehd` - Dolby True HD Encoding
 * `dts | dolbydts` - Dolby DTS Encoding
 * `eac3 | eac-3 | e-ac3 | e-ac-3` E-AC3 Encoding
 * `iec61937` - IEC61937 Encoding
 * `pcm16bit | 16bit| 16` - PCM 16Bit Encoding
 * `pcm8bit | 8bit | 8` - PCM 8Bit Encoding
 * `pcmfloat | float` - PCM Float Encoding

**pcmOptions.usage:**
 * `alarm`
 * `assistanceaccessibility | accessibility`
 * `assistant`
 * `assistancenavigationguidance | navigationguidance`
 * `assistancesonification | sonification | system`
 * `dtmf | voicecommunicationsignalling | voicesignal | voicesignalling`
 * `game`
 * `movie`
 * `music`
 * `notification`
 * `notificationcommunicationdelayed | notificationdelayed`
 * `notificationcommunicationinstant | notificationinstant`
 * `notificationcommunicationrequest | notificationrequest`
 * `notificationevent`
 * `notificationringtone | ring | ringtone`
 * `voice | voicecall | voicecommunication`

**pcmOptions.type:** *same as `usage`*

**pcmOptions.streamType:** *same as `usage`*

**pcmOptions.sampleRate:** `(any integer)`  
**pcmOptions.sampleRateInHz:** *same as `sampleRate`*

**pcmOptions.channels:** `(any integer)`

**pcmOptions.bufferSize:** `(any integer)`  
**pcmOptions.bufferSizeInBytes:** *same as `bufferSize`*

**pcmOptions.mode:**
 * `streaming | stream | forever` - Streaming Buffer
 * `playOnce | static | oneTime | once ` Static Buffer

**pcmOptions.onMarkerReached:** `(any truthy value)` - should emit callback on marker reached

**pcmOptions.onPeriodicNotification:** `(any truthy value)` - should emit notification callback periodically

**Callbacks:**
 * `onMarkerReached` - emitted when set marker is reached (not implemented)
 * `onPeriodicNotification` - emitted periodically based on notification period
 * `onSessionId` - emitted after track has been built

### Set Position Notification Period
`NativeModules.PcmAudio.setPositionNotificationPeriod(sessionId, periodInFrames)`

### Write Samples
`NativeModules.PcmAudio.write(sessionId, base64Data);`

### Play Audio
`NativeModules.PcmAudio.play(sessionId);`

### Stop Audio
`NativeModules.PcmAudio.stop(sessionId);`

### Release Session
`NativeModules.PcmAudio.release(sessionId);`

*Note: Sessions are automatically released when the React Native host is destroyed to avoid memory leaks.*

## Examples:

*Create PCM session with streaming mode and store SessionID*
```
this.pcmAudioSessionId = null;

startAudioStream = (pcmOptions) => {
  return new Promise((resolve, reject) => {
    var callback = (event, data) => {
      switch(event) {
        case 'onSessionId':
          this.pcmAudioSessionId = data;
          /* start playing audio immediately */
          resolve(data);
          break;
      }
    };
    NativeModules.PcmAudio.build(pcmOptions, callback);
  });
};

writeAudioStream = (samples) => {
  return new Promise((resolve, reject) => {
    var my16BitAudioSource = new Int16Array(mySampleLength);
    /* write pcm samples here */
    var convertedTo8BitByteArray = new Uint8Array(my16BitAudioSource.buffer);
    var base64Data = base64.encode(String.fromCharCode.apply(null, convertedTo8BitByteArray));
    NativeModules.PcmAudio.write(this.pcmAudioSessionId, byteData);
    resolve();
  });
};

stopAudioStream = () => {
  return new Promise((resolve, reject) => {
    if (!this.pcmAudioSessionId) {
      console.log('no audio stream');
      return resolve();
    }
    NativeModules.PcmAudio.release(this.pcmAudioSessionId);
    this.pcmAudioSessionId = null;
    resolve();
  });
};
```

Stream audio:
```
var pcmOptions = {
  encoding: '16bit',
  usage: 'alarm',
  sampleRate: 8000,
  channels: 1,
  mode: 'streaming'
};
startAudioStream(pcmOptions)
  .then(NativeModules.PcmAudio.play);
  .then(startStreamingLoop)

/* Set up streaming loop */
function onAudioStreamSamples(samples) {
  writeAudioStream(samples);
}
function onAudioStreamDone() {
  /* Release when done */
  stopAudioStream();
}
```

Buffer and play on-demand:
```
var pcmOptions = {
  encoding: '16bit',
  usage: 'alarm',
  sampleRate: 8000,
  channels: 1,
  mode: 'static'
};
startAudioStream(pcmOptions)
  .then(writeAllSamples)
  .then(playOnce)

function writeAllSamples() {
  writeAudioStream(sample1);
  writeAudioStream(sample2);
  writeAudioStream(sample3);
}

function playOnce() {
  NativeModules.PcmAudio.play(this.pcmAudioSessionId);
}

function onAudioRequested() {
  playOnce();
}
```
