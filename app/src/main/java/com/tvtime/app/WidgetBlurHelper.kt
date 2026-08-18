package com.example.widget

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

object WidgetBlurHelper {
    /**
     * Applies real-time hardware blur to views on Android 12+ (API 31+)
     */
    fun applyGlassBlur(view: View, blurRadius: Float = 30f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                Shader.TileMode.CLAMP
            )
            view.setRenderEffect(blurEffect)
        }
    }
}
