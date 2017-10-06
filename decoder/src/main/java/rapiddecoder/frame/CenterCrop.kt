package rapiddecoder.frame

import android.graphics.Rect
import rapid.decoder.frame.AspectRatioCalculator

internal class CenterCrop : FramingMethod {
    override fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int,
                           outSrc: Rect, outDest: Rect) {
        val targetWidth: Int
        var targetHeight = AspectRatioCalculator.getHeight(sourceWidth, sourceHeight, frameWidth)
        if (targetHeight >= frameHeight) {
            targetWidth = frameWidth
        } else {
            targetWidth = AspectRatioCalculator.getWidth(sourceWidth, sourceHeight, frameHeight)
            targetHeight = frameHeight
        }

        val targetLeft = (frameWidth - targetWidth) / 2
        val targetTop = (frameHeight - targetHeight) / 2

        val ratioWidth = targetWidth.toFloat() / sourceWidth
        val ratioHeight = targetHeight.toFloat() / sourceHeight

        outSrc.left = Math.round(-targetLeft / ratioWidth)
        outSrc.top = Math.round(-targetTop / ratioHeight)
        outSrc.right = outSrc.left + Math.round(frameWidth / ratioWidth)
        outSrc.bottom = outSrc.top + Math.round(frameHeight / ratioHeight)

        outDest.set(0, 0, frameWidth, frameHeight)
    }
}