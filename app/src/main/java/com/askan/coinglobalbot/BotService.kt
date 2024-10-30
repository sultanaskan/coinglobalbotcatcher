package com.askan.coinglobalbot

import android.os.Bundle

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.Toast
import com.askan.coinglobalbot.BotService.constants.ACCOUNT_UID
import com.askan.coinglobalbot.BotService.constants.MINI_ORDER_PRICE
import com.askan.coinglobalbot.BotService.constants.SECURITY_CODE
import java.lang.Thread.sleep

class BotService : AccessibilityService() {
    private val appPackageName = "com.coinglobal.bdt"
    private var itemSelected = false
    private lateinit var securityCode:String
    private var miniOrderPrice: Int = 100
    private  var accountUid:String? = null
    private var isServiceAccess = false


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
      //  Toast.makeText(this, "Service connected", Toast.LENGTH_SHORT).show()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            securityCode = intent.getStringExtra(SECURITY_CODE).toString()
            miniOrderPrice = Integer.parseInt(intent.getStringExtra(MINI_ORDER_PRICE).toString())
            accountUid = intent.getStringExtra(ACCOUNT_UID).toString()
            if(accountUid.equals("stop")) {
                println("Bot Stop Successfully with: Security Code: $securityCode \nMini Order Price: $miniOrderPrice \nAccountUid: $accountUid")
            }else {
                println("Security Code: $securityCode \nMini Order Price: $miniOrderPrice \nAccountUid: $accountUid")
                openTargetApp()
            }
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
                if(!isServiceAccess) {
                    if(accountUid != null  ){
                        checkServiceAccess()
                    }
                    return
                }
                val source= event.source ?: return
                performv1(source, miniOrderPrice, securityCode)

        //  findNodeWithCheckPageAndTk(miniOrderPrice)

