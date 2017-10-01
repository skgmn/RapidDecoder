package rapiddecoder.frame

import android.graphics.Rect

interface FramingMethod {
    fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int,
                  outSrc: Rect, outDest: Rect)
}