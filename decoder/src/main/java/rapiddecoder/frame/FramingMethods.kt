package rapiddecoder.frame

import android.view.Gravity
import android.widget.ImageView

object FramingMethods {
    @JvmStatic
    fun fromScaleType(scaleType: ImageView.ScaleType): FramingMethod {
        return if (ImageView.ScaleType.MATRIX == scaleType) {
            Matrix()
        } else if (ImageView.ScaleType.FIT_XY == scaleType) {
            FitXy()
        } else if (ImageView.ScaleType.FIT_START == scaleType) {
            FitGravity(Gravity.START)
        } else if (ImageView.ScaleType.FIT_CENTER == scaleType) {
            FitGravity(Gravity.CENTER)
        } else if (ImageView.ScaleType.FIT_END == scaleType) {
            FitGravity(Gravity.END)
        } else if (ImageView.ScaleType.CENTER == scaleType) {
            Center()
        } else if (ImageView.ScaleType.CENTER_CROP == scaleType) {
            CenterCrop()
        } else if (ImageView.ScaleType.CENTER_INSIDE == scaleType) {
            CenterInside()
        } else {
            throw IllegalArgumentException("scaleType")
        }
    }
}