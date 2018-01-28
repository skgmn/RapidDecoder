package rapiddecoder.decoder

import rapiddecoder.LoadBitmapOptions

internal class BitmapDecodeInput(
        var finalScale: Boolean = true,
        var scaleX: Float = 1f,
        var scaleY: Float = 1f) {
    constructor(options: LoadBitmapOptions): this(options.finalScale)

    constructor(other: BitmapDecodeInput): this(
            finalScale = other.finalScale,
            scaleX = other.scaleX,
            scaleY = other.scaleY
    )
}