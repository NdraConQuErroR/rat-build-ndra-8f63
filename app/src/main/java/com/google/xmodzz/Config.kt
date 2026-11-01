package com.google.xmodzz

object Config {
    const val UrlServer = "szmarket.id"
    const val Port = 2882
    val BaseUrl: String get() = "http://${UrlServer}:${Port}"
}
