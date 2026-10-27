/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.launcher3.popup

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.CrossWindowBlurListeners
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import com.android.app.animation.Interpolators.EMPHASIZED_ACCELERATE
import com.android.app.animation.Interpolators.EMPHASIZED_DECELERATE
import com.android.launcher3.BubbleTextView
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R
import com.android.launcher3.util.Themes
import com.android.launcher3.views.BaseDragLayer
import kotlin.math.roundToInt

/**
 * Focus Mode: while the long-press popup of an app drawer icon is open, blurs the rest of the app
 * drawer and keeps only the popup and the selected icon sharp.
 *
 * The popup lives in the drag layer above the app drawer, so it is never affected by the blur. The
 * blur is a [RenderEffect] set on the app drawer view itself, which blurs the live content (icons,
 * labels, header, search bar) without any bitmap capture. Because the selected icon is a child of
 * that view it would be blurred as well, so it is made transparent in place and redrawn sharp by
 * [FocusOverlayView], which sits between the app drawer and the popup.
 *
 * If [RenderEffect] is unavailable or the system has disabled blurs (e.g. battery saver), only a
 * stronger scrim is used and the icon is left untouched.
 */
class FocusModeController
private constructor(private val launcher: Launcher, private val icon: BubbleTextView) {

    private val appsView = launcher.appsView
    private val blurRadiusPx = launcher.resources.getDimension(R.dimen.focus_mode_blur_radius)
    private val blurSupported =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            CrossWindowBlurListeners.getInstance().isCrossWindowBlurEnabled
    private val scrimAlpha = if (blurSupported) SCRIM_ALPHA else SCRIM_ALPHA_NO_BLUR

    private var overlay: FocusOverlayView? = null
    private var progress = 0f
    private var appliedRadius = 0
    private var savedIconAlpha = 1f
    private var iconHidden = false
    private var released = false

    /** Called from the popup's open animation creation; adds the blur-in to [anim]. */
    fun onOpen(popup: View, anim: AnimatorSet) {
        if (released) return
        if (overlay == null) attach(popup)
        anim.play(createProgressAnimator(1f, OPEN_DURATION_MS, EMPHASIZED_DECELERATE))
    }

    /** Called from the popup's close animation creation; adds the blur-out to [anim]. */
    fun onClose(anim: AnimatorSet) {
        if (released) return
        anim.play(createProgressAnimator(0f, CLOSE_DURATION_MS, EMPHASIZED_ACCELERATE))
        anim.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = release()
            }
        )
    }

    /** The selected icon is being dragged out of the popup; the drag owns it from now on. */
    fun onDragStarted() {
        overlay?.drawIcon = false
        restoreIcon()
    }

    /** Removes the blur and the overlay immediately. Safe to call multiple times. */
    fun release() {
        if (released) return
        released = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appsView.setRenderEffect(null)
        }
        overlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
        overlay = null
        restoreIcon()
    }

    private fun attach(popup: View) {
        val dragLayer = launcher.dragLayer
        val view = FocusOverlayView(launcher, icon, dragLayer, scrimColor())
        val lp =
            BaseDragLayer.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        lp.ignoreInsets = true
        // Keep the overlay directly below the popup (and its arrow, which is added after it).
        val popupIndex = dragLayer.indexOfChild(popup)
        dragLayer.addView(view, if (popupIndex >= 0) popupIndex else dragLayer.childCount, lp)
        overlay = view

        if (blurSupported) {
            // alpha (unlike visibility) keeps the icon tappable, so tapping it still launches the app.
            savedIconAlpha = icon.alpha
            icon.alpha = 0f
            iconHidden = true
        }
    }

    private fun restoreIcon() {
        if (iconHidden) {
            icon.alpha = savedIconAlpha
            iconHidden = false
        }
    }

    private fun createProgressAnimator(
        target: Float,
        durationMs: Long,
        interpolator: TimeInterpolator,
    ): ValueAnimator =
        ValueAnimator.ofFloat(progress, target).apply {
            duration = durationMs
            setInterpolator(interpolator)
            addUpdateListener { setProgress(it.animatedValue as Float) }
        }

    private fun setProgress(value: Float) {
        if (released) return
        progress = value
        overlay?.let {
            it.scrimProgress = value
            it.invalidate()
        }
        applyBlur(value)
    }

    private fun applyBlur(value: Float) {
        if (!blurSupported) return
        // Quantize to whole pixels to avoid building a new RenderEffect for sub-pixel changes.
        val radius = (blurRadiusPx * value).roundToInt()
        if (radius == appliedRadius) return
        appliedRadius = radius
        appsView.setRenderEffect(
            if (radius < 1) null
            else RenderEffect.createBlurEffect(radius.toFloat(), radius.toFloat(), Shader.TileMode.CLAMP)
        )
    }

    private fun scrimColor(): Int {
        val base = Themes.getAttrColor(launcher, R.attr.allAppsScrimColor)
        return ColorUtils.setAlphaComponent(base, (255 * scrimAlpha).roundToInt())
    }

    /** Draws the scrim and a sharp copy of the selected icon above the blurred app drawer. */
    private class FocusOverlayView(
        context: Context,
        private val icon: BubbleTextView,
        private val dragLayer: BaseDragLayer<*>,
        private val scrimColor: Int,
    ) : View(context) {

        var scrimProgress = 0f
        var drawIcon = true
        private val iconPos = FloatArray(2)

        init {
            // Purely visual: never take touches or show up for accessibility.
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = false
            isFocusable = false
        }

        override fun onDraw(canvas: Canvas) {
            if (scrimProgress > 0f) {
                val alpha = (Color.alpha(scrimColor) * scrimProgress).roundToInt()
                canvas.drawColor(ColorUtils.setAlphaComponent(scrimColor, alpha))
            }
            if (!drawIcon || !icon.isAttachedToWindow) return

            iconPos[0] = 0f
            iconPos[1] = 0f
            val scale = dragLayer.getDescendantCoordRelativeToSelf(icon, iconPos)
            val count = canvas.save()
            canvas.translate(iconPos[0], iconPos[1])
            canvas.scale(scale, scale)
            // Only the icon is kept; the app label is left out, as the popup replaces it.
            val iconBottom = icon.paddingTop + icon.iconHeight
            if (iconBottom > 0) canvas.clipRect(0, 0, icon.width, iconBottom)
            icon.draw(canvas)
            canvas.restoreToCount(count)
        }
    }

    companion object {
        private const val OPEN_DURATION_MS = 300L
        private const val CLOSE_DURATION_MS = 200L
        private const val SCRIM_ALPHA = 0.32f
        private const val SCRIM_ALPHA_NO_BLUR = 0.55f

        /**
         * Returns a controller if Focus Mode is enabled and [icon] is an app drawer icon of
         * [activityContext], otherwise null (original Launcher3 behavior).
         */
        @JvmStatic
        fun createIfApplicable(activityContext: Any?, icon: View): FocusModeController? {
            val launcher = activityContext as? Launcher ?: return null
            if (icon !is BubbleTextView) return null
            if (!LauncherPrefs.FOCUS_MODE.get(launcher)) return null
            if (!isInAppsView(launcher, icon)) return null
            return FocusModeController(launcher, icon)
        }

        private fun isInAppsView(launcher: Launcher, icon: View): Boolean {
            val appsView = launcher.appsView
            var parent = icon.parent
            while (parent != null) {
                if (parent === appsView) return true
                parent = parent.parent
            }
            return false
        }
    }
}
