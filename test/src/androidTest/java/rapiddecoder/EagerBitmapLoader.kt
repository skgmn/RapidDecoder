package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap

internal abstract class EagerBitmapLoader {
    abstract fun scaleTo(width: Int, height: Int): EagerBitmapLoader
    abstract fun scaleBy(x: Float, y: Float): EagerBitmapLoader
    abstract fun region(left: Int, top: Int, right: Int, bottom: Int): EagerBitmapLoader
    abstract fun loadBitmap(): Bitmap

    companion object {
        fun fromResources(res: Resources, id: Int): EagerBitmapLoader =
                ResourceEagerBitmapLoader(res, id)
    }
}