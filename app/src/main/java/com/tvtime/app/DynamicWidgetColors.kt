package com.tvtime.app

import android.content.Context
import android.graphics.Color
import android.os.Build

object DynamicWidgetColors {

    data class Palette(
        val accent: Int,
        val background: Int,
        val border: Int,
        val text: Int,
        val subtext: Int
    )

    fun get(context: Context): Palette? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null
        }

        val resources = context.resources
        val accentId = resources.getIdentifier(
            "system_accent1_600",
            "color",
            "android"
        )
        val backgroundId = resources.getIdentifier(
            "system_neutral1_900",
            "color",
            "android"
        )
        val borderId = resources.getIdentifier(
            "system_neutral2_200",
            "color",
            "android"
        )

        if (accentId == 0 || backgroundId == 0 || borderId == 0) {
            return null
        }

        return Palette(
            accent = resources.getColor(accentId, context.theme),
            background = resources.getColor(backgroundId, context.theme),
            border = resources.getColor(borderId, context.theme),
            text = Color.WHITE,
            subtext = 0xFFB8C0CC.toInt()
        )
    }
}
