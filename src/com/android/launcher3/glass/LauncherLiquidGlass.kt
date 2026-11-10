/*
 * Copyright (C) 2026 MistOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.glass

import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Liquid glass settings as Launcher3 sees them (Settings > Mistify > Themes > Liquid glass).
 *
 * Launcher3's shared sources build against the public SDK, so this mirrors the keys and maths of
 * the framework's com.android.internal.util.mist.glass.LiquidGlassSettings. Keep them in sync.
 * Everything lives in Settings.System, so it survives launcher restarts and reboots.
 */
object LauncherLiquidGlass {
    private const val KEY_ENABLED = "mist_glass_enabled"
    private const val KEY_STYLE = "mist_glass_style"
    private const val KEY_BLUR = "mist_glass_blur"
    private const val KEY_TINT = "mist_glass_tint"
    private const val KEY_HIGHLIGHT = "mist_glass_highlight"
    private const val KEY_LIGHT_ANGLE = "mist_glass_light_angle"
    private const val KEY_READABILITY = "mist_glass_readability"
    private const val KEY_PERFORMANCE = "mist_glass_performance"
    private const val KEY_AREA_LAUNCHER = "mist_glass_launcher"
    private val KEYS =
        arrayOf(
            KEY_ENABLED,
            KEY_STYLE,
            KEY_BLUR,
            KEY_TINT,
            KEY_HIGHLIGHT,
            KEY_LIGHT_ANGLE,
            KEY_READABILITY,
            KEY_PERFORMANCE,
            KEY_AREA_LAUNCHER,
        )

    data class Config(
        val enabled: Boolean = false,
        val style: Int = 1,
        val blur: Int = 60,
        val tint: Int = 40,
        val highlight: Int = 60,
        val lightAngle: Int = 135,
        val readability: Boolean = true,
        /**
         * Smooth mode, a user choice in Mistify: no blur regions of its own (dock), stronger
         * tint instead, so home screen swipes stay fluid on GPUs that struggle with glass.
         */
        val lite: Boolean = false,
    ) {
        fun blurRadiusPx(density: Float): Float {
            var factor = blur / 100f
            if (style == 0) factor *= 0.45f else if (style == 2) factor = min(1f, factor * 1.35f + 0.1f)
            return factor * 64f * density
        }

        fun tintAlpha(dark: Boolean): Float {
            var alpha = 0.04f + 0.70f * (tint / 100f)
            if (style == 0) alpha *= 0.55f else if (style == 2) alpha *= 1.35f
            if (readability) alpha = max(alpha, if (dark) 0.38f else 0.45f)
            alpha = min(alpha, 0.92f)
            if (lite) alpha = min(0.92f, max(alpha + 0.25f, if (dark) 0.62f else 0.72f))
            return alpha
        }

        fun tintColor(dark: Boolean): Int = tintColor(dark, tintAlpha(dark))

        fun tintColor(dark: Boolean, alpha: Float): Int {
            val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
            return (a shl 24) or (if (dark) 0x1C1C1E else 0xFFFFFF)
        }

        fun rimAlpha(): Float = 0.15f + 0.65f * (highlight / 100f)

        fun lightX(): Float = cos(Math.toRadians(lightAngle.toDouble())).toFloat()

        fun lightY(): Float = -sin(Math.toRadians(lightAngle.toDouble())).toFloat()
    }

    @Volatile private var config = Config()
    private var observer: ContentObserver? = null
    private val listeners = mutableSetOf<Runnable>()

    /** Current settings, or null when glass is off for the launcher. */
    @JvmStatic
    fun get(context: Context): Config? {
        ensureObserving(context)
        return config.takeIf { it.enabled }
    }

    /** Runs [listener] on the main thread whenever the settings change. */
    @JvmStatic
    fun addListener(context: Context, listener: Runnable) {
        ensureObserving(context)
        synchronized(listeners) { listeners += listener }
    }

    @JvmStatic
    fun removeListener(listener: Runnable) {
        synchronized(listeners) { listeners -= listener }
    }

