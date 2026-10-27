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

package com.android.launcher3.util

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.view.View
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import com.android.launcher3.R
import com.android.launcher3.dagger.ActivityContextSingleton
import com.android.launcher3.folder.Folder
import com.android.launcher3.graphics.PathWrapper
import com.android.launcher3.util.WindowBlurState.WINDOW_BLUR_STATE
import com.android.launcher3.views.ActivityContext
import javax.inject.Inject
import javax.inject.Named

/**
 * Quickstep implementation of the helper class that creates and updates the blur drawable used
 * for folders and the homescreen popup.
 */
@ActivityContextSingleton
class QuickstepBackgroundBlurHelper
@Inject
constructor(
    private val activityContext: ActivityContext,
    @Named(WINDOW_BLUR_STATE) private val blurState: ListenableRef<Boolean>,
) : BlurBackgroundHelper() {

    /**
     * The blur state of one surface (the folder or the popup). Each surface needs its own render
     * node and blur drawable: a popup can open on top of an open folder, and sharing them lets the
     * folder draw the node that the popup is recording, which makes the node draw itself.
     */
    private inner class BlurLayer(name: String) {
        val workspaceRenderNode = RenderNode(name)
        val workspaceRenderNodeOutline = Outline()
        val crossWindowBlurDrawable: BackgroundBlurDrawable? by lazy {
            if (!isBlurEnabled()) null
            else
                activityContext.dragLayer.getViewRootImpl()
                    .createBackgroundBlurDrawable()?.apply {
                        setBlurRadius(folderBlurRadius.toInt())
                        setVisible(false, false)
                    }
        }

        fun close() {
            if (workspaceRenderNode.hasDisplayList()) {
                workspaceRenderNode.discardDisplayList()
            }
            crossWindowBlurDrawable?.setVisible(false, false)
        }
    }

    private val folderBlurRadius = activityContext.asContext().resources.getDimension(
        R.dimen.folder_blur_radius
    )

    private val cornerRadius = Themes.getDialogCornerRadius(activityContext.asContext())
    private val bounds = Rect()
    private val folderLayer = BlurLayer("folderWorkspaceBlur")
    private val popupLayer = BlurLayer("popupWorkspaceBlur")

    /** True while the drag layer is being recorded into a render node. */
    private var isRecordingSnapshot = false

    override fun prepareToOpenFolder(folder: Folder) {
        if (!isBlurEnabled()) {
            return
        }

        val folderIcon = folder.folderIcon
        val folderNameVisibility: Int = folderIcon.folderName.visibility
        val isIconVisible = folderIcon.iconVisible

        folderIcon.setTextVisible(false)
        folderIcon.setIconVisible(false)

        recordWorkspaceSnapshot(folderLayer)

        folderIcon.folderName.visibility = folderNameVisibility
        folderIcon.setIconVisible(isIconVisible)
    }

    override fun drawFolderBlur(canvas: Canvas, pathWrapper: PathWrapper?, view: View) {
        drawBlur(folderLayer, canvas, pathWrapper, view)
    }

    private fun drawBlur(layer: BlurLayer, canvas: Canvas, pathWrapper: PathWrapper?, view: View) {
        // While the drag layer is recorded into a snapshot, views inside it (e.g. an open folder)
        // must not draw blur: they would add a node to a snapshot that is itself being recorded.
        if (!isBlurEnabled() || isRecordingSnapshot) {
            return
        }

        drawCrossWindowBlur(layer, canvas, pathWrapper, view)
        drawWorkspaceBlur(layer, canvas, pathWrapper?.path, view)
    }

    private fun recordWorkspaceSnapshot(layer: BlurLayer) {
        val dragLayer = activityContext.dragLayer
        val node = layer.workspaceRenderNode
        isRecordingSnapshot = true
        try {
            val canvas = node.beginRecording(dragLayer.getWidth(), dragLayer.getHeight())
            try {
                dragLayer.draw(canvas)
            } finally {
                node.endRecording()
            }
        } finally {
            isRecordingSnapshot = false
        }
        node.setPosition(0, 0, dragLayer.getWidth(), dragLayer.getHeight())
    }

    private fun drawWorkspaceBlur(layer: BlurLayer, canvas: Canvas, path: Path?, view: View) {
        val node = layer.workspaceRenderNode
        if (!node.hasDisplayList()) {
            return
        }
        node.translationX = -view.left.toFloat()
        node.translationY = -view.top.toFloat()

        val outline = layer.workspaceRenderNodeOutline
        if (path != null && !path.isEmpty) {
            outline.setPath(path)
            outline.mPath.offset(view.left.toFloat(), view.top.toFloat())
        } else {
            outline.setRoundRect(
                view.left,
                view.top,
                view.right,
                view.bottom,
                cornerRadius,
            )
        }
        node.setOutline(outline)
        node.setClipToOutline(true)

        node.setRenderEffect(
            RenderEffect.createBlurEffect(
                folderBlurRadius,
                folderBlurRadius,
                Shader.TileMode.CLAMP
            )
        )
        canvas.drawRenderNode(node)
    }

    private fun drawCrossWindowBlur(
        layer: BlurLayer,
        canvas: Canvas,
        pathWrapper: PathWrapper?,
        view: View,
    ) {
        val d = layer.crossWindowBlurDrawable ?: return

        d.setVisible(true, false)
        if (pathWrapper != null) {
            pathWrapper.bounds.roundOut(bounds)
            d.bounds = bounds
            d.setCornerRadius(pathWrapper.cornerRadius)
        } else {
            d.setBounds(0, 0, view.width, view.height)
            d.setCornerRadius(cornerRadius)
        }
        d.draw(canvas)
    }

    override fun folderCloseComplete() {
        folderLayer.close()
    }

    override fun prepareToOpenPopup(popup: View) {
        if (!isBlurEnabled()) {
            return
        }

        val popupVisibility = popup.visibility
        popup.visibility = View.INVISIBLE

        recordWorkspaceSnapshot(popupLayer)

        popup.visibility = popupVisibility
    }

    override fun drawPopupBlur(canvas: Canvas, pathWrapper: PathWrapper?, view: View) {
        drawBlur(popupLayer, canvas, pathWrapper, view)
    }

    override fun popupCloseComplete() {
        popupLayer.close()
    }

    override fun isBlurEnabled(): Boolean {
        return blurState.value
    }
}
