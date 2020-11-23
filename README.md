Choco Music
=====================

Choco Music is a fork of Vanilla Music, primarily for personal use, so some bugs might be present.

Vanilla Music player is a [GPLv3](LICENSE) licensed MP3/OGG/FLAC/PCM player for Android with the following features:
* multiple playlist support
* grouping by artist, album or genre
* plain filesystem browsing
* [ReplayGain](https://en.wikipedia.org/wiki/ReplayGain) support
* headset/Bluetooth controls
* accelerometer/shake control
* cover art support
* [Simple Last.fm Scrobbler](https://github.com/tgwizard/sls) support

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/app/ch.blinkenlights.android.vanilla)
	 
Choco Music add these features:
* The ability to rate your music locally from 1 to 5 stars.
* The ability to tag songs and albums as being instrumentals or songs with vocals.
* A filtering feature on playback to only play songs that are either instrumentals or vocal songs.
* A Smart Shuffle feature that will shuffle a playlist and slightly prioritize highly rated songs over lower ones, to mainly listen to your favorite songs, while occasionally spicing it up with some less common songs.

Plugins
===========

Vanilla Music also includes support for plugins, this is a list of some existing plugins:
* [Cover fetcher](https://play.google.com/store/apps/details?id=com.kanedias.vanilla.coverfetch)
* [Lyrics search](https://play.google.com/store/apps/details?id=com.kanedias.vanilla.lyrics)
* [Tag editor](https://play.google.com/store/apps/details?id=com.kanedias.vanilla.audiotag)
* [Headphone detector](https://play.google.com/store/apps/details?id=ch.blinkenlights.android.vanillaplug)


Building
========
To build you will need:

 * A Java compiler compatible with Java 1.8
 * The Android SDK with platform 26 installed

Building from command-line
--------------------------
> Note: at the time of this writing, the current version of Gradle ([4.5.1](https://gradle.org/releases/)) is not compatible with the current version of JDK ([9.0.4](http://www.oracle.com/technetwork/java/javase/downloads/jdk9-downloads-3848520.html)). To have the build succeed, use JDK version [1.8.0_162](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
 * `gradle build` to build the APK
 * Optional: `gradle installDebug` to install the APK to a connected device

Building with Android Studio
---------------------
You can also build with Android Studio by importing this project into it.

Documentation
=============
Javadocs can be generated using `gradle javadoc` or `ant doc`


  [1]: https://www.transifex.com/projects/p/vanilla-music-1/
  [2]: https://github.com/vanilla-music/vanilla/issues
  [3]: https://github.com/vanilla-music/vanilla/labels/patches-welcome
