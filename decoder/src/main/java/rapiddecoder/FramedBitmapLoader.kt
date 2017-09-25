package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable

abstract class FramedBitmapLoader(private val source: BitmapLoader,
                                  private val frameWidth: Int,
                                  private val frameHeight: Int,
                                  private val background: Drawable? = null) : BitmapLoader() {
    override val sourceWidth: Int
        get() = source.sourceWidth
    override val sourceHeight: Int
        get() = source.sourceHeight
    override val width: Int
        get() = frameWidth
    override val height: Int
        get() = frameHeight
    override val mimeType: String?
        get() = source.mimeType
    override val hasSize: Boolean
        get() = true

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        if (frameWidth == width && frameHeight == height) {
            return this
        } else {
            return ScaleToTransformLoader(this, width.toFloat(), height.toFloat())
        }
    }

    protected abstract fun getBounds(sourceWidth: Int, sourceHeight: Int,
                                     frameWidth: Int, frameHeight: Int,
                                     outSrc: Rect, outDest: Rect)

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val sourceBounds = Rect()
        val destBounds = Rect()
        getBounds(source.width, source.height, frameWidth, frameHeight, sourceBounds, destBounds)

        val newOptions = options.buildUpon().setFinalScale(false).build()
        val sourceBitmap = source.region(sourceBounds).loadBitmap(newOptions)

        return if (destBounds.left == 0 && destBounds.top == 0 &&
                destBounds.right == frameWidth && destBounds.bottom == frameHeight) {
            Bitmap.createScaledBitmap(sourceBitmap, frameWidth, frameHeight, true)
        } else {
            val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, sourceBitmap.config)
            val canvas = Canvas(bitmap)
            background?.run {
                setBounds(0, 0, frameWidth, frameHeight)
                draw(canvas)
            }
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sourceBitmap, null, destBounds, paint)
            bitmap
        }
    }
}