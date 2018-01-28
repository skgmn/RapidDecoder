package rapiddecoder.test

import android.graphics.Bitmap

internal class MemoryEagerBitmapLoader(private val bitmap: Bitmap): EagerBitmapLoader() {
    override fun scaleTo(width: Int, height: Int): EagerBitmapLoader {
        val newBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return MemoryEagerBitmapLoader(newBitmap)
    }

    override fun scaleBy(x: Float, y: Float): EagerBitmapLoader {
        val newBitmap = Bitmap.createScaledBitmap(
                bitmap,
                Math.round(bitmap.width * x),
                Math.round(bitmap.height * y),
                true)
        return MemoryEagerBitmapLoader(newBitmap)
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): EagerBitmapLoader {
        val newBitmap = Bitmap.createBitmap(bitmap, left, top,
                right - left, bottom - top, null, true)
        return MemoryEagerBitmapLoader(newBitmap)
    }

    override fun loadBitmap(): Bitmap = bitmap
}