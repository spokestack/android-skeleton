package io.spokestack.skeleton

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.spokestack.spokestack.OnSpeechEventListener
import io.spokestack.spokestack.SpeechContext
import io.spokestack.spokestack.SpeechPipeline
import io.spokestack.spokestack.nlu.TraceListener
import io.spokestack.spokestack.nlu.tensorflow.TensorflowNLU
import io.spokestack.spokestack.tts.SynthesisRequest
import io.spokestack.spokestack.tts.TTSEvent
import io.spokestack.spokestack.tts.TTSListener
import io.spokestack.spokestack.tts.TTSManager
import io.spokestack.spokestack.util.EventTracer
import java.io.File
import java.io.FileOutputStream

// for wakeword model caching (see below)
private const val PREF_NAME = "AppPrefs"
private const val versionKey = "versionCode"
private const val nonexistent = -1

class MainActivity : AppCompatActivity(), OnSpeechEventListener, TTSListener, TraceListener {
    private val logTag = javaClass.simpleName

    // a sentinel value we'll use to verify we have the proper permissions to use Spokestack to
    // record audio
    private val audioPermission = 1337

    // Spokestack's main subsystems; you might not want them directly in your activity, but
    // you will need to provide your application `Context` for certain features
    private var pipeline: SpeechPipeline? = null
    private var nlu: TensorflowNLU? = null
    private var tts: TTSManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Spokestack setup
        if (this.pipeline == null && checkMicPermission()) {
            buildPipeline()
        }
        // you'll need NLU models to use this component; see the README for more information on
        // obtaining them
        buildNLU()
        buildTTS()
    }

    private fun checkMicPermission(): Boolean {
        // On API levels >= 23, users can revoke permissions at any time, and API levels >= 26
        // require the RECORD_AUDIO permission to be requested at runtime, so we'll need
        // to verify it on launch
        val recordPerm = Manifest.permission.RECORD_AUDIO
        val granted = PackageManager.PERMISSION_GRANTED
        if (ContextCompat.checkSelfPermission(this, recordPerm) == granted) {
            return true
        }
        ActivityCompat.requestPermissions(this, arrayOf(recordPerm), audioPermission)
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = PackageManager.PERMISSION_GRANTED
        // respond to the permission request's asynchronous result
        when (requestCode) {
            audioPermission -> {
                if (grantResults.isNotEmpty() && grantResults[0] == granted) {
                    buildPipeline()
                } else {
                    Log.w(logTag, "Record permission not granted; voice control disabled!")
                }
                return
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun buildPipeline() {
        // We're going to use TensorFlow Lite models for wakeword detection to demonstrate how
        // you'd set up the speech pipeline to expect them. Compressing them in your asset and
        // decompressing them to the cache folder if they're absent is one way to keep app size
        // down while making the models available at runtime. Another way would be to download the
        // models from a CDN and cache them in a similar fashion.
        // You don't need to worry about this step if you're not using TFLite for wakeword
        // detection (see the io.spokestack.spokestack.profile package for descriptions of
        // the various pre-configured profiles available).
        // See the README for links to the original files.
        checkForModels()

        // Note that support for on-device ASR (which we're using here via an ".*AndroidASR"
        // profile) is device-dependent. You may wish to call
        // `SpeechRecognizer.isRecognitionAvailable(Context)` and use a different ASR if it is
        // unavailable (or use Android-provided ASR only for demos).
        pipeline = SpeechPipeline.Builder()
            .useProfile("io.spokestack.spokestack.profile.TFWakewordAndroidASR")
            .setProperty("wake-detect-path", "$cacheDir/detect.lite")
            .setProperty("wake-encode-path", "$cacheDir/encode.lite")
            .setProperty("wake-filter-path", "$cacheDir/filter.lite")
            .setAndroidContext(applicationContext)
            .addOnSpeechEventListener(this)
            .build()

        pipeline?.start()
    }

    private fun checkForModels() {
        if (!modelsCached()) {
            decompressModels()
        } else {
            val currentVersionCode = BuildConfig.VERSION_CODE
            val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val savedVersionCode = prefs.getInt(versionKey, nonexistent)

            if (currentVersionCode != savedVersionCode) {
                decompressModels()

                // Update the shared preferences with the current version code
                prefs.edit().putInt(versionKey, currentVersionCode).apply()
            }
        }
    }

    private fun modelsCached(): Boolean {
        // We'll use the presence of one of the wakeword models as a proxy for everything being
        // decompressed since we do it all in the same step. You may wish to be more thorough.
        val filterName = "filter.lite"
        val filterFile = File("$cacheDir/$filterName")
        return filterFile.exists()
    }

    private fun decompressModels() {
        listOf(
            "detect.lite",
            "encode.lite",
            "filter.lite",
            "nlu.tflite",
            "metadata.json",
            "vocab.txt"
        ).forEach(::cacheAsset)
    }

    private fun cacheAsset(fileName: String) {
        val cachedFile = File("$cacheDir/$fileName")
        val inputStream = assets.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val fos = FileOutputStream(cachedFile)
        fos.write(buffer)
        fos.close()
    }

    private fun buildNLU() {
        if (this.nlu == null) {
            this.nlu = TensorflowNLU.Builder()
                .setProperty("nlu-model-path", "$cacheDir/nlu.tflite")
                .setProperty("nlu-metadata-path", "$cacheDir/metadata.json")
                .setProperty("wordpiece-vocab-path", "$cacheDir/vocab.txt")
                .setProperty("trace-level", EventTracer.Level.DEBUG.value())
                .addTraceListener(this)
                .build()
        }
    }

    private fun buildTTS() {
        if (this.tts == null) {
            this.tts = TTSManager.Builder()
                .setTTSServiceClass("io.spokestack.spokestack.tts.SpokestackTTSService")
                // automatic playback; omit to manage playback of synthesized audio via a
                // TTSListener
                .setOutputClass("io.spokestack.spokestack.tts.SpokestackTTSOutput")
                // demo Spokestack credentials; not guaranteed to work indefinitely
                // see https://spokestack.io/create to get your own
                .setProperty("spokestack-id", "f0bc990c-e9db-4a0c-a2b1-6a6395a3d97e")
                .setProperty(
                    "spokestack-secret",
                    "5BD5483F573D691A15CFA493C1782F451D4BD666E39A9E7B2EBE287E6A72C6B6"
                )
                .addTTSListener(this)
                // the next two lines are only necessary for the automatic playback component
                .setAndroidContext(applicationContext)
                .setLifecycle(lifecycle)
                .build()
        } else {
            // You'd do something like this if you're using automatic playback in a multi-activity
            // app. The TTS subsystem should be long-lived, but in order for it to manage resources
            // used by its internal media player, it needs to be registered to the lifecycle of
            // each new activity as it becomes active.
            // In other words, it's not necessary in this skeleton, but it's here for demonstration
            // purposes.
            this.tts!!.apply {
                prepare()
                registerLifecycle(lifecycle)
            }
        }
    }

    // OnSpeechEventListener implementation
    // It's advisable, especially in multi-activity apps, to put this in a separate class, perhaps
    // a `DialogueManager` component that handles logging and response generation
    override fun onEvent(event: SpeechContext.Event?, context: SpeechContext?) {
        when (event) {
            SpeechContext.Event.ACTIVATE -> Log.i(logTag, "Pipeline activated")
            SpeechContext.Event.DEACTIVATE -> Log.i(logTag, "Pipeline deactivated")
            SpeechContext.Event.RECOGNIZE -> context?.transcript?.let { respond(it) }
            SpeechContext.Event.TIMEOUT -> Log.i(logTag, "ASR timeout")
            SpeechContext.Event.ERROR -> context?.error.let { Log.w(logTag, "ASR Error: $it") }
            SpeechContext.Event.TRACE -> context?.message.let { Log.w(logTag, "TRACE: $it") }
        }
    }

    private fun respond(utterance: String) {
        // A (too-) simple response generator that parrots back what the user just said. Given our
        // TTS pipeline setup above, this response will be automatically played when the audio is
        // available.
        val request = SynthesisRequest.Builder("Why do you feel that $utterance?").build()
        tts?.synthesize(request)
    }

    // TTSListener implementation
    // We're letting Spokestack automatically handle TTS playback,
    // so this isn't strictly necessary,
    // but it's here to provide error feedback
    override fun eventReceived(event: TTSEvent?) {
        when (event?.type) {
            TTSEvent.Type.ERROR -> Log.w(logTag, event.error)
            // If you're managing playback yourself, this is where you'd receive the URL to your
            // synthesized audio
            TTSEvent.Type.AUDIO_AVAILABLE ->
                Log.i(logTag, "Audio received: ${event.ttsResponse.audioUri}")
            // If you want to restart ASR in anticipation of an immediate user response (for
            // example, as a response to a question from the app), you'd call pipeline?.activate()
            // here
            TTSEvent.Type.PLAYBACK_COMPLETE -> Log.i(logTag, "TTS playback complete")
        }
    }

    override fun onTrace(level: EventTracer.Level, message: String) {
        when (level) {
            EventTracer.Level.ERROR -> Log.e(logTag, message)
            EventTracer.Level.DEBUG -> Log.d(logTag, message)
            EventTracer.Level.INFO -> Log.i(logTag, message)
            EventTracer.Level.WARN -> Log.w(logTag, message)
            else -> Log.v(logTag, message)
        }
    }
}
