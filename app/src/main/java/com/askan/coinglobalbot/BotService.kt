package com.askan.coinglobalbot

import android.os.Bundle

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.askan.coinglobalbot.BotService.constants.MINI_ORDER_PRICE
import com.askan.coinglobalbot.BotService.constants.SECURITY_CODE
import java.lang.Thread.sleep

class BotService : AccessibilityService() {
    private val appPackageName = "com.coinglobal.bdt"

    private var isBound = false
    private var securityCode: String? = null
    private var miniOrderPrice: Int = 100
/*
    private var screenCaptureService: ScreenCaptureService? = null// ServiceConnection to manage the connection to the service
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

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "Service created", Toast.LENGTH_SHORT).show()
        Intent(this, ScreenCaptureService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
*/
    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Service connected", Toast.LENGTH_SHORT).show()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            securityCode = intent.getStringExtra(SECURITY_CODE)
            miniOrderPrice = Integer.parseInt(intent.getStringExtra(MINI_ORDER_PRICE).toString())
            println("Security Code: $securityCode Mini Order Price: $miniOrderPrice")
            openTargetApp()
        }
        return START_STICKY
    }
    private fun openTargetApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(appPackageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
            Toast.makeText(this, "Target App Opened", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Package Not Found", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val source = event.source ?: return
            if ((fnwtk(source, miniOrderPrice))?.performAction(AccessibilityNodeInfo.ACTION_CLICK)!!) {
                if (fillInputAndConfirm(securityCode!!)) {
                    println("Order processed successfully!")
                } else {
                    println("Failed to process the order.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fnwtk(nodeInfo: AccessibilityNodeInfo, miniOrderPrice: Int): AccessibilityNodeInfo? {
        try{
            if ( nodeInfo.text.toString().contains("TK")) {
                try {
                    if ((nodeInfo.text.removeSuffix("TK").toString().toDouble()) > miniOrderPrice) {
                        nodeInfo.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return nodeInfo.parent
                       /* var parent = nodeInfo.parent
                        while (parent != null) {
                            if (parent.isClickable) {
                                println("This node is clickable $parent")
                                count = 0
                                return parent
                            }
                            parent = parent.parent
                            println("${count--}This node parent of target price")
                        } */
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }catch (_: Exception){
        }




        // Recursively search the children
        for (j in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(j)
            if (child != null) {
                val result = fnwtk(child, miniOrderPrice)
                if (result != null) {
                    return result
                }
            }
        }
        // If no clickable parent was found, return null
        return null
    }
    private fun fillInputAndConfirm(input: String): Boolean {
        var attempts = 0
        while (attempts < 3) {
            try {
                val result = findNodeByClassName(rootInActiveWindow, "android.widget.EditText")
                val edit = result["editText"]
                val buy = result["buyOrder"]

                if (edit != null && buy != null) {
                    if (edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS) &&
                        edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, createTextInput(input))) {
                        buy.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        sleep(10)
                        return true
                    }
                }

                if (edit == null) {
                    println("Input field not found, retrying...")
                    attempts++
                    sleep(20)  // Small delay before retry
                } else {
                    break
                }
            } catch (_: Exception) {
                sleep(100)
                return false
            }
        }
        return false
    }
    private fun createTextInput(input: String): Bundle {
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input)
        return args
    }
    private var i = 0
    private fun findNodeByClassName(root: AccessibilityNodeInfo?, className: String): Map<String, AccessibilityNodeInfo> {
        if (root == null) return emptyMap()
        i++
        val result = mutableMapOf<String, AccessibilityNodeInfo>()
        if (root.className == className) {
            result["editText"] = root
        }
        if (root.text?.toString() == "Confirm buy" && root.isClickable && root.className == "android.widget.TextView") {
            result["buyOrder"] = root
        }
      //  root.findAccessibilityNodeInfosByViewId(1)
/*
        // If both nodes are found, return early
        if (result.size == 2) {
            println("Its Nodes is: $i ")
            i = 0
            return result}
*/
        for (i in 0 until root.childCount) {
            val childResult = findNodeByClassName( root.getChild(i), className)
            result.putAll(childResult)
            if (result.size == 2){
                return result}
        }
        return result
    }


    override fun onInterrupt() {
        // Handle service interruption
    }


    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopSelf()
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }












    object constants {
        const val EXTRA_SECURITY_CODE = "com.askan.coinglobalbot.extra.SECURITY_CODE"
        const val SECURITY_CODE  = "com.askan.coinglobalbot.extra.SECURITY_CODE"
        const val MINI_ORDER_PRICE = "com.askan.coinglobalbot.extra.MINI_ORDER_PRICE"
    }
}