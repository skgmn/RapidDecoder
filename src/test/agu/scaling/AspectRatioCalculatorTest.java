package agu.scaling;

import android.graphics.Rect;
import android.test.AndroidTestCase;

public class AspectRatioCalculatorTest extends AndroidTestCase {
	private static void assertEquals(Rect rect, int left, int top, int right, int bottom) {
		assertEquals(left, rect.left);
		assertEquals(top, rect.top);
		assertEquals(right, rect.right);
		assertEquals(bottom, rect.bottom);
	}
	
	public void testScale() {
		final Rect rect = new Rect();
		
		AspectRatioCalculator.scale(100, 100, 200, 200, null, true, rect);
		assertEquals(rect, 0, 0, 200, 200);

		AspectRatioCalculator.scale(300, 300, 200, 200, null, true, rect);
		assertEquals(rect, 0, 0, 200, 200);
		
		AspectRatioCalculator.scale(50, 100, 200, 200, ScaleAlignment.LEFT_OR_TOP, false, rect);
		assertEquals(rect, 0, 0, 100, 200);
		AspectRatioCalculator.scale(50, 100, 200, 200, ScaleAlignment.CENTER, false, rect);
		assertEquals(rect, 50, 0, 150, 200);
		AspectRatioCalculator.scale(50, 100, 200, 200, ScaleAlignment.RIGHT_OR_BOTTOM, false, rect);
		assertEquals(rect, 100, 0, 200, 200);
		AspectRatioCalculator.scale(50, 100, 200, 200, ScaleAlignment.LEFT_OR_TOP, true, rect);
		assertEquals(rect, 0, 0, 200, 400);
		AspectRatioCalculator.scale(50, 100, 200, 200, ScaleAlignment.CENTER, true, rect);
		assertEquals(rect, 0, -100, 200, 300);
		AspectRatioCalculator.scale(50, 100, 200, 200, ScaleAlignment.RIGHT_OR_BOTTOM, true, rect);
		assertEquals(rect, 0, -200, 200, 200);
		
		AspectRatioCalculator.scale(100, 50, 200, 200, ScaleAlignment.LEFT_OR_TOP, false, rect);
		assertEquals(rect, 0, 0, 200, 100);
		AspectRatioCalculator.scale(100, 50, 200, 200, ScaleAlignment.CENTER, false, rect);
		assertEquals(rect, 0, 50, 200, 150);
		AspectRatioCalculator.scale(100, 50, 200, 200, ScaleAlignment.RIGHT_OR_BOTTOM, false, rect);
		assertEquals(rect, 0, 100, 200, 200);
		AspectRatioCalculator.scale(100, 50, 200, 200, ScaleAlignment.LEFT_OR_TOP, true, rect);
		assertEquals(rect, 0, 0, 400, 200);
		AspectRatioCalculator.scale(100, 50, 200, 200, ScaleAlignment.CENTER, true, rect);
		assertEquals(rect, -100, 0, 300, 200);
		AspectRatioCalculator.scale(100, 50, 200, 200, ScaleAlignment.RIGHT_OR_BOTTOM, true, rect);
		assertEquals(rect, -200, 0, 200, 200);
	}
}
