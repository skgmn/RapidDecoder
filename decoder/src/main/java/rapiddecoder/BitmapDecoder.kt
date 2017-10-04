package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build

internal abstract class BitmapDecoder : BitmapLoader() {
    protected val decodeLock = Any()

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        if (hasSize && width == this.width && height == this.height) {
            return this
        }
        return ScaleToTransformDecoder(this, width.toFloat(), height.toFloat())
    }

    override fun scaleWidth(width: Int): BitmapLoader {
        checkScaleToArguments(width, 1)
        return ScaleWidthTransformDecoder(this, width.toFloat(), 1f)
    }

    override fun scaleHeight(height: Int): BitmapLoader {
        checkScaleToArguments(1, height)
        return ScaleHeightTransformDecoder(this, height.toFloat(), 1f)
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleByTransformDecoder(this, x, y)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (hasSize && left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        return RegionTransformDecoder(this, left, top, right, bottom)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = options.config
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            opts.inMutable = options.mutable
        }
        return synchronized(decodeLock) {
            decode(opts).let {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB &&
                        options.mutable &&
                        !it.isMutable) {
                    val clone = Bitmap.createBitmap(it.width, it.height, it.config)
                    Canvas(clone).drawBitmap(it, 0f, 0f, null)
                    it.recycle()
                    clone
                } else {
                    it
                }
            }
        }
    }

    internal abstract fun decode(opts: BitmapFactory.Options): Bitmap
    internal abstract fun decodeRegion(region: Rect, opts: BitmapFactory.Options): Bitmap
    internal abstract fun decodeBounds(opts: BitmapFactory.Options)
}