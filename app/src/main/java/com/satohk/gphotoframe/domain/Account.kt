package com.satohk.gphotoframe.domain

enum class ServiceProvider(val url: String){
    GOOGLE("com.google"),
}

data class Account(
    val serviceProvider: ServiceProvider,
    val userName: String,
    val accessToken: String
)