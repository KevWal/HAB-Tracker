# HAB-Tracker
Android HAB Tracker App.  Originally written by Matt Brejza and modified by Kevin Walton.

If you just want the latest APK it is here:  [https://github.com/KevWal/HAB-Tracker/tree/master/habmodem/apk-release](https://github.com/KevWal/HAB-Tracker/tree/master/habmodem/apk-release)

## About 

HAB-Tracker is an android app which decodes rtty signals and displays the telemetry on an offline map for high altitude balloon tracking.

Original project page is here: [https://ukhas.org.uk/projects:hab_modem](https://ukhas.org.uk/projects:hab_modem)

## Translating

If you wish to translate into your own language, all the strings can be found in habmodem/src/main/res/values/strings.xml The translated file should then be saved under values-XX/strings.xml, where XX is the ISO country code.

## Android Studio Update

This is the first commit of a copy of Matt's code, updated to compile in Android Studio.  It was originally written in Eclipse which is now [defunct](https://android-developers.googleblog.com/2015/06/an-update-on-eclipse-android-developer.html) for Android apps.

It is my first time working with Android apps so please feel free to point out any howlers, but it does build to an APK!  The repository is not a direct fork of Matt's [original repository](https://github.com/mattbrejza/rtty_modem), as the files have all changed locations as part of the Android Studio Upgrade.

## Version History

V24 - 0.8.3 - First Android Studio built version, including the following changes:
* Increase the vertical size of the waterfall and message box components on the screen
* Add a waterfall colour gradient scale parameter to advanced options - allows the full colour gradient to be used
* Increase the speed of the waterfall by reducing the default buffer size to 1,500 (was 3,000)
* Fix the Echo button functionality on my Galaxy S10 (doesn't seem to work on some other devices)
* Fix the Strings in the Payload data on the map page

V - - Matt's unreleased latest version, included untested 100 and 150 baud functionality

V23 - 0.8.2 - Matt's last released APK
