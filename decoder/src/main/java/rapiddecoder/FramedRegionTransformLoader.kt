package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import rapiddecoder.frame.FramingMethod

internal class FramedRegionTransformLoader(private val source: BitmapLoader,
                                           private val framingMethod: FramingMethod,
                                           private val frameWidth: Int,
                                           private val frameHeight: Int,
                                           private val background: Drawable? = null,
                                           private val l: Int,
                                           private val t: Int,
                                           private val r: Int,
                                           private val b: Int) : BitmapLoader() {
    private val left: Int
        get() = l.coerceAtLeast(0)
    private val top: Int
        get() = t.coerceAtLeast(0)
    private val right: Int
        get() = r.coerceAtMost(frameWidth)
    private val bottom: Int
        get() = b.coerceAtMost(frameHeight)

    override val sourceWidth: Int
        get() = source.sourceWidth
    override val sourceHeight: Int
        get() = source.sourceHeight
    override val width: Int
        get() = right - left
    override val height: Int
        get() = bottom - top
    override val mimeType: String?
        get() = source.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (this.width == width && this.height == height) {
            this
        } else {
            ScaleToTransformLoader(this, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleByTransformLoader(this, x, y)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        val newLeft = this.left + left
        val newTop = this.top + top
        return FramedRegionTransformLoader(source, framingMethod, frameWidth, frameHeight,
                background, newLeft, newTop, newLeft + (right - left), newTop + (bottom - top))
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val sourceBounds = Rect()
        val destBounds = Rect()
        framingMethod.getBounds(source.width, source.height, frameWidth, frameHeight, sourceBounds,
                destBounds)

        val destRegionBounds = Rect()
        val region = Rect(left, top, right, bottom)
        if (destRegionBounds.setIntersect(destBounds, region)) {
            val widthDelta = sourceBounds.left - destBounds.left
            val heightDelta = sourceBounds.top - destBounds.top
            val widthRatio = sourceBounds.width().toFloat() / destBounds.width()
            val heightRatio = sourceBounds.height().toFloat() / destBounds.height()

            val sourceRegionLeft = widthDelta + destRegionBounds.left.toFloat() * widthRatio
            val sourceRegionTop = heightDelta + destRegionBounds.top.toFloat() * heightRatio
            val sourceRegionRight = sourceRegionLeft +
                    destRegionBounds.width().toFloat() * widthRatio
            val sourceRegionBottom = sourceRegionTop +
                    destRegionBounds.height().toFloat() * heightRatio

            val newOptions = options.buildUpon().setFinalScale(false).build()
            val sourceBitmap = source.region(
                    Math.round(sourceRegionLeft),
                    Math.round(sourceRegionTop),
                    Math.round(sourceRegionRight),
                    Math.round(sourceRegionBottom)).loadBitmap(newOptions)

            if (destRegionBounds == region) {
                val scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap,
                        region.width(), region.height(), true)
                if (scaledBitmap !== sourceBitmap) {
                    sourceBitmap.recycle()
                }
                return scaledBitmap
            } else {
                val bitmap = Bitmap.createBitmap(region.width(), region.height(),
                        sourceBitmap.config)
                val canvas = Canvas(bitmap)
                drawBackground(canvas, region)
                val paint = if (newOptions.filterBitmap) Paint(Paint.FILTER_BITMAP_FLAG) else null
                destRegionBounds.offset(-region.left, -region.top)
                canvas.drawBitmap(sourceBitmap, null, destRegionBounds, paint)
                sourceBitmap.recycle()
                return bitmap
            }
        } else {
            val bitmap = Bitmap.createBitmap(region.width(), region.height(),
                    options.config ?: Bitmap.Config.ARGB_8888)
            drawBackground(Canvas(bitmap), region)
            return bitmap
        }
    }

    private fun drawBackground(canvas: Canvas, region: Rect) {
        background?.run {
            setBounds(-region.left, -region.top,
                    -region.left + frameWidth, -region.top + frameHeight)
            draw(canvas)
        }
    }

    override fun hasMetadata(type: MetadataType): Boolean =
            type != MetadataType.DENSITY_SCALE
}