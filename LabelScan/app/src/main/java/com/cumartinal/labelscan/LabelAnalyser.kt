package com.cumartinal.labelscan

import android.Manifest
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage

import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition

//data class Result(val isAchieved: Boolean, val nutritionInfo: HashMap<String, Int>)
data class Result (val isAchieved: Boolean, val recognizedText: String, val analysisResult: String)
// var nutritionInfo = hashMapOf<String, Int>()


class LabelAnalyser {

    var recognizedText: String = ""
    var analysisResult: String = ""

    // Main function, will return a boolean stating if the analysis has been
    // successful or not, as well as a Map with all the content of the
    // nutrition label extracted and neatly organised
    //TODO
    fun analyse(imageProxy : ImageProxy) : Result {
        var isAchieved = false
        val image = imageProxy.image
        val result: Task<Text>

        if (image != null) {
            val image = InputImage.fromMediaImage(image,
                imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient()
            result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    isAchieved = true
                    recognizedText = visionText.text
                    analysisResult="Successful"
                    Log.d(TAG, recognizedText)
                    // can't return function here, is there a way to do so in the if image != null?
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
                .addOnFailureListener { exc ->
                    // Task failed with an exception
                    isAchieved = false
                    Log.e(LabelAnalyser.TAG, "MLKit text recognition failed", exc)
                }

        } else {

            isAchieved = false
            analysisResult = "No image to analyse (image was null)"
            return Result (isAchieved, recognizedText, analysisResult)
        }

        // Return Result dataClass defined in MainActivity.kt
        Log.d(LabelAnalyser.TAG, "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD. Here's the recognized text: " + recognizedText)
        return Result (isAchieved, recognizedText, analysisResult)
        //return Result(false, nutritionInfo)
    }


    companion object {
        private const val TAG = "LabelAnalyser"
    }

}