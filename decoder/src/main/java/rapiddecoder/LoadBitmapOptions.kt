package rapiddecoder

class LoadBitmapOptions(val finalScale: Boolean = true) {
    class Builder() {
        private var finalScale = true

        fun setFinalScale(finalScale: Boolean): Builder {
            this.finalScale = finalScale
            return this
        }

        fun build(): LoadBitmapOptions {
            return LoadBitmapOptions(
                    finalScale = finalScale)
        }
    }
}