    @JvmStatic
    fun isNightMode(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    @Synchronized
    private fun ensureObserving(context: Context) {
        if (observer != null) return
        val appContext = context.applicationContext ?: context
        val resolver = appContext.contentResolver
        config = read(appContext)
        val obs =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    config = read(appContext)
                    synchronized(listeners) { listeners.toList() }.forEach { it.run() }
                }
            }
        KEYS.forEach { resolver.registerContentObserver(Settings.System.getUriFor(it), false, obs) }
        observer = obs
    }

    private fun read(context: Context): Config {
        val cr = context.contentResolver
        fun int(key: String, def: Int) =
            try {
                Settings.System.getInt(cr, key, def)
            } catch (_: RuntimeException) {
                def
            }
        return Config(
            enabled = int(KEY_ENABLED, 0) != 0 && int(KEY_AREA_LAUNCHER, 1) != 0,
            style = int(KEY_STYLE, 1).coerceIn(0, 2),
            blur = int(KEY_BLUR, 60).coerceIn(0, 100),
            tint = int(KEY_TINT, 40).coerceIn(0, 100),
            highlight = int(KEY_HIGHLIGHT, 60).coerceIn(0, 100),
            lightAngle = int(KEY_LIGHT_ANGLE, 135),
            readability = int(KEY_READABILITY, 1) != 0,
            lite = int(KEY_PERFORMANCE, 0) == 1,
        )
    }
}

/**
 * Tint, sheen and rim of a launcher glass surface. The blurred backdrop below it is drawn by
 * BlurBackgroundHelper (a cross-window blur of the wallpaper, refracted by the compositor).
 *
 * The dock redraws every frame of a home screen swipe, so shaders are only rebuilt when the size,
 * settings or theme change; per-frame alpha goes through the paints.
 */
class LauncherGlassDrawable(private val density: Float) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val strokeRect = RectF()
    private var strokeRadius = 0f
    private var drawAlpha = 255

    // Cache key of the shaders and path.
    private var builtWidth = -1
    private var builtHeight = -1
    private var builtRadius = -1f
    private var builtConfig: LauncherLiquidGlass.Config? = null
    private var builtDark = false

    var config = LauncherLiquidGlass.Config()
    var dark = false
    /** Corner radius in px; negative for a capsule. */
    var cornerRadius = -1f
    /** Only the sheen and rim, for surfaces that draw their own tinted fill. */
    var rimOnly = false
    /** Overrides the tint alpha (0..1); negative uses the settings. */
    var tintAlphaOverride = -1f

    private fun rebuildIfNeeded(r: Float) {
        val w = bounds.width()
        val h = bounds.height()
        if (w == builtWidth && h == builtHeight && r == builtRadius && config == builtConfig &&
            dark == builtDark) {
            return
        }
        builtWidth = w
        builtHeight = h
        builtRadius = r
        builtConfig = config
        builtDark = dark
        // Shaders are built at the origin; draw() translates to the bounds.
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        val a = config.rimAlpha()
        sheen.shader =
            LinearGradient(0f, 0f, 0f, h * 0.55f,
                white((if (dark) 0.10f else 0.22f) * a), 0x00FFFFFF, Shader.TileMode.CLAMP)
        val reach = max(w, h) / 2f
        val lx = config.lightX()
        val ly = config.lightY()
        val cx = w / 2f
        val cy = h / 2f
        rim.shader =
            LinearGradient(
                cx + lx * reach, cy + ly * reach, cx - lx * reach, cy - ly * reach,
                intArrayOf(white(a), white(a * 0.12f), white(a * 0.45f)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
        // Strokes run along the outline pulled in by half their width: no clipPath needed.
        val stroke = max(1f, density)
        rim.strokeWidth = stroke
        hairline.strokeWidth = stroke
        strokeRect.set(rect)
        strokeRect.inset(stroke / 2f, stroke / 2f)
        strokeRadius = max(0f, r - stroke / 2f)
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty || drawAlpha == 0) return
        val r =
            if (cornerRadius < 0f) min(bounds.width(), bounds.height()) / 2f
            else min(cornerRadius, min(bounds.width(), bounds.height()) / 2f)
        rebuildIfNeeded(r)
        val save = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())

        if (!rimOnly) {
            val alpha = if (tintAlphaOverride >= 0f) tintAlphaOverride else config.tintAlpha(dark)
            fill.color = config.tintColor(dark, alpha * drawAlpha / 255f)
            canvas.drawRoundRect(rect, r, r, fill)
        }
        sheen.alpha = drawAlpha
        canvas.drawRoundRect(rect, r, r, sheen)

        hairline.color = if (dark) 0x0FFFFFFF else 0x12000000
        hairline.alpha = hairline.alpha * drawAlpha / 255
        rim.alpha = drawAlpha
        canvas.drawRoundRect(strokeRect, strokeRadius, strokeRadius, hairline)
        canvas.drawRoundRect(strokeRect, strokeRadius, strokeRadius, rim)
        canvas.restoreToCount(save)
    }

    private fun white(alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255f).roundToInt() shl 24) or 0xFFFFFF

    override fun setAlpha(alpha: Int) {
        if (drawAlpha == alpha) return
        drawAlpha = alpha
        invalidateSelf()
    }

    override fun getAlpha(): Int = drawAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
