package rapiddecoder

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.PointF
import android.support.test.InstrumentationRegistry
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rapiddecoder.test.EagerBitmapLoader
import rapiddecoder.test.R

class LoadResourceTest {
    private lateinit var context: Context
    private lateinit var res: Resources

    private lateinit var testTargets: Array<TestTarget>

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        res = context.resources

        if (!fileExported) {
            fileExported = true
            context.openFileOutput("soap_bubble.jpg", Context.MODE_PRIVATE).use { output ->
                context.assets.open("soap_bubble.jpg").use { input ->
                    input.copyTo(output)
                }
            }
        }

        testTargets = arrayOf(
                TestTarget(
                        name = "res/android",
                        loaderProvider = {
                            BitmapLoader.fromResource(res, R.drawable.android)
                        },
                        eagerLoaderProvider = {
                            EagerBitmapLoader.fromResources(res, R.drawable.android)
                        }),
                TestTarget(
                        name = "res/dummy_image",
                        loaderProvider = {
                            BitmapLoader.fromResource(res, R.drawable.dummy_image)
                        },
                        eagerLoaderProvider = {
                            EagerBitmapLoader.fromResources(res, R.drawable.dummy_image)
                        }),
                TestTarget(
                        name = "res/pond",
                        loaderProvider = {
                            BitmapLoader.fromResource(res, R.drawable.pond)
                        },
                        eagerLoaderProvider = {
                            EagerBitmapLoader.fromResources(res, R.drawable.pond)
                        }),
                TestTarget(
                        name = "asset/doomsday_rule",
                        loaderProvider = {
                            BitmapLoader.fromAsset(context, "doomsday_rule.jpg")
                        },
                        eagerLoaderProvider = {
                            EagerBitmapLoader.fromAsset(context, "doomsday_rule.jpg")
                        }),
                TestTarget(
                        name = "stream/img_fjords",
                        loaderProvider = {
                            BitmapLoader.fromStream(context.assets.open("img_fjords.jpg"))
                        },
                        eagerLoaderProvider = {
                            EagerBitmapLoader.fromStream(context.assets.open("img_fjords.jpg"))
                        }),
                TestTarget(
                        name = "file/soap_bubble",
                        loaderProvider = {
                            BitmapLoader.fromFile(context.getFileStreamPath("soap_bubble.jpg"))
                        },
                        eagerLoaderProvider = {
                            EagerBitmapLoader.fromFile(context.getFileStreamPath("soap_bubble.jpg"))
                        })
        )
    }

    @Test
    fun decodeFull() {
        testTargets.forEach { t ->
            val loader = t.loaderProvider()
            val bitmap1 = loader.loadBitmap()
            val bitmap2 = t.eagerLoaderProvider().loadBitmap()
            assertTrue(t.name, bitmap1.sameAs(bitmap2))
            assertEquals(t.name, bitmap1.width, loader.width)
            assertEquals(t.name, bitmap1.height, loader.height)
        }
        testTargets.forEach { t ->
            val loader = t.loaderProvider()
            val width = loader.width
            val height = loader.height

            val bitmap1 = loader.loadBitmap()
            val bitmap2 = t.eagerLoaderProvider().loadBitmap()
            assertEquals(t.name, bitmap1.width, width)
            assertEquals(t.name, bitmap1.height, height)
            assertTrue(t.name, bitmap1.sameAs(bitmap2))
        }
    }

    @Test
    fun scaleTo() {
        // Some generated random values
        val testDimensions = arrayOf(
                Point(471, 53),
                Point(335, 480),
                Point(187, 365),
                Point(206, 275),
                Point(56, 113),
                Point(98, 163),
                Point(286, 236),
                Point(292, 328),
                Point(364, 43),
                Point(282, 457)
        )
        testTargets.forEach { t ->
            testDimensions.forEach { dimension ->
                val loader = t.loaderProvider()
                        .scaleTo(dimension.x, dimension.y)
                val bitmap1 = loader.loadBitmap()
                val bitmap2 = t.eagerLoaderProvider()
                        .scaleTo(dimension.x, dimension.y)
                        .loadBitmap()
                assertTrue("${t.name}.scaleTo(${dimension.x}, ${dimension.y})",
                        bitmap1.sameAs(bitmap2))
                assertEquals(bitmap1.width, loader.width)
                assertEquals(bitmap1.height, loader.height)
            }
            testDimensions.forEach { dimension ->
                val loader = t.loaderProvider()
                        .scaleTo(dimension.x, dimension.y)
                val width = loader.width
                val height = loader.height

                val bitmap1 = loader.loadBitmap()
                val bitmap2 = t.eagerLoaderProvider()
                        .scaleTo(dimension.x, dimension.y)
                        .loadBitmap()
                assertEquals(bitmap1.width, width)
                assertEquals(bitmap1.height, height)
                assertTrue("${t.name}.scaleTo(${dimension.x}, ${dimension.y})",
                        bitmap1.sameAs(bitmap2))
            }
        }
    }

    @Test
    fun scaleBy() {
        // Some generated random values
        val testScales = arrayOf(
                PointF(0.5f, 0.5f),
                PointF(0.969f, 0.568f),
                PointF(0.051f, 0.779f),
                PointF(0.926f, 0.367f),
                PointF(0.561f, 0.162f),
                PointF(0.104f, 0.276f),
                PointF(1.090f, 2.064f),
                PointF(2.691f, 2.423f),
                PointF(1.674f, 1.349f),
                PointF(2.475f, 2.322f),
                PointF(2.287f, 2.965f)
        )
        testTargets.forEach { t ->
            testScales.forEach { dimension ->
                val loader = t.loaderProvider()
                        .scaleBy(dimension.x, dimension.y)
                val bitmap1 = loader.loadBitmap()
                val bitmap2 = t.eagerLoaderProvider()
                        .scaleBy(dimension.x, dimension.y)
                        .loadBitmap()
                val message = "${t.name}.scaleBy(${dimension.x}, ${dimension.y})"
                assertEquals(message, bitmap2.width, bitmap1.width)
                assertEquals(message, bitmap2.height, bitmap1.height)
                assertTrue(message, bitmap1.sameAs(bitmap2))
                assertEquals(message, bitmap1.width, loader.width)
                assertEquals(message, bitmap1.height, loader.height)
            }
            testScales.forEach { dimension ->
                val loader = t.loaderProvider()
                        .scaleBy(dimension.x, dimension.y)
                val width = loader.width
                val height = loader.height

                val bitmap1 = loader.loadBitmap()
                val bitmap2 = t.eagerLoaderProvider()
                        .scaleBy(dimension.x, dimension.y)
                        .loadBitmap()
                assertEquals(bitmap1.width, width)
                assertEquals(bitmap1.height, height)
                assertTrue("${t.name}.scaleBy(${dimension.x}, ${dimension.y})",
                        bitmap1.sameAs(bitmap2))
            }
        }
    }

    @Test
    fun scaleWidth() {
        val testWidths = intArrayOf(
                404/*, 311, 448, 152, 278,
                127, 466, 258, 329, 483*/
        )
        testTargets.forEach { t ->
            testWidths.forEach { w ->
                val loader = t.loaderProvider()
                        .scaleWidth(w)
                val bitmap1 = loader.loadBitmap()
                val bitmap2 = t.eagerLoaderProvider()
                        .scaleWidth(w)
                        .loadBitmap()
                assertEquals(bitmap1.width, loader.width)
                assertEquals(bitmap1.height, loader.height)
                assertTrue("${t.name}.scaleWidth($w)",
                        bitmap1.sameAs(bitmap2))
            }
            testWidths.forEach { w ->
                val loader = t.loaderProvider()
                        .scaleWidth(w)
                val width = loader.width
                val height = loader.height

                val bitmap1 = loader.loadBitmap()
                val bitmap2 = t.eagerLoaderProvider()
                        .scaleWidth(w)
                        .loadBitmap()
                assertEquals(bitmap1.width, width)
                assertEquals(bitmap1.height, height)
                assertTrue("${t.name}.scaleWidth($w)",
                        bitmap1.sameAs(bitmap2))
            }
        }
    }

    companion object {
        private var fileExported = false
    }
}