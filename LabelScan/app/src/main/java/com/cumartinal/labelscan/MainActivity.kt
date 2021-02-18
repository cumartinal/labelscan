package com.cumartinal.labelscan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.renderscript.ScriptGroup
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.collections.HashMap

typealias LumaListener = (luma: Double) -> Unit
const val EXTRA_MESSAGE = "com.cumartinal.LabelScan.TEXT"

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        // Create alert
        MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Analysing label")
                .setMessage("Please wait...")
                .setCancelable(false)
                .show()
        // Show progress indicator
        image_analysis_progress_indicator.show()

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Code that is used to save the image, not needed until future features
        /*
        // Create time-stamped output file to hold the image
        val photoFile = File(
                outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
         */

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        analyze(image)
                        super.onCaptureSuccess(image)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }
        })
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                    }

            imageCapture = ImageCapture.Builder()
                    .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Method that takes the media.Image from takePhoto() and runs it through
    // MLKit to extract the text
    private fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image != null) {
            val image = InputImage.fromMediaImage(image,
                    imageProxy.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API
            val recognizer = TextRecognition.getClient()
            val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Task completed successfully
                        Log.d("TAG", visionText.text)
                        // Parse the text and send it to a new activity
                        parseRecognizedText(visionText)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        Log.e(TAG, "MLKit text recognition failed", e)
                    }
        }
    }

    private fun parseRecognizedText(visionText : Text) {
        val recognizedText = visionText.text

        // Create map that will hold all the nutritional information
        // Possible values include: kcal, totfat, satfat, trafat
        // cholesterol, sodium, totcarbs, fiber, sugars, protein
        val nutritionMap = hashMapOf<String, Int>()

        // Testing code to see how information is separated in the vision model
        for (block in visionText.textBlocks) {
            for (line in block.lines) {

                if (line.text.contains("Total Fat")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                           nutritionMap["totfat"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Saturated Fat")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["satfat"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Trans Fat")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["trafat"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Cholesterol")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["cholesterol"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Sodium")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["sodium"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Total Carbohydrate")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["totcarbs"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Fiber")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["fiber"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Total Sugars")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["sugars"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

                if (line.text.contains("Protein")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() })
                            nutritionMap["protein"] = (element.text.filter { it.isDigit() }).toInt()
                    }
                }

            }
        }

        // Log.d(TAG, "TESTTESTTEST. Saturated fat = " + nutritionMap["satfat"].toString() + "g")
        nutritionMap.forEach { (key, value) -> Log.d(TAG, "$key : $value") }
        sendText(recognizedText, nutritionMap)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Not needed for now, possibly needed in the future if user's want
    // to store the photo for future features
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Go to DisplayTextActivity to show recognized text
    private fun sendText(message : String, nutritionMap: HashMap<String, Int>) {
        val intent = Intent(this, DisplayTextActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}