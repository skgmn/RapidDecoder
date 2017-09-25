package rapiddecoder

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.test.InstrumentationRegistry
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rapiddecoder.test.R

class BitmapFromResourceTest {
    private lateinit var context: Context
    private lateinit var res: Resources

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        res = context.resources
    }

    @Test
    fun simpleLoad() {
        val dummyImageLoader = BitmapLoader.fromResource(res, R.drawable.dummy_image)
        val width = dummyImageLoader.width
        val height = dummyImageLoader.height

        val bitmap1 = dummyImageLoader.loadBitmap()
        val bitmap2 = BitmapFactory.decodeResource(res, R.drawable.dummy_image)
        assertTrue(bitmap1.sameAs(bitmap2))

        assertEquals(bitmap1.width, width)
        assertEquals(bitmap1.height, height)
    }

    @Test
    fun scaleTo() {
        val scaleTargetWidth = 100
        val scaleTargetHeight = 110

        val source = BitmapLoader.fromResource(res, R.drawable.dummy_image)
        var sourceWidth = source.sourceWidth.toFloat()
        var sourceHeight = source.sourceHeight.toFloat()

        val dummyImageLoader = source.scaleTo(scaleTargetWidth, scaleTargetHeight)
        val width = dummyImageLoader.width
        val height = dummyImageLoader.height

        val bitmap1 = dummyImageLoader.loadBitmap()

        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inSampleSize = 1
        while (sourceWidth >= scaleTargetWidth * 2 && sourceHeight >= scaleTargetHeight * 2) {
            opts.inSampleSize *= 2
            sourceWidth /= 2f
            sourceHeight /= 2f
        }
        val bitmap2 = BitmapFactory.decodeResource(res, R.drawable.dummy_image, opts)
        val bitmap3 = Bitmap.createScaledBitmap(bitmap2, scaleTargetWidth, scaleTargetHeight, true)

        assertTrue(bitmap1.sameAs(bitmap3))

        assertEquals(scaleTargetWidth, width)
        assertEquals(scaleTargetHeight, height)
        assertEquals(scaleTargetWidth, bitmap1.width)
        assertEquals(scaleTargetHeight, bitmap1.height)
    }
}