                /*
            if ((fnwtk(source, miniOrderPrice))?.performAction(AccessibilityNodeInfo.ACTION_CLICK)!!) {
                if (fillInputAndConfirm(securityCode!!)) {
                    println("Order processed successfully!")
                } else {
                    println("Failed to process the order.")
                }
            }*/
    }


    //version 2.0
    private  fun performv2(buyPage: AccessibilityNodeInfo){
        if(itemSelected){
            // sleep(100)
                var i = 0
                sleep(100)
                while (i < 40) {
                    if (fillInputAndConfirm(buyPage, securityCode) ) {
                        itemSelected = false
                        return
                    }
                    if(i > 38){
                        itemSelected = false
                        return
                    }
                    i++
                }
        }else{
            if (!(findNodeWithTk(buyPage, miniOrderPrice))) {
                itemSelected = false
            }
        }
    }
    private fun findNodeWithTk(root: AccessibilityNodeInfo, miniOrderPrice: Int):Boolean {
        try {
            for (j in 2 until root.childCount) {
                    val listItem = root.getChild(j)
                    if ((listItem.getChild(1).text.removeSuffix("TK").toString()
                            .toDouble()) > miniOrderPrice
                    ) {
                        if (listItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            itemSelected = true
                            return true
                        }
                    }
            }

        }catch (_:Exception){}
        return  false
    }

    private fun findNodeWithCheckPageAndTk( miniOrderPrice: Int):Boolean {
       val root = rootInActiveWindow ?: return false
        try {
            val buy = root.getChild(0).getChild(0).getChild(0)
            val text = buy.text
             if(text.contains("pages/buy/index")) {
                for (j in 2 until buy.childCount) {
                    val listItem = buy.getChild(j)
                    if ((listItem.getChild(1).text.removeSuffix("TK").toString()
                            .toDouble()) > miniOrderPrice
                    ) {
                        if (listItem.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            if (fillInputAndConfirm()) {
                                println("Success")
                            }
                            return true
                        }
                    }
                }
            }else if(text.contains("pages/index/index") ){
                isServiceAccess = false
                findTabAndClick("buy")
                Toast.makeText(this, "Wait for a moment! \n" +
                        " We have to check your authentication again! \n" +
                        "Please don't change buy page while I am working on it. \n ",Toast.LENGTH_SHORT).show()
            }else{
                 findTabAndClick("buy")
                 }

        }catch (_:Exception){}
        return  false
    }




    private  fun fillInputAndConfirm(pageNodes: AccessibilityNodeInfo, securityCode: String):Boolean{
          //  println("edit text node serarching  ${pageNodes.parent}")
        val start = System.currentTimeMillis()
                for (j in 2 until pageNodes.childCount) {
                    try {
                    val stripsDialog = pageNodes.getChild(j)
                    //println("($j edit text  dialog:$stripsDialog")
                    if (stripsDialog.viewIdResourceName == "buystips") {
                      //println("Buystrips found $stripsDialog")
                        val stripsBox = stripsDialog.getChild(1).getChild(0)
                       // println("Strips box $stripsBox")
                        val editText = stripsBox.getChild(12).getChild(1)
                     //   println("Edit text $editText")
                        if (editText != null) {
                            if(editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                                if (editText.performAction(
                                        AccessibilityNodeInfo.ACTION_SET_TEXT,
                                        createTextInput(securityCode)
                                    )
                                ) {
                                    // println("text inputed")
                                    sleep(10)
                                    if (stripsBox.getChild(13)
                                            .performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    ) {
                                        println("Confirm in : ${System.currentTimeMillis() - start}")
                                        itemSelected = false
                                        //   println("Success")
                                        return true
                                    }
                                }
                            }
                        }
                    } }catch (_:Exception){}
                }
        return false
    }



    //version 1.0
    private fun performv1(root: AccessibilityNodeInfo, miniOrderPrice: Int, input:String){
       if(findNodeWithTk(root, miniOrderPrice)){
           if(fillInputAndConfirm()){
               println("Success")
           }
       }
    }
    private fun fnwtk(nodeInfo: AccessibilityNodeInfo, miniOrderPrice: Int): AccessibilityNodeInfo? {
        try{
            if ( nodeInfo.text.toString().contains("TK")) {
                try {
                    if ((nodeInfo.text.removeSuffix("TK").toString().toDouble()) > miniOrderPrice) {
                        nodeInfo.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return nodeInfo.parent
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
    private fun fillInputAndConfirm(): Boolean {
            val root = rootInActiveWindow ?: return false
            try {
                val result = findNodeByClassName(root, "android.widget.EditText")
                val edit = result["editText"]
                val buy = result["buyOrder"]
               // val close = result["close"]
               // println("edit: $edit \nbuy:$buy \nclose: $close ")
                if (edit != null && buy != null ) {
                    if (edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, createTextInput(securityCode))) {
                        if( buy.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
                            return true
                        }
                    }
                  //  close.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }else{

                 //   println("Input field not found, retrying...")
                  //  attempts++
                  //  sleep(10)  // Small delay before retry
                }
            } catch (_: Exception) {
                println("Exception occer")

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
          //  result["close"] = root.parent.getChild(2)
            result["buyOrder"] = root
        }

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
    private fun findBuyPage(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val start = System.currentTimeMillis()
        val text = root.text?.toString() // Convert to string if it's a CharSequence
        if (text != null && text.trim() == "pages/buy/index[3]") {
           return root
       }
        for(j in 0 until root.childCount){
            val chiled = root.getChild(j)
            if(chiled != null) {
                val buyPage = findBuyPage(chiled)
                if(buyPage != null){
                    println("Page found in : ${System.currentTimeMillis() - start}")
                    return buyPage
                }
            }
        }
       return null
    }





    //Check Service access of user
    private fun  checkServiceAccess(){
        val root = rootInActiveWindow ?: return
        println("Check Service Access ")
        if(findTabAndClick("my")){
            sleep(30)
            println("My Tab Clicked Successfuly")
            val uid = findUid("UID", root)
            println("UID FROM SCREEN: $uid")
            if(uid != null){
                if(uid == accountUid){
                    isServiceAccess = true
                    findTabAndClick("buy")
                }else{
                    Toast.makeText(this, "BOT SERVICE NOT REGISTERED FOR THIS ACCOUNT!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun findUid(text: String, nodeInfo: AccessibilityNodeInfo): String? {
        if(nodeInfo.text != null && nodeInfo.text.toString().contains("UID")){
            if(nodeInfo.parent == null) return null
            sleep(20)
           return (nodeInfo.parent?.getChild(2)?.text?.toString()!!)
        };

        for(j in 0 until nodeInfo.childCount){
            val child = nodeInfo.getChild(j)
            if(child != null){
                val uid = findUid(text, child)
                if(uid != null){
                    return uid
                }
            }
        }
        return null
    }

    private fun findTabAndClick(text:String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node =findNodeByTextFromEnd(text, root )
        if(node != null){
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        return true
    }

    // Updated version to allow recursive search through children
    private fun findNodeByTextFromEnd(text: String, node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if this node contains the required text
        println("Searching my")
        if (node.text != null && node.text.toString() == text) {
            return node
        }
        println("Searching my 2")
        // Traverse children from last to first
        for (j in node.childCount - 1 downTo 0) {
            val child = node.getChild(j)
            if (child != null) {
                // Recursively search within this child node
                val foundNode = findNodeByTextFromEnd(text, child)
                if (foundNode != null) {
                    println("my found")
                    return foundNode
                }
            }
        }
        return null
    }



    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        isServiceAccess = false
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_LONG).show()
    }












    object constants {
        const val EXTRA_SECURITY_CODE = "com.askan.coinglobalbot.extra.SECURITY_CODE"
        const val SECURITY_CODE  = "com.askan.coinglobalbot.extra.SECURITY_CODE"
        const val MINI_ORDER_PRICE = "com.askan.coinglobalbot.extra.MINI_ORDER_PRICE"
        const val ACCOUNT_UID = "com.askan.coinglobalbot.extra.ACCOUN_UID"
    }


}