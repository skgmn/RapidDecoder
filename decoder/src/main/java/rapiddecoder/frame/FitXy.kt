package rapiddecoder.frame

import android.graphics.Rect

class FitXy : FramingMethod {
    override fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int, outSrc: Rect, outDest: Rect) {
        outSrc.set(0, 0, sourceWidth, sourceHeight)
        outDest.set(0, 0, frameWidth, frameHeight)
    }
}