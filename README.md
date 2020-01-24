# Spokestack Skeleton

Hi, and welcome to the Spokestack skeleton "app" (in the loosest sense of the term). This is the companion code for our Android [getting started guide](https://spokestack.io/docs/Android/getting-started), created so you don't have to read its code snippets in isolation.

You should be able to clone this project, move a few things around if you have multiple activities, and have a simple voice-controlled app up and running in short order. That's exactly how we made the [Spokestack Control Room](https://github.com/spokestack/android-control-room) app, just to test our own claim.

Due to the nature of this app, it might not be updated as frequently as the Spokestack library itself, so always check dependency versions to ensure you have the latest and greatest for your own app.

We've included a few details in code comments in this project, but check out the [Spokestack documentation]() for a lot more information.

---
## Android NDK setup

Because Spokestack relies on native libraries for some audio processing tasks to optimize performance, you'll need the Android NDK to build this project. You can install it by opening the SDK manager in Android Studio, selecting the SDK Tools tab, and setting the `ndkVersion` property in the `android` block of  your app-level `build.gradle` file. The NDK can also be downloaded [directly from Google](https://developer.android.com/ndk/downloads). The property has been set in this project to the current version at the time of writing, so it may need to be updated.

---
## Wakeword models

To use the demo "Spokestack" wakeword, download the following TensorFlow Lite models and place them in `src/main/assets`. The code in `MainActivity` expects these files to exist, so you'll have to change it if you don't want to use them.
- [detect](https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack/detect.lite)
- [encode](https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack/encode.lite)
- [filter](https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack/filter.lite)
