package com.askan.coinglobalbot

interface ManageUsersInt {
    public fun registerUser(user: UserProfile): Boolean;
    public fun loginUser(email: String, password: String): Boolean;
    public fun grantPermission(email: String): Boolean;
    public fun revokePermission(email: String): Boolean;

}