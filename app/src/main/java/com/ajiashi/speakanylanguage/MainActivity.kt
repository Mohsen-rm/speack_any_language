package com.ajiashi.speakanylanguage

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

interface JsonResponseCallback {
    fun onSuccess(jsonResponse: String)
    fun onFailure(errorMessage: String)
}

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val RECORD_AUDIO_PERMISSION_CODE = 1
    private lateinit var speechRecognizer: SpeechRecognizer
    lateinit var btn_start : Button
    lateinit var btn_stop : Button
    lateinit var edit_ar : EditText
    lateinit var edit_en : EditText
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_start = findViewById(R.id.btn_start)
        btn_stop = findViewById(R.id.btn_stop)
        edit_ar = findViewById(R.id.editText_ar)
        edit_en = findViewById(R.id.editText_en)

        checkPermission()

        textToSpeech = TextToSpeech(this, this)

        textToSpeech.setPitch(1.2f) // Adjust the value as needed

        // Set the speech rate (1.0 is the default, adjust as needed)
        textToSpeech.setSpeechRate(0.8f) // Adjust the value as needed

        if (textToSpeech.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE) {
            // Get the available voices
            val voices = textToSpeech.voices

            // Find a voice with desired language (e.g., English)
            val desiredVoice = voices.find { voice ->
                voice.locale == Locale.FRENCH
            }

            // Set the desired voice
            textToSpeech.voice = desiredVoice

            // Speak a sample text
            textToSpeech.speak("Hello, how are you?", TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TextToSpeech", "Language not available or TextToSpeech initialization failed.")
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-AR")

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0]
                    edit_ar.setText(text)

                    // Make the HTTP request and handle the JSON response
                    makeGetRequest(text, object : JsonResponseCallback {
                        override fun onSuccess(jsonResponse: String) {
                            // Process the JSON response
                            Log.d("OkHttp", "Response: $jsonResponse")

                            try {
                                // Parse the JSON response
                                val jsonObject = JSONObject(jsonResponse)

                                // Access the value of the 'translatedText' field
                                val translatedText = jsonObject.getString("translatedText")

                                // Process or use the 'translatedText' as needed
                                Log.d("OkHttp", "Translated Text: $translatedText")

                                // Optionally, update UI or perform other actions with the translated text here
                                runOnUiThread {
                                    speakText(translatedText)
                                    edit_en.setText(translatedText)
                                }

                            } catch (e: JSONException) {
                                // Handle JSON parsing error
                                Log.e("OkHttp", "Error parsing JSON", e)
                            }
                        }

                        override fun onFailure(errorMessage: String) {
                            // Handle failure (e.g., network issues, server errors)
                            Log.e("OkHttp", "Failed to make GET request: $errorMessage")
                        }
                    })
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        btn_start.setOnClickListener {
            speechRecognizer.startListening(recognizerIntent)
        }

        btn_stop.setOnClickListener {
            speechRecognizer.stopListening()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(java.util.Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "Language not supported")
            }
        } else {
            Log.e("TextToSpeech", "Initialization failed")
        }
    }

    private fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun makeGetRequest(text: String, callback: JsonResponseCallback) {
        // Replace the URL with the actual URL of your API
        val url = "http://192.168.1.228:5000/tr?text=$text"

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Invoke the failure callback
                callback.onFailure(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle the response
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    // Invoke the success callback with the JSON response
                    responseBody?.let { callback.onSuccess(it) }
                } else {
                    // Invoke the failure callback with the error message
                    callback.onFailure("Unsuccessful response: ${response.code}")
                }
            }
        })
    }
    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
        speechRecognizer.destroy()
    }
}