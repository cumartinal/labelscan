package com.cumartinal.labelscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_display_text.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.bottom_navigation_main
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val EXTRA_MESSAGE = "com.cumartinal.LabelScan.TEXT"

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var analysisProgressDialog: AlertDialog

    private var backPressedTimeStamp = 0.toLong()

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Send uri as image to be analysed
        analyze(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme depending on saved preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        when (sharedPreferences.getString("theming", "")) {
            "Light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_LabelScan)
            }
            "Dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                setTheme(R.style.Theme_LabelScan)
            }
            "Pale" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                setTheme(R.style.Theme_LabelScan_Pale)
            }
            "System" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                setTheme(R.style.Theme_LabelScan)
            }
            else -> {
                Log.d(TAG, "ERROR LOADING THEMING, PREFERENCE VALUE DOES NOT EXIST")
            }
        }
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button, make it play sound, make it freeze preview
        camera_capture_button.setOnClickListener {
            if (sharedPreferences.getBoolean("earcons", true)) {
                val mediaPlayerCamera = MediaPlayer.create(this, R.raw.ui_camera_shutter)
                mediaPlayerCamera.start()
            }
            takePhoto()
            val frozenBitmap = viewFinder.bitmap
            frozen_view.setImageBitmap(frozenBitmap)
            frozen_view.visibility = View.VISIBLE
            viewFinder.visibility = View.INVISIBLE

        }

        // Set up listener for choose photo from gallery button
        gallery_button.setOnClickListener {
            getContent.launch("image/*")
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
                    if (sharedPreferences.getBoolean("earcons", true)) {
                        val mediaPlayerNavigationFav = MediaPlayer.create(this, R.raw.ui_tap_03)
                        mediaPlayerNavigationFav.start()
                    }
                    false
                }
                R.id.settingsItem -> {
                    if (sharedPreferences.getBoolean("earcons", true)) {
                        val mediaPlayerNavigationSet = MediaPlayer.create(this, R.raw.ui_tap_01)
                        mediaPlayerNavigationSet.start()
                    }
                    openSettings()
                    false
                }
                else -> false
            }
        }

        // outputDirectory for saving photos
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Dialog that appears when analysing a label
        analysisProgressDialog = MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Analysing label")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create()

        // Handle shared images to the app from an external application
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        // Update UI to reflect image being shared
                        analyze(it)
                    }
                }
            }
            else -> {
                // Handle other intents, such as being started from the home screen
            }
        }

    }

    override fun onResume() {
        super.onResume()

        // Avoid viewFinder being frozen after coming back from DisplayTextActivity
        frozen_view.visibility = View.INVISIBLE
        viewFinder.visibility = View.VISIBLE

    }

    private fun takePhoto() {
        // Create alert
        analysisProgressDialog.show()

        // Show progress indicator
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        if (!sharedPreferences.getBoolean("reducedMotion", false))
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

        // Set up image capture callback, which is triggered after photo has
        // been taken
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // Send to analysis
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

            // Preview of camera
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

                // Modified Code from https://proandroiddev.com/android-camerax-tap-to-focus-pinch-to-zoom-zoom-slider-eb88f3aa6fc6
                // Listen to tap events on the viewfinder and set them as focus regions
                viewFinder.setOnTouchListener(View.OnTouchListener setOnTouchListener@{ view: View, motionEvent: MotionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                        MotionEvent.ACTION_UP -> {
                            // Get the MeteringPointFactory from PreviewView
                            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
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
            // Obtain image object
            val image = InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
            )

            // Pass image to an ML Kit Vision API
            val recognizer = TextRecognition.getClient()
            recognizer.process(image)
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

    // Method that passes a photo URI to MLKit to extract the text
    // Called instead of analyze(ImageProxy) when the user presses the gallery_button
    // Inner lines are the same as analyze(ImageProxy)
    private fun analyze(uri: Uri?) {
        // Create alert as it's not tied to camera button
        analysisProgressDialog.show()

        // Show progress indicator
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        if (!sharedPreferences.getBoolean("reducedMotion", false))
            image_analysis_progress_indicator.show()

        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, uri)

            // Pass image to an ML Kit Vision API
            val recognizer = TextRecognition.getClient()
            recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Task completed successfully
                        Log.d("TAG", visionText.text)
                        // Parse the text and send it to a new activity
                        parseRecognizedText(visionText)
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        Log.e(TAG, "MLKit text recognition failed", e)
                    }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun parseRecognizedText(visionText: Text) {
        val recognizedText = visionText.text

        // Create array that will hold all the nutritional information
        // Possible values include: kcal, totFat, satFat, traFat
        // cholesterol, sodium, totCarbs, fiber, totSugars, addSugars protein
        // vitD, calcium, iron, potassium
        val nutritionArray = FloatArray(15) { i -> 0.0f}
        val isNutritionElementChanged = BooleanArray(15) { i -> false}
        var hasNutritionalInformation = false

        // Parses through lines checking if they have text for a nutrient.
        // If so, only takes the FIRST numerical value
        // to avoid reading percentages or text like "2,000 calories".
        // It also checks that the number is not too big and that we have not accidentally annexed
        // another, separate value (like percentages).
        for ((index, block) in visionText.textBlocks.withIndex()) {
            for (line in block.lines) {

                // The kcal amount is recognized in the following/previous block from
                // from the "Calories" block, so text recognition here is different
                // The previous and following blocks are checked to see if they have the value
                if (line.text.contains("Calories")) {
                    var isCaloriesInSameLine = false
                    for (element in line.elements) {
                        if (nutritionArray[0] == 0.0f && element.text.any { it.isDigit() }) {
                            nutritionArray[0] = (element.text.filter { it.isDigit() }).take(3).toFloat()
                            hasNutritionalInformation = true
                            isCaloriesInSameLine = true
                        }
                    }
                    // Avoid ArrayIndexOutOfBoundsException
                    if (!isCaloriesInSameLine && index != 0 && index < visionText.textBlocks.size) {
                        if (nutritionArray[0] == 0.0f && visionText.textBlocks[index - 1].text.any {it.isDigit()}) {
                            nutritionArray[0] = (visionText.textBlocks[index - 1].text.filter { it.isDigit() }).take(3).toFloat()
                            hasNutritionalInformation = true
                        } else if (nutritionArray[0] == 0.0f && visionText.textBlocks[index + 1].text.any {it.isDigit()}) {
                            nutritionArray[0] = (visionText.textBlocks[index + 1].text.filter { it.isDigit() }).take(3).toFloat()
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Total Fat", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Total", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[1] && element.text.any { it.isDigit() }) {
                            nutritionArray[1] = extractValue(2, element.text, 1)
                            isNutritionElementChanged[1] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Saturated Fat", true)
                        || line.text.contains("Sat", true) && line.text.contains("Fat", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Sat", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[2] && element.text.any { it.isDigit() }) {
                            nutritionArray[2] = extractValue(2, element.text, 2)
                            isNutritionElementChanged[2] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Trans Fat", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Trans", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[3] && element.text.any { it.isDigit() }) {
                            nutritionArray[3] = extractValue(2, element.text, 3)
                            isNutritionElementChanged[3] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Cholest", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Cholest", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[4] && element.text.any { it.isDigit() }) {
                            nutritionArray[4] = extractValue(3, element.text, 4)
                            isNutritionElementChanged[4] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Sodium", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Sodium", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[5] && element.text.any { it.isDigit() }) {
                            nutritionArray[5] = extractValue(4, element.text, 5)
                            isNutritionElementChanged[5] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Carb", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Carb", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[6] && element.text.any { it.isDigit() }) {
                            nutritionArray[6] = extractValue(3, element.text, 6)
                            isNutritionElementChanged[6] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Fiber", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Fiber", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[7] && element.text.any { it.isDigit() }) {
                            nutritionArray[7] = extractValue(2, element.text, 7)
                            isNutritionElementChanged[7] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                // Recognises Total Sugars thanks to nutritionArray[8] == 0, taking the first line
                // With 'sugars' and a related value
                if (line.text.contains("Sugars", true) && !line.text.contains("Added", true)
                        || line.text.contains("Total Sugars", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Sugars", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[8] && element.text.any { it.isDigit() }) {
                            nutritionArray[8] = extractValue(2, element.text, 8)
                            isNutritionElementChanged[8] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Added Sugars", true)) {
                    // Added sugars does not use isAfterNutrientName because FDA standards explain
                    // That this element is written as "Includes Xg Added Sugars"
                    // So it finds "Added" and checks the word before
                    var i = -1
                    var hasSeenNutrientName = false
                    for (element in line.elements) {
                        i++
                        if (element.text.contains("Added", true)) {
                            hasSeenNutrientName = true
                            if (line.elements[i-1].text.any { it.isDigit() }) {
                                nutritionArray[9] = extractValue(2, line.elements[i-1].text, 9)
                                isNutritionElementChanged[9] = true
                            }
                        }
                        if (!isNutritionElementChanged[9] && hasSeenNutrientName && element.text.any { it.isDigit() }) {
                            nutritionArray[9] = extractValue(2, element.text, 9)
                            isNutritionElementChanged[9] = true
                            if (isNutritionElementChanged[8] && nutritionArray[9] > nutritionArray[8]) {
                                nutritionArray[9] = nutritionArray[8]
                            } else if (!isNutritionElementChanged[8]) {
                                nutritionArray[8] = nutritionArray[9]
                            }
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Protein", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Protein", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[10] && element.text.any { it.isDigit() }) {
                            nutritionArray[10] = extractValue(2, element.text, 10)
                            isNutritionElementChanged[10] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Vitamin D", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("D", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[11] && element.text.any { it.isDigit() }) {
                            nutritionArray[11] = extractValue(2, element.text, 11)
                            isNutritionElementChanged[11] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Calcium", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Calcium", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[12] && element.text.any { it.isDigit() }) {
                            nutritionArray[12]  = extractValue(4, element.text, 12)
                            isNutritionElementChanged[12] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                if (line.text.contains("Iron", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Iron", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[13] && element.text.any { it.isDigit() }) {
                            nutritionArray[13] = extractValue(2, element.text, 13)
                            isNutritionElementChanged[13] = true
                            hasNutritionalInformation = true
                        }
                    }
                }

                // Potassium gets cut to Potas. in shortened labels
                if (line.text.contains("Potas", true)) {
                    var isAfterNutrientName = false
                    for (element in line.elements) {
                        if (element.text.contains("Potas", true))
                            isAfterNutrientName = true
                        if (isAfterNutrientName && !isNutritionElementChanged[14] && element.text.any { it.isDigit() }) {
                            nutritionArray[14] = extractValue(4, element.text, 14)
                            isNutritionElementChanged[14] = true
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
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
            if (sharedPreferences.getBoolean("earcons", true)) {
                val mediaPlayerError = MediaPlayer.create(this, R.raw.alert_error)
                mediaPlayerError.start()
            }
            MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Analysis failed!")
                    .setMessage(
                            "There was no nutritional information in the image." +
                                    "\nPlease try again."
                    )
                    .setPositiveButton("OK") { dialog, which ->
                        if (sharedPreferences.getBoolean("earcons", true)) {
                            val mediaPlayerNavigationScan = MediaPlayer.create(this, R.raw.navigation_hover_tap)
                            mediaPlayerNavigationScan.start()
                        }
                        viewFinder.visibility = View.VISIBLE
                        frozen_view.visibility = View.INVISIBLE
                    }
                    .setOnCancelListener {
                        viewFinder.visibility = View.VISIBLE
                        frozen_view.visibility = View.INVISIBLE
                    }
                    .show()
            // Done last to avoid weird moment with no dialog
            analysisProgressDialog.cancel()
        }
    }

    // Extracts value from an element.text
    // Also ensures that the number read is of correct size
    // And that a "g" has not been read as a 9
    private fun extractValue(digitsToTake: Int, element: String, correspondingArray: Int): Float {
        var numberToTake = ""
        var isGPresent = false
        var hasDecimals = false

        // Handle if we're somehow reading the percentage value
        // Instead of the raw value
        if ("%" in element) {
            val nutrientDVs = arrayOf(2000f, 78f, 20f, 1f, 300f, 2300f, 275f, 28f, 1f, 50f, 50f, 20f,
                    1300f, 18f, 4700f)
            val percentageDV = element.filter { it.isDigit() }.toInt()
            return if (percentageDV == 0)
                0.0f
            else
                nutrientDVs[correspondingArray]*(percentageDV/100)
        }

        for (char in element) {
            if (char == 'g')
                isGPresent = true
            else if (char.isDigit() && !isGPresent)
                numberToTake += char
            else if (char == '.' && !isGPresent) {
                numberToTake += char
                hasDecimals = true
            }
        }
        // If "g" is not present, it means it has wrongly been interpreted as a 9
        // So we need to drop it so the value is right
        if (!isGPresent) {
            if (numberToTake.length != 1)
                numberToTake = numberToTake.dropLast(1)
        }
        return when {
            hasDecimals -> numberToTake.toFloat()
            // Don't return an empty string
            numberToTake.length == 1 -> numberToTake.toFloat()
            else -> numberToTake.take(digitsToTake).toFloat()
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
    private fun sendText(message: String, nutritionArray: FloatArray) {
        val intent = Intent(this, DisplayTextActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
            putExtra("floatArray", nutritionArray)
        }
        startActivity(intent)

    }

    // Go to settings
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
        }
        startActivity(intent)
    }

    // Double tap back to exit override
    // Enables doubletapping to exit if it's enabled in settings
    override fun onBackPressed() {
        Log.d("CDA", "onBackPressed Called")
        // Have we enabled double tap?
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
        if (sharedPreferences.getBoolean("doubleTapExit", false)) {
            // Have we pressed the back button recently twice?
            if (backPressedTimeStamp + 2500 > System.currentTimeMillis()) {
                super.onBackPressed()
                return
            } else {
                val contextView = findViewById<View>(R.id.bottom_navigation_main)
                Snackbar.make(contextView, "Double tap back to exit", Snackbar.LENGTH_LONG)
                        .setAnchorView(camera_capture_button)
                        .show()
            }
            backPressedTimeStamp = System.currentTimeMillis()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}