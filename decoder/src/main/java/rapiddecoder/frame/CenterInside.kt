package rapiddecoder.frame

import android.graphics.Rect
import rapid.decoder.frame.AspectRatioCalculator

internal class CenterInside : FramingMethod {
    override fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int, outSrc: Rect, outDest: Rect) {
        outSrc.set(0, 0, sourceWidth, sourceHeight)
        if (sourceWidth <= frameWidth && sourceHeight <= frameHeight) {
            outDest.left = (frameWidth - sourceWidth) / 2
            outDest.top = (frameHeight - sourceHeight) / 2
            outDest.right = outDest.left + sourceWidth
            outDest.bottom = outDest.top + sourceHeight
        } else {
            val targetWidth: Int
            var targetHeight = AspectRatioCalculator.getHeight(sourceWidth, sourceHeight, frameWidth)
            if (targetHeight <= frameHeight) {
                targetWidth = frameWidth
            } else {
                targetWidth = AspectRatioCalculator.getWidth(sourceWidth, sourceHeight, frameHeight)
                targetHeight = frameHeight
            }

            outDest.left = (frameWidth - targetWidth) / 2
            outDest.top = (frameHeight - targetHeight) / 2
            outDest.right = outDest.left + targetWidth
            outDest.bottom = outDest.top + targetHeight
        }
    }
}