package rapiddecoder.util

object AspectRatioCalculator {
    fun getHeight(width: Int, height: Int, targetWidth: Int): Int {
        val ratio = height.toDouble() / width
        return Math.round(ratio * targetWidth).toInt()
    }

    fun getWidth(width: Int, height: Int, targetHeight: Int): Int {
        val ratio = width.toDouble() / height
        return Math.round(ratio * targetHeight).toInt()
    }

    fun getHeight(width: Float, height: Float, targetWidth: Float): Float =
            height / width * targetWidth

    fun getWidth(width: Float, height: Float, targetHeight: Float): Float =
            width / height * targetHeight
}
