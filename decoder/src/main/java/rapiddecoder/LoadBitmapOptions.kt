package rapiddecoder

import android.graphics.Bitmap

class LoadBitmapOptions(val finalScale: Boolean = true,
                        val filterBitmap: Boolean = true,
                        val config: Bitmap.Config? = null) {
    fun buildUpon(): Builder =
            Builder(finalScale = finalScale,
                    filterBitmap = filterBitmap,
                    config = config)

    class Builder(private var finalScale: Boolean = true,
                  private var filterBitmap: Boolean = true,
                  private var config: Bitmap.Config? = null) {
        fun setFinalScale(finalScale: Boolean): Builder {
            this.finalScale = finalScale
            return this
        }

        fun setFilterBitmap(filterBitmap: Boolean): Builder {
            this.filterBitmap = filterBitmap
            return this
        }

        fun setConfig(config: Bitmap.Config?): Builder {
            this.config = config
            return this
        }

        fun build(): LoadBitmapOptions {
            return LoadBitmapOptions(
                    finalScale = finalScale,
                    filterBitmap = filterBitmap,
                    config = config)
        }
    }
}