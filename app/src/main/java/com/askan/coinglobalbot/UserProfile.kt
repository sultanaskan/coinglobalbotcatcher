package com.askan.coinglobalbot

data class UserProfile(
    var uid: String? = null,
    var username: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var securityCode: String? = null,
    var accountUid: String? = null,
    var rule: String? = null,
    var accountAccess: String? = null,
    var password: String? = null
) {
    // No-argument constructor for Firestore
    constructor() : this(null, null, null, null, null, null, null, null, null)
}
