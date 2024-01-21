// Initially was from https://github.com/material-components/material-components-android/blob/7bc26e5/lib/java/com/google/android/material/carousel/MaskableFrameLayout.java
// but turned to Kotlin, removed use of Material library internal things and highly simplified.
// We used to use the Material's one but since 1.10.0 it has become unusable, it just shows nothing
// and probably we shouldn't have used it at the first place as it was intended to be used by carousel.
package com.byagowi.persiancalendar.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.graphics.withClip
import com.google.android.material.shape.AbsoluteCornerSize
import com.google.android.material.shape.CornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapeAppearancePathProvider
import com.google.android.material.shape.Shapeable
import kotlin.math.min

/** A [FrameLayout] than is able to shape itself and all children.  */
class ShapeableFrameLayout(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs), Shapeable {
    private val bounds = RectF()
    private var shapeAppearanceModel =
        ShapeAppearanceModel.builder(context, attrs, 0, 0, 0).build()
    private val shapePath = Path()
    private val pathProvider = ShapeAppearancePathProvider()

    /**
     * For API 33+ it uses {@link ViewOutlineProvider} to clip for all shapes as
     * {@link Outline#setPath(Path)} was added in API 33 which allows use of a
     * ViewOutlineProvider to clip for all shapes.
     */
    @ChecksSdkIntAtLeast(Build.VERSION_CODES.TIRAMISU)
    private val usesViewOutline = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    init {
        setShapeAppearanceModel(shapeAppearanceModel)
        if (usesViewOutline) {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    if (!shapePath.isEmpty) outline.setPath(shapePath)
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(0f, 0f, width.toFloat(), height.toFloat())
        updateShapePath()
        invalidateClippingMethod(this)
    }

    private fun clampCornerSize(cornerSize: CornerSize): CornerSize {
        /** See [com.google.android.material.carousel.MaskableFrameLayout.setShapeAppearanceModel] */
        return if (cornerSize is AbsoluteCornerSize) {
            // Enforce that the corners of the shape appearance are never larger than half the
            // width of the shortest edge. As the size of the mask changes, we never want the
            // corners to be larger than half the width or height of this view.
            CornerSize { min(cornerSize.cornerSize, min(it.width() / 2f, it.height() / 2f)) }
        } else {
            // Relative corner size already enforces a max size based on shortest edge.
            cornerSize
        }
    }

    override fun setShapeAppearanceModel(model: ShapeAppearanceModel) {
        this.shapeAppearanceModel = model.toBuilder()
            .setBottomLeftCornerSize(clampCornerSize(model.bottomLeftCornerSize))
            .setBottomRightCornerSize(clampCornerSize(model.bottomRightCornerSize))
            .setTopLeftCornerSize(clampCornerSize(model.topLeftCornerSize))
            .setTopRightCornerSize(clampCornerSize(model.topRightCornerSize))
            .build()
        updateShapePath()
        invalidateClippingMethod(this)
    }

    override fun getShapeAppearanceModel() = shapeAppearanceModel

    override fun dispatchDraw(canvas: Canvas) {
        if (usesViewOutline || shapePath.isEmpty) super.dispatchDraw(canvas)
        else canvas.withClip(shapePath) { super.dispatchDraw(canvas) }
    }

    private fun invalidateClippingMethod(view: View) {
        if (usesViewOutline) {
            view.clipToOutline = true
            view.invalidateOutline()
        } else if (!bounds.isEmpty) view.invalidate()
    }

    private fun updateShapePath() {
        if (!bounds.isEmpty)
            pathProvider.calculatePath(shapeAppearanceModel, 1f, bounds, shapePath)
    }
}
