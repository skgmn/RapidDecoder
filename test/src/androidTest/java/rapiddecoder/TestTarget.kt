package rapiddecoder

import rapiddecoder.test.EagerBitmapLoader

internal class TestTarget(
        val loaderProvider: () -> BitmapLoader,
        val eagerLoaderProvider: () -> EagerBitmapLoader,
        val name: String)