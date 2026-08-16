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
        val webUrl: String,
        val storeUrl: String = "https://play.google.com/store/apps/details?id=$packageName"
    )

    val ALL = listOf(
        Service("Tubi", "com.tubitv", "https://tubitv.com", "https://play.google.com/store/apps/details?id=com.tubitv"),
        Service(
            "Netflix",
            "com.netflix.mediaclient",
            "https://netflix.com",
            "https://play.google.com/store/apps/details?id=com.netflix.mediaclient"
        ),
        Service(
            "Prime Video",
            "com.amazon.avod.thirdparty.client",
            "https://primevideo.com",
            "https://play.google.com/store/apps/details?id=com.amazon.avod.thirdparty.client"
        ),
        Service(
            "Disney+",
            "com.disney.disneyplus",
            "https://disneyplus.com",
            "https://play.google.com/store/apps/details?id=com.disney.disneyplus"
        ),
        Service(
            "Hulu",
            "com.hulu.plus",
            "https://hulu.com",
            "https://play.google.com/store/apps/details?id=com.hulu.plus"
        ),
        Service(
            "YouTube",
            "com.google.android.youtube",
            "https://youtube.com",
            "https://play.google.com/store/apps/details?id=com.google.android.youtube"
        )
    )

    fun byPackage(packageName: String): Service? = ALL.firstOrNull { it.packageName == packageName }

    fun default(): Service = ALL[0]

    /**
     * Builds an Intent that launches [service]'s app when installed, otherwise opens its website
     * (so the button still does something useful) and, failing that, its store page.
     */
    fun createLaunchIntent(context: Context, service: Service): Intent {
        val launch = context.packageManager.getLaunchIntentForPackage(service.packageName)
        return if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launch
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(service.webUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
