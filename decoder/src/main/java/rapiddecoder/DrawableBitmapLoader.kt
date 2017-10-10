package rapiddecoder

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import rapiddecoder.util.BitmapUtils

internal class DrawableBitmapLoader(private val d: Drawable,
                                    left: Int, top: Int, right: Int, bottom: Int,
                                    private val targetWidth: Float,
                                    private val targetHeight: Float) : BitmapLoader() {
    init {
        if (d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) {
            throw IllegalArgumentException("Drawable without dimensions is not allowed")
        }
    }

    private val l: Int = left.coerceAtLeast(0)
    private val t: Int = top.coerceAtLeast(0)
    private val r: Int = right.coerceAtMost(d.intrinsicWidth)
    private val b: Int = bottom.coerceAtMost(d.intrinsicHeight)

    override val sourceWidth: Int
        get() = d.intrinsicWidth
    override val sourceHeight: Int
        get() = d.intrinsicHeight
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val mimeType: String?
        get() = "image/png"
    override val hasSize: Boolean
        get() = true

    override fun scaleTo(width: Int, height: Int): BitmapLoader =
            DrawableBitmapLoader(d, l, t, r, b, width.toFloat(), height.toFloat())

    override fun scaleBy(x: Float, y: Float): BitmapLoader =
            DrawableBitmapLoader(d, l, t, r, b, targetWidth * x, targetHeight * y)

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val l = left.coerceAtLeast(0)
        val t = top.coerceAtLeast(0)
        val r = right.coerceAtMost(width)
        val b = bottom.coerceAtMost(height)
        return if (l == 0 && t == 0 && r == width && b == height) {
            this
        } else {
            val w = right - left
            val h = bottom - top
            val scaleX = width.toFloat() / d.intrinsicWidth
            val scaleY = height.toFloat() / d.intrinsicHeight
            val newLeft = this.l + l / scaleX
            val newTop = this.t + t / scaleY
            val newRight = newLeft + w / scaleX
            val newBottom = newTop + h / scaleY
            DrawableBitmapLoader(d,
                    Math.round(newLeft), Math.round(newTop),
                    Math.round(newRight), Math.round(newBottom),
                    w.toFloat(), h.toFloat())
        }
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        if (d is BitmapDrawable) {
            val bitmap = d.bitmap
            if (!options.shouldBeRedrawnFrom(bitmap)) {
                val regionBitmap =
                        if (hasRegion()) {
                            return Bitmap.createBitmap(bitmap, l, t, r - l, b - t)
                        } else {
                            bitmap
                        }
                return Bitmap.createScaledBitmap(regionBitmap, width, height, options.filterBitmap)
            }
        }
        val config = options.config ?:
                if (d.opacity == PixelFormat.OPAQUE) {
                    Bitmap.Config.RGB_565
                } else {
                    Bitmap.Config.ARGB_8888
                }
        val region =
                if (hasRegion()) {
                    Rect(l, t, r, b)
                } else {
                    null
                }
        return BitmapUtils.copy(d, region, width, height, config, options.filterBitmap)
    }

    private fun hasRegion(): Boolean =
            l != 0 || t != 0 || r != d.intrinsicWidth || b != d.intrinsicHeight
}