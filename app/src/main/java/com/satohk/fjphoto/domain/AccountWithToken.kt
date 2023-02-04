package com.satohk.fjphoto.domain

enum class ServiceProvider(val url: String){
    GOOGLE("com.google"),
}

data class AccountWithToken(
    val serviceProviderUrl: String,
    val userName: String,
    val accessToken: String){

    val accountId: String get() = "${userName}@${serviceProviderUrl}"
}