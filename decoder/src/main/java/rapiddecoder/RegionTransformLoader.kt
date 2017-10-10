package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Rect
import rapiddecoder.util.BitmapUtils

internal class RegionTransformLoader(private val other: BitmapLoader,
                                     private val left: Int,
                                     private val top: Int,
                                     private val right: Int,
                                     private val bottom: Int) : BitmapLoader() {
    private val l: Int
        get() = left.coerceAtLeast(0)
    private val t: Int
        get() = top.coerceAtLeast(0)
    private val r: Int
        get() = right.coerceAtMost(other.width)
    private val b: Int
        get() = bottom.coerceAtMost(other.height)

    override val sourceWidth: Int
        get() = other.sourceWidth
    override val sourceHeight: Int
        get() = other.sourceHeight
    override val width: Int
        get() = r - l
    override val height: Int
        get() = b - t
    override val mimeType: String?
        get() = other.mimeType
    override val hasSize: Boolean
        get() = other.hasSize

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        return if (hasSize && width == this.width && height == this.height) {
            this
        } else {
            ScaleToTransformLoader(this, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleByTransformLoader(this, x, y)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return if (hasSize && left <= 0 && top <= 0 && right >= width && bottom >= height) {
            this
        } else {
            val newLeft = this.left + left
            val newTop = this.top + top
            RegionTransformLoader(other, newLeft, newTop,
                    newLeft + (right - left), newTop + (bottom - top))
        }
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val l = l
        val t = t
        val r = r
        val b = b

        return if (l == 0 && t == 0 && r == other.width && b == other.height) {
            other.loadBitmap(options)
        } else {
            val bitmap = other.loadBitmap(options)
            val w = r - l
            val h = b - t
            if (options.shouldBeRedrawnFrom(bitmap)) {
                BitmapUtils.copy(bitmap, Rect(l, t, r, b), w, h,
                        options.config ?: bitmap.config, options.filterBitmap).also {
                    if (it !== bitmap) {
                        bitmap.recycle()
                    }
                }
            } else {
                Bitmap.createBitmap(bitmap, l, t, w, h)
            }
        }
    }
}