# Spokestack Skeleton

Hi, and welcome to the Spokestack skeleton "app" (in the loosest sense of the term). This is the companion code for our Android [getting started guide](https://spokestack.io/docs/Android/getting-started), created so you don't have to read its code snippets in isolation.

There's no UI included in this "app", but you should be able to clone this project, move a few things around if you have multiple activities, and have a simple voice-controlled app up and running in short order. That's exactly how we made the [Spokestack Control Room](https://github.com/spokestack/android-control-room) app, just to test our own claim.

Due to the nature of this app, it might not be updated as frequently as the Spokestack library itself, so always check dependency versions to ensure you have the latest and greatest for your own app.

We've included a few details in code comments in this project, but check out the [Spokestack documentation](https://spokestack.io/docs) for a lot more information.

---
## Wakeword models

To use the demo "Spokestack" wakeword, download the following TensorFlow Lite models and place them in `src/main/assets`. The code in `MainActivity` expects these files to exist, so you'll have to change it if you don't want to use them.
- [detect.tflite](https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack/detect.tflite)
- [encode.tflite](https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack/encode.tflite)
- [filter.tflite](https://d3dmqd7cy685il.cloudfront.net/model/wake/spokestack/filter.tflite)

---
## NLU models

To obtain NLU models, you'll need a free [Spokestack account](https://spokestack.io/create). Once you have one, you can create your own model by [exporting an Alexa or Dialogflow skill](https://spokestack.io/docs/Concepts/export), or you can download one of the shared models in the "Language Understanding" section of your account.
