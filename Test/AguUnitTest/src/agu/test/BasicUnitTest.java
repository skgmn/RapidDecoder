package agu.test;

import agu.bitmap.BitmapDecoder;
import agu.scaling.AspectRatioCalculator;
import agu.scaling.FrameAlignment;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.test.AndroidTestCase;

public class BasicUnitTest extends AndroidTestCase {
	private Resources res;
	
	private static void assertEquals(Rect rect, int left, int top, int right, int bottom) {
		assertEquals(left, rect.left);
		assertEquals(top, rect.top);
		assertEquals(right, rect.right);
		assertEquals(bottom, rect.bottom);
	}
	
	@Override
	protected void setUp() throws Exception {
		res = getContext().getResources();
	}
	
	public void testCalculator() {
		final Rect rect = new Rect();
		
		AspectRatioCalculator.frame(100, 100, 200, 200, null, true, rect);
		assertEquals(rect, 0, 0, 200, 200);

		AspectRatioCalculator.frame(300, 300, 200, 200, null, true, rect);
		assertEquals(rect, 0, 0, 200, 200);
		
		AspectRatioCalculator.frame(50, 100, 200, 200, FrameAlignment.LEFT_OR_TOP, true, rect);
		assertEquals(rect, 0, 0, 100, 200);
		AspectRatioCalculator.frame(50, 100, 200, 200, FrameAlignment.CENTER, true, rect);
		assertEquals(rect, 50, 0, 150, 200);
		AspectRatioCalculator.frame(50, 100, 200, 200, FrameAlignment.RIGHT_OR_BOTTOM, true, rect);
		assertEquals(rect, 100, 0, 200, 200);
		AspectRatioCalculator.frame(50, 100, 200, 200, FrameAlignment.LEFT_OR_TOP, false, rect);
		assertEquals(rect, 0, 0, 200, 400);
		AspectRatioCalculator.frame(50, 100, 200, 200, FrameAlignment.CENTER, false, rect);
		assertEquals(rect, 0, -100, 200, 300);
		AspectRatioCalculator.frame(50, 100, 200, 200, FrameAlignment.RIGHT_OR_BOTTOM, false, rect);
		assertEquals(rect, 0, -200, 200, 200);
		
		AspectRatioCalculator.frame(100, 50, 200, 200, FrameAlignment.LEFT_OR_TOP, true, rect);
		assertEquals(rect, 0, 0, 200, 100);
		AspectRatioCalculator.frame(100, 50, 200, 200, FrameAlignment.CENTER, true, rect);
		assertEquals(rect, 0, 50, 200, 150);
		AspectRatioCalculator.frame(100, 50, 200, 200, FrameAlignment.RIGHT_OR_BOTTOM, true, rect);
		assertEquals(rect, 0, 100, 200, 200);
		AspectRatioCalculator.frame(100, 50, 200, 200, FrameAlignment.LEFT_OR_TOP, false, rect);
		assertEquals(rect, 0, 0, 400, 200);
		AspectRatioCalculator.frame(100, 50, 200, 200, FrameAlignment.CENTER, false, rect);
		assertEquals(rect, -100, 0, 300, 200);
		AspectRatioCalculator.frame(100, 50, 200, 200, FrameAlignment.RIGHT_OR_BOTTOM, false, rect);
		assertEquals(rect, -200, 0, 200, 200);
	}
	
	public void testDecoding() {
		decodingTest(R.drawable.pond);
	}
	
	private void decodingTest(int id) {
		Bitmap bitmap = BitmapFactory.decodeResource(res, id);
		Bitmap bitmap2;
		
		bitmap2 = BitmapDecoder.from(res, id).decode();
		assertEquals(bitmap.getWidth(), bitmap2.getWidth());
		assertEquals(bitmap.getHeight(), bitmap2.getHeight());
		bitmap2.recycle();
		
		bitmap2 = BitmapDecoder.from(res, id).useBuiltInDecoder().decode();
		assertEquals(bitmap.getWidth(), bitmap2.getWidth());
		assertEquals(bitmap.getHeight(), bitmap2.getHeight());
		bitmap2.recycle();
		
		bitmap2 = BitmapDecoder.from(res, id).region(10, 10, 100, 90).decode();
		assertEquals(90, bitmap2.getWidth());
		assertEquals(80, bitmap2.getHeight());
		bitmap2.recycle();

		bitmap2 = BitmapDecoder.from(res, id).scale(210, 220).decode();
		assertEquals(210, bitmap2.getWidth());
		assertEquals(220, bitmap2.getHeight());
		bitmap2.recycle();

		bitmap2 = BitmapDecoder.from(res, id).scaleBy(0.5).decode();
		assertEquals(bitmap.getWidth() / 2, bitmap2.getWidth());
		assertEquals(bitmap.getHeight() / 2, bitmap2.getHeight());
		bitmap2.recycle();
	}
}
