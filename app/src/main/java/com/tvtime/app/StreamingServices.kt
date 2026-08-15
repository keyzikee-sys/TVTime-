package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Supported streaming services the widget can launch. The widget opens the app when installed,
 * otherwise falls back to its Play Store page.
 */
object StreamingServices {

    data class Service(
        val name: String,
        val packageName: String,
        val fallbackUrl: String
    )

    val ALL = listOf(
        Service("Tubi", "com.tubitv", "https://play.google.com/store/apps/details?id=com.tubitv"),
        Service(
            "Netflix",
            "com.netflix.mediaclient",
            "https://play.google.com/store/apps/details?id=com.netflix.mediaclient"
        ),
        Service(
            "Prime Video",
            "com.amazon.avod.thirdparty.client",
            "https://play.google.com/store/apps/details?id=com.amazon.avod.thirdparty.client"
        ),
        Service(
            "Disney+",
            "com.disney.disneyplus",
            "https://play.google.com/store/apps/details?id=com.disney.disneyplus"
        ),
        Service(
            "Hulu",
            "com.hulu.plus",
            "https://play.google.com/store/apps/details?id=com.hulu.plus"
        ),
        Service(
            "YouTube",
            "com.google.android.youtube",
            "https://play.google.com/store/apps/details?id=com.google.android.youtube"
        )
    )

    fun byPackage(packageName: String): Service? = ALL.firstOrNull { it.packageName == packageName }

    fun default(): Service = ALL[0]

    /** Builds an Intent that launches [service], falling back to its store page if not installed. */
    fun createLaunchIntent(context: Context, service: Service): Intent {
        var launch = context.packageManager.getLaunchIntentForPackage(service.packageName)
        if (launch == null) {
            launch = Intent(Intent.ACTION_VIEW, Uri.parse(service.fallbackUrl))
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch
    }
}
