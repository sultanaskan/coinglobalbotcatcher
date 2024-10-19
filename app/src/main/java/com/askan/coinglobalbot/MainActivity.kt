package com.askan.coinglobalbot


import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import java.lang.Thread.sleep
import android.widget.Toast


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize

class MainActivity : AppCompatActivity() {
    private lateinit var securityCode: EditText
    private lateinit var miniOrderPrice: EditText
    private lateinit var startBotButton: Button
    private lateinit var takeScreenShotButton: Button
    private lateinit var imageView: ImageView
    private lateinit var stopButton: Button
    private  lateinit var auth: FirebaseAuth
    private  var isLogin = false

  //  private var screenCaptureService: ScreenCaptureService? = null

    // ServiceConnection to manage the connection to the service
/*

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ScreenCaptureService.LocalBinder
            screenCaptureService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to ScreenCaptureService
        Intent(this, ScreenCaptureService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Firebase.initialize(this)
        auth = FirebaseAuth.getInstance()
       // createUser("admin@gmail.com", "111111")
         // Ensure this is declared outside the loop


        if(!isLogin) {
            login("admin@gmail.com", "111111")  // Attempt to login
             sleep(5000)
        }





        // Initialize UI components

            securityCode = findViewById(R.id.securityCode)
            miniOrderPrice = findViewById(R.id.targetText)
            startBotButton = findViewById(R.id.startBotButton)
            takeScreenShotButton = findViewById(R.id.btCaptureScreenShot)
            imageView = findViewById(R.id.imageView)
            stopButton = findViewById(R.id.StopBotButton)
            // Initialize MediaProjectionManager
            //  mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            //startScreenCapture()

            startBotButton.setOnClickListener {
                if(isLogin) {
                    if (isAccessibilityServiceEnabled()) {
                        startBotService()
                    } else {
                        openAccessibilitySettings()
                    }
                }else{
                    Toast.makeText(this, "Something went wrong! \nplease close this app and try again. Or Contract with developer: \nEmail: sultanahedaskan@gmail.com", Toast.LENGTH_SHORT).show()
                }
            }

            stopButton.setOnClickListener {
                openAccessibilitySettings()
                stopBotService()
            }

/*
        takeScreenShotButton.setOnClickListener {
            if (isBound) {
                GlobalScope.launch(Dispatchers.IO){
                    withContext(Dispatchers.Main){
                        screenCaptureService?.startScreenCapture{
                                text ->
                            if (text != null) {
                                println("Recognized Text is: ${text.text}")
                            } else {
                                println("No text found")
                            }
                        }
                    }
                }

            } else {
                Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }
 */

    }

    private fun login(email: String, password: String): Boolean {
        var f = false
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) {
            task ->
            if (task.isSuccessful) {
                // User login success
                val logEmail = auth.currentUser?.email
                if(logEmail == email){
                    isLogin = true
                    f= true
                }
                Toast.makeText(this, "Welcome  ", Toast.LENGTH_LONG).show()
            }
        }
        return f
    }

    private fun createUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User registration success
                    val user = auth.currentUser
                    Toast.makeText(this, "User registered successfully!\nWelcome $user", Toast.LENGTH_SHORT).show()

                    // Optionally, you can navigate to another activity or handle user session
                } else {
                    // If registration fails, display a message to the user.
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun stopBotService() {
        stopService(Intent(this, BotService::class.java))
        Toast.makeText(this,"Coin global bot stop Successfully!",Toast.LENGTH_SHORT).show()
    }

    private fun startBotService() {
        val intent = Intent(this, BotService::class.java)
        val sec = securityCode.text.toString()
        val mini = miniOrderPrice.text.toString()
        if (sec.isNotEmpty() && (mini.isNotEmpty())) {
            Toast.makeText(this,"Starting bot with  \nSecurity Code: $sec \nMinimum Price: $mini", Toast.LENGTH_SHORT).show()
            intent.putExtra(BotService.constants.SECURITY_CODE, sec)
            intent.putExtra(BotService.constants.MINI_ORDER_PRICE, mini)
            startService(intent)
        }else{
            Toast.makeText(this, "Please fill the security code and minimum price filed", Toast.LENGTH_SHORT).show()
        }


    }

/*
    // Start screen capture using MediaProjection
    private fun startScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE)
    }

    // Handle the result of screen capture intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
            // Start the screen capture service as a foreground service
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent) // Android O+ requires foreground service
            }else{
            startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    */
    // Check if the accessibility service is enabled
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in services) {
            if (info.id == "$packageName/.BotService") {
                return true
            }
        }
        return false
    }

    // Open accessibility settings to enable the service
    private fun openAccessibilitySettings() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility Service Not Enabled")
            .setMessage("This app requires accessibility services to function correctly. Please enable the service in the settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun getBitmapRootView(v: View): Bitmap {
        val rootView = v.rootView
        rootView.isDrawingCacheEnabled = true
        return rootView.drawingCache
    }




}

