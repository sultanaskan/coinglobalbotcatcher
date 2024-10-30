package com.askan.coinglobalbot

import java.util.Date


data class UserProfile(
    var uid: String? ,
    var username: String?,
    var email: String?,
    var phone: String? ,
    var securityCode: String?,
    var accountUid: String?,
    var rule: String?,
    var accountAccess: String?,
    var password: String?,
    val logoutTime: Long? = 0,
    var accessOutTime: Long? = 0,
    var accessInTime: Long? = 0,

) {
    // No-argument constructor for Firestore
    constructor() : this(null, null, null, null, null, null, null, null, null,)
}
