package rapiddecoder

import android.graphics.Rect

interface Framer {
    fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int,
                  outSrc: Rect, outDest: Rect)
}