package com.cumartinal.labelscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit
const val EXTRA_MESSAGE = "com.cumartinal.LabelScan.TEXT"

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var analysisProgressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        val mediaPlayerCamera = MediaPlayer.create(this, R.raw.ui_camera_shutter)
        // Set up the listener for take photo button, make it play sound
        camera_capture_button.setOnClickListener {
            mediaPlayerCamera.start()
            takePhoto()
        }

        // Set up bottom navigation and its listener
        bottom_navigation_main.selectedItemId = R.id.placeholder

        bottom_navigation_main.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.favoritesItem -> {
                    val contextView = findViewById<View>(R.id.bottom_navigation_main)
                    Snackbar.make(
                        contextView,
                        "This feature is not yet implemented! Please wait for future updates",
                        Snackbar.LENGTH_LONG
                    )
                        .setAnchorView(camera_capture_button)
                        .show()
                    val mediaPlayerNavigationFav = MediaPlayer.create(this, R.raw.ui_tap_03)
                    mediaPlayerNavigationFav.start()
                    false
                }
                R.id.settingsItem -> {
                    val mediaPlayerNavigationSet = MediaPlayer.create(this, R.raw.ui_tap_01)
                    mediaPlayerNavigationSet.start()
                    openSettings()
                    true
                }
                else -> false
            }
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        analysisProgressDialog = MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Analysing label")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create()
    }

    private fun takePhoto() {
        // Create alert
        analysisProgressDialog.show()

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
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                val cameraControl = camera.cameraControl

                // Code from https://proandroiddev.com/android-camerax-tap-to-focus-pinch-to-zoom-zoom-slider-eb88f3aa6fc6
                // Listen to tap events on the viewfinder and set them as focus regions
                viewFinder.setOnTouchListener(View.OnTouchListener setOnTouchListener@{ view: View, motionEvent: MotionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                        MotionEvent.ACTION_UP -> {
                            // Get the MeteringPointFactory from PreviewView
                            val factory : MeteringPointFactory  = SurfaceOrientedMeteringPointFactory(
                                viewFinder.width.toFloat(), viewFinder.height.toFloat())

                            // Create a MeteringPoint from the tap coordinates
                            val point = factory.createPoint(motionEvent.x, motionEvent.y)

                            // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                            val action = FocusMeteringAction.Builder(point).build()

                            // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                            // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                            cameraControl.startFocusAndMetering(action)

                            return@setOnTouchListener true
                        }
                        else -> return@setOnTouchListener false
                    }
                })

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Method that takes the media.Image from takePhoto() and runs it through
    // MLKit to extract the text
    private fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image != null) {
            val image = InputImage.fromMediaImage(
                image,
                imageProxy.imageInfo.rotationDegrees
            )
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

    private fun parseRecognizedText(visionText: Text) {
        val recognizedText = visionText.text

        // Create array that will hold all the nutritional information
        // Possible values include: kcal, totFat, satFat, traFat
        // cholesterol, sodium, totCarbs, fiber, sugars, protein
        // vitD, calcium, iron, potassium
        val nutritionArray = IntArray(14) { i -> 0}
        var hasNutritionalInformation = false

        for ((index, block) in visionText.textBlocks.withIndex()) {
            for (line in block.lines) {

                // The kcal amount is recognized in the following/previous block from
                // from the "Calories" block, so text recognition here is different
                // The previous and following blocks are checked to see if they have the value
                if (line.text.contains("Calories")) {
                    if (visionText.textBlocks[index + 1].text.any {it.isDigit()}) {
                        nutritionArray[0] = (visionText.textBlocks[index + 1].text.filter { it.isDigit() }).toInt()
                        hasNutritionalInformation = true
                    } else if (visionText.textBlocks[index - 1].text.any {it.isDigit()}) {
                        nutritionArray[0] = (visionText.textBlocks[index - 1].text.filter { it.isDigit() }).toInt()
                        hasNutritionalInformation = true
                    }
                }

                if (line.text.contains("Total Fat")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[1] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Saturated Fat")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[2] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Trans Fat")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[3] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Cholesterol")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[4] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Sodium")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[5] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Total Carbohydrate")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[6] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Fiber")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[7] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Total Sugars")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[8] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Protein")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[9] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Vitamin D")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[10] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Calcium")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[11]  = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Iron")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[12] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Potassium")) {
                    for (element in line.elements) {
                        if (element.text.any { it.isDigit() }) {
                            nutritionArray[13] = (element.text.filter { it.isDigit() }).toInt()
                            hasNutritionalInformation = true
                        }
                    }
                }
            }
        }

        // Check if we have extracted nutritional information or not, and act accordingly
        if (hasNutritionalInformation) {
            image_analysis_progress_indicator.hide()
            analysisProgressDialog.cancel()
            sendText(recognizedText, nutritionArray)
        } else {
            image_analysis_progress_indicator.hide()
            val mediaPlayerError = MediaPlayer.create(this, R.raw.alert_error)
            mediaPlayerError.start()
            MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Analysis failed!")
                    .setMessage(
                        "There was no nutritional information in the image." +
                                "\nPlease try again"
                    )
                    .setPositiveButton("OK") { dialog, which ->
                        val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.navigation_hover_tap)
                        mediaPlayerNavigationScan.start()
                    }
                    .show()
            // Done last to avoid weird moment with no dialog
            analysisProgressDialog.cancel()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
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
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    // Go to DisplayTextActivity to show recognized text
    private fun sendText(message: String, nutritionArray: IntArray) {
        val intent = Intent(this, DisplayTextActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
            putExtra("intArray", nutritionArray)
        }
        startActivity(intent)
    }

    // Go to settings
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
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