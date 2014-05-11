package agu.test;

import agu.bitmap.BitmapDecoder;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.test.AndroidTestCase;

public class BasicUnitTest extends AndroidTestCase {
	private Resources res;
	
	@SuppressWarnings("unused")
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
	
	public void testDecoding() {
		decodingTest(R.drawable.android);
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

		bitmap2 = BitmapDecoder.from(res, id).region(10, 10, 100, 90).scaleBy(0.7f, 0.8f).decode();
		assertEquals(63, bitmap2.getWidth());
		assertEquals(64, bitmap2.getHeight());
		bitmap2.recycle();

		bitmap2 = BitmapDecoder.from(res, id).region(10, 10, 100, 90).useBuiltInDecoder().decode();
		assertEquals(90, bitmap2.getWidth());
		assertEquals(80, bitmap2.getHeight());
		bitmap2.recycle();

		bitmap2 = BitmapDecoder.from(res, id).region(10, 10, 100, 90).scaleBy(0.7f, 0.8f).useBuiltInDecoder().decode();
		assertEquals(63, bitmap2.getWidth());
		assertEquals(64, bitmap2.getHeight());
		bitmap2.recycle();
		
		bitmap2 = BitmapDecoder.from(res, id).scale(210, 220).decode();
		assertEquals(210, bitmap2.getWidth());
		assertEquals(220, bitmap2.getHeight());
		bitmap2.recycle();
		
		final float SCALE_FACTOR = 0.5f;

		bitmap2 = BitmapDecoder.from(res, id).scaleBy(SCALE_FACTOR).decode();
		assertEquals((int) Math.ceil(bitmap.getWidth() * SCALE_FACTOR), bitmap2.getWidth());
		assertEquals((int) Math.ceil(bitmap.getHeight() * SCALE_FACTOR), bitmap2.getHeight());
		bitmap2.recycle();
		
		bitmap2 = BitmapDecoder.from(res, id).scaleBy(SCALE_FACTOR).useBuiltInDecoder().decode();
		assertEquals((int) Math.ceil(bitmap.getWidth() * SCALE_FACTOR), bitmap2.getWidth());
		assertEquals((int) Math.ceil(bitmap.getHeight() * SCALE_FACTOR), bitmap2.getHeight());
		bitmap2.recycle();
	}
}
