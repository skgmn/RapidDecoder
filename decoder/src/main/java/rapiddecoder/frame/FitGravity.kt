package rapiddecoder.frame

import android.graphics.Rect
import android.view.Gravity
import rapid.decoder.frame.AspectRatioCalculator

internal class FitGravity(private val gravity: Int) : FramingMethod {
    override fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int, outSrc: Rect, outDest: Rect) {
        outSrc.set(0, 0, sourceWidth, sourceHeight)

        val targetWidth: Int
        var targetHeight = AspectRatioCalculator.getHeight(sourceWidth, sourceHeight, frameWidth)
        if (targetHeight <= frameHeight) {
            targetWidth = frameWidth
        } else {
            targetWidth = AspectRatioCalculator.getWidth(sourceWidth, sourceHeight, frameHeight)
            targetHeight = frameHeight
        }
        when (gravity) {
            Gravity.START -> outDest.set(0, 0, targetWidth, targetHeight)
            Gravity.CENTER -> {
                outDest.left = (frameWidth - targetWidth) / 2
                outDest.top = (frameHeight - targetHeight) / 2
                outDest.right = outDest.left + targetWidth
                outDest.bottom = outDest.top + targetHeight
            }
            Gravity.END -> {
                outDest.right = frameWidth
                outDest.bottom = frameHeight
                outDest.left = outDest.right - targetWidth
                outDest.top = outDest.bottom - targetHeight
            }
            else -> outDest.set(0, 0, targetWidth, targetHeight)
        }
    }
}