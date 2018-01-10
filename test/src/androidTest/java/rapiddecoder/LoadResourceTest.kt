package rapiddecoder

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.PointF
import android.support.test.InstrumentationRegistry
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rapiddecoder.test.R

class LoadResourceTest {
    private lateinit var context: Context
    private lateinit var res: Resources

    private val bitmaps = intArrayOf(
            R.drawable.android,
            R.drawable.dummy_image,
            R.drawable.pond
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        res = context.resources
    }

    @Test
    fun decodeBounds() {
        bitmaps.forEach { id ->
            val loader = BitmapLoader.fromResource(res, id)
            val width = loader.width
            val height = loader.height

            val bitmap = loader.loadBitmap()
            assertEquals(width, bitmap.width)
            assertEquals(height, bitmap.height)
        }
    }

    @Test
    fun decodeFull() {
        bitmaps.forEach { id ->
            val bitmap1 = BitmapLoader.fromResource(res, id).loadBitmap()
            val bitmap2 = BitmapFactory.decodeResource(res, id)
            assertTrue(bitmap1.sameAs(bitmap2))
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
        bitmaps.forEach { id ->
            val resName = res.getResourceName(id)
            testDimensions.forEach { dimension ->
                val bitmap1 = BitmapLoader.fromResource(res, id)
                        .scaleTo(dimension.x, dimension.y)
                        .loadBitmap()
                val bitmap2 = EagerBitmapLoader.fromResources(res, id)
                        .scaleTo(dimension.x, dimension.y)
                        .loadBitmap()
                if (!bitmap1.sameAs(bitmap2)) {
                    System.out.println("$bitmap1, $bitmap2")
                }
                assertTrue("scaleTo(${dimension.x}, ${dimension.y}) for $resName",
                        bitmap1.sameAs(bitmap2))
            }
        }
    }

    @Test
    fun scaleBy() {
        // Some generated random values
        val testScales = arrayOf(
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
        bitmaps.forEach { id ->
            val resName = res.getResourceName(id)
            testScales.forEach { dimension ->
                val bitmap1 = BitmapLoader.fromResource(res, id)
                        .scaleBy(dimension.x, dimension.y)
                        .loadBitmap()
                val bitmap2 = EagerBitmapLoader.fromResources(res, id)
                        .scaleBy(dimension.x, dimension.y)
                        .loadBitmap()
                assertTrue("scaleBy(${dimension.x}, ${dimension.y}) for $resName",
                        bitmap1.sameAs(bitmap2))
            }
        }
    }
}