# Stream Metadata Guidelines for Watcharr

To ensure that the **Stream Stats** overlay in Watcharr (on both Android TV and Mobile/Tablet) displays complete and accurate statistics, your IPTV streams and manifests (HLS or DASH) must publish correct metadata.

Below is the documentation of manifest tags and elementary stream properties that upstream services (such as Dispatcharr, delta-bridge, or transcoding servers) should output.

---

## 1. HLS (HTTP Live Streaming / M3U8) Specifications

For HLS streams, ExoPlayer extracts track-level metadata from the master playlist and variant attributes.

### Master Playlist (`#EXT-X-STREAM-INF`) Tag
Each variant stream in your master playlist should define its parameters correctly using the attributes below:

```m3u8
#EXT-X-STREAM-INF:BANDWIDTH=4500000,AVERAGE-BANDWIDTH=4000000,RESOLUTION=1920x1080,FRAME-RATE=60.000,CODECS="avc1.64002a,mp4a.40.2",AUDIO="audio-group"
https://example.com/stream_1080p/index.m3u8
```

#### Key Attributes:
*   **`RESOLUTION`** (e.g., `RESOLUTION=1920x1080`):
    *   *Watcharr Stat:* Initial Resolution.
    *   *Detail:* Set this to the true pixel width and height of the video. (Watcharr dynamically overrides this with the actual decoded video dimensions if the stream differs, but this tag is essential for initial rendering).
*   **`FRAME-RATE`** (e.g., `FRAME-RATE=60.000` or `FRAME-RATE=29.970`):
    *   *Watcharr Stat:* Frame Rate (fps).
    *   *Detail:* Must be specified as a floating-point or integer value. Without this, ExoPlayer cannot show the frame rate until multiple frames are decoded.
*   **`CODECS`** (e.g., `CODECS="avc1.64002a,mp4a.40.2"` or `CODECS="hvc1.1.6.L120.90,ec-3"`):
    *   *Watcharr Stat:* Video & Audio Codec.
    *   *Detail:* Specify both the video and audio codecs using RFC 6381 profiles.
        *   `avc1.xxxxxx` represents H.264 / AVC.
        *   `hvc1.xxxxxx` represents H.265 / HEVC.
        *   `mp4a.40.2` represents AAC-LC.
        *   `ec-3` represents Enhanced AC-3 (Dolby Digital Plus).
*   **`BANDWIDTH` & `AVERAGE-BANDWIDTH`** (e.g., `BANDWIDTH=4500000` in bits/second):
    *   *Watcharr Stat:* Video Bitrate.
    *   *Detail:* Represents the bit rate of the stream. ExoPlayer divides this by `1,000,000` to show the value in Mbps (e.g., `4.00 Mbps`).

### Audio Stream Playlist (`#EXT-X-MEDIA`) Tag
If you use demuxed audio streams, declare them properly using `#EXT-X-MEDIA` linked to the stream:

```m3u8
#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio-group",NAME="English",DEFAULT=YES,AUTOSELECT=YES,LANGUAGE="en",URI="audio.m3u8",CHANNELS="6"
```

#### Key Attributes:
*   **`CHANNELS`** (e.g., `CHANNELS="2"` or `CHANNELS="6"`):
    *   *Watcharr Stat:* Audio Channels.
    *   *Detail:* Set to `"2"` for Stereo, or `"6"` for 5.1 surround sound. Without this tag, the player must wait for audio stream buffer decoding to detect the channels.

---

## 2. DASH (Dynamic Adaptive Streaming over HTTP / MPD) Specifications

For DASH streams, ExoPlayer maps XML representation attributes to the format parameters.

### XML Representation Attributes
Inside the `.mpd` manifest file, ensure the `<Representation>` tag for each adaptation set specifies the properties:

```xml
<AdaptationSet mimeType="video/mp4">
  <Representation id="1" codecs="avc1.64002a" width="1920" height="1080" frameRate="60" bandwidth="4000000" />
</AdaptationSet>

<AdaptationSet mimeType="audio/mp4">
  <Representation id="2" codecs="mp4a.40.2" audioSamplingRate="48000" bandwidth="192000">
    <AudioChannelConfiguration schemeIdUri="urn:mpeg:dash:23003:3:audio_channel_configuration:2011" value="6" />
  </Representation>
</AdaptationSet>
```

#### Key Attributes:
*   **`width` & `height`**: Maps to Resolution.
*   **`framerate`**: Maps to Frame Rate (fps).
*   **`codecs`**: Maps to Codec names.
*   **`bandwidth`**: Maps to Video/Audio Bitrate.
*   **`audioSamplingRate`**: Maps to Audio Sample Rate (e.g. `48000` maps to `48.0 kHz`).
*   **`AudioChannelConfiguration` (with `value="6"`)**: Maps to Audio Channels (e.g., `5.1`).

---

## 3. Media Stream Payload / Elementary Streams (TS or MP4 container)

When manifest files do not contain rich metadata attributes (such as in simple single-file direct streams or basic IPTV streams), ExoPlayer parses metadata directly from the media headers once the buffer loads.

### Video Elementary Stream (SPS/PPS Headers)
*   **Resolution and Aspect Ratio:** The SPS (Sequence Parameter Set) header inside the video stream (H.264/H.265) must contain the correct frame cropping parameters and width/height.
*   **Frame Rate:** The frame timing descriptors or VUI (Video Usability Information) inside the elementary stream must specify the correct framerate.

### Audio Elementary Stream (ADTS / AudioSpecificConfig)
*   **Sample Rate & Channels:** For AAC, the ADTS frame header or the `AudioSpecificConfig` block (in MP4) must specify the correct sample frequency index (e.g., `4` for `44100 Hz`, `3` for `48000 Hz`) and the correct channel configuration (e.g., `2` for stereo, `6` for 5.1).
