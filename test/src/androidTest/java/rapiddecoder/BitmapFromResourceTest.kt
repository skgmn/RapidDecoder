package rapiddecoder

import android.content.Context
import android.graphics.BitmapFactory
import android.support.test.InstrumentationRegistry
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rapiddecoder.test.R

class BitmapFromResourceTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun load() {
        val res = context.resources

        val bitmap1 = BitmapLoader.fromResource(res, R.drawable.dummy_image).loadBitmap()
        val bitmap2 = BitmapFactory.decodeResource(res, R.drawable.dummy_image)
        assertTrue(bitmap1.sameAs(bitmap2))
    }
}