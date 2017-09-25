package rapiddecoder

class LoadBitmapOptions(val finalScale: Boolean = true,
                        val filterBitmap: Boolean = true) {
    fun buildUpon(): Builder =
            Builder(finalScale = finalScale,
                    filterBitmap = filterBitmap)

    class Builder(private var finalScale: Boolean = true,
                  private var filterBitmap: Boolean = true) {
        fun setFinalScale(finalScale: Boolean): Builder {
            this.finalScale = finalScale
            return this
        }

        fun setFilterBitmap(filterBitmap: Boolean): Builder {
            this.filterBitmap = filterBitmap
            return this
        }

        fun build(): LoadBitmapOptions {
            return LoadBitmapOptions(
                    finalScale = finalScale,
                    filterBitmap = filterBitmap)
        }
    }
}