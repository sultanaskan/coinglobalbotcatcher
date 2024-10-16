package com.askan.coinglobalbot
/*

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.processNextEventInCurrentThread
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var  virtualDisplay : VirtualDisplay? =null


    private val binder = LocalBinder()
    inner class LocalBinder(): Binder() {
        fun  getService(): ScreenCaptureService= this@ScreenCaptureService
    }
    override fun onBind(intent: Intent?): IBinder? {return binder }


    companion object {
        const val CHANNEL_ID = "ScreenCaptureChannel"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        println("Capture class created")
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("On start command called")
        println("Intent: $intent flags: $flags startId: $startId")
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData: Intent? = intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        println("resultCode:  $resultCode resultData: $resultData")

        if (resultCode == -1 && resultData != null) {
            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            println("Media projection created")
          //  startScreenCapture()
        }

        Toast.makeText(this, "Screen capture service started", Toast.LENGTH_SHORT).show()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        println("Starting foreground service started")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Running")
            .setContentText("Capturing screen...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    public fun startScreenCapture(callback: (Text) -> Unit) {
     //   println("Start screen capture running")
        // Properly initialize display metrics
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        // Ensure dimensions are valid
        if (screenWidth > 0 && screenHeight > 0) {
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
            virtualDisplay =  mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                0,
                imageReader?.surface,
                null,
                null
            )
         //   println("Virtual Display created")
            imageReader?.setOnImageAvailableListener({ reader ->
            //    println("Image available")
                val image = reader.acquireLatestImage()
                if (image != null) {
                //    println("Image acquired, WIDTH: ${image.width} HEIGHT: ${image.height} SIZE: ${image.planes}")
                    val bitmap: Bitmap = imageToBitmap(image)
                    recognizeTextFromBitmap(bitmap) { recognizedText ->
                        callback(recognizedText)
                    }
                    image.close()
                    stopScreenCapture()
                    stopSelf()  // Stop the service after capturing one image
                } else {
                }
            }, null)
        } else {
            println("Error: Screen width or height is invalid.")
        }
    }


    private fun stopScreenCapture() {
      //  println("Stopping screen capture")
         imageReader?.setOnImageAvailableListener(null, null)  // Disable the listener
         imageReader?.close()
        virtualDisplay = null
        // mediaProjection?.stop()
         //mediaProjection?.stop()  // Stop the media projection
       // mediaProjection = null  // Clear the reference
    }

    fun imageToBitmap(image: Image): Bitmap {
        // Get image details
        val width = image.width
        val height = image.height
        val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // Create a bitmap with the image's width and height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Prepare an int array to hold the pixel data
        val pixels = IntArray(width * height)

        // Read the pixel data from the buffer
        var offset = 0
        for (row in 0 until height) {
            var pixelOffset = row * rowStride
            for (col in 0 until width) {
                val index = pixelOffset / pixelStride
                val pixelValue = buffer.getInt(index * 4)  // Read the ARGB values from the buffer
                pixels[offset++] = pixelValue
                pixelOffset += pixelStride
            }
        }

        // Set the pixels into the bitmap
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }


    private fun recognizeTextFromBitmap(bitmap: Bitmap, callback: (Text) -> Unit){
     //   println("Text recognition start ")
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { text ->
            //    println("TEXT RECOUNT: ${text}")
                callback(text)
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Error recognizing text: ${e.message}")
            }
    }


    fun saveImage(bitmap: Bitmap) {
        println("Image saved: $bitmap")
       /* val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * resources.displayMetrics.widthPixels

        val bitmap = Bitmap.createBitmap(
            resources.displayMetrics.widthPixels + rowPadding / pixelStride,
            resources.displayMetrics.heightPixels,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        */
       // val bitmap : Bitmap= imageToBitmap(image)

        // Save the bitmap to the public Downloads directory
        val fileName =  "screenshot${System.currentTimeMillis()}.png"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloadsDir, "Screens/")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        println("Bitmap: $bitmap")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        println("Image saved to: ${file.absolutePath}")
    }





    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
       // mediaProjection?.stop()
      //  mediaProjection = null
    }
}


 */