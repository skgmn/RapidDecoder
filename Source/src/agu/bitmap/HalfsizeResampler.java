package agu.bitmap;

import android.graphics.Color;

public class HalfsizeResampler extends Resampler {
	private int[] first;
	private int[] resultCache;
	private boolean hasFirst;
	
	private Resampler another;
	private int a, r, g, b;
	private int pixelCount;
	private int sampleSize = 0;
	
	public HalfsizeResampler(Resampler another) {
		this.another = another;
		this.hasFirst = false;
	}
	
	@Override
	public int[] resample(int[] pixels, int offset, int count) {
		if (another != null) {
			pixels = another.resample(pixels, offset, count);
			if (pixels == null) return null;
		}
		
		if (!hasFirst) {
			if (first == null || first.length != count) {
				first = new int [count];
			}
			System.arraycopy(pixels, offset, first, 0, count);
			
			hasFirst = true;
			return null;
		} else {
			// Don't check the length for performance.
			// assert first.length == count;
			
			final int[] result = downsize(pixels, offset);
			
			hasFirst = false;
			return result;
		}
	}
	
	private void addPixel(int[] pixels, int offset) {
		if (pixels == null || offset >= pixels.length) return;
		
		final int pixel = pixels[offset];
		
		a += Color.alpha(pixel);
		r += Color.red(pixel);
		g += Color.green(pixel);
		b += Color.blue(pixel);
		++pixelCount;
	}
	
	private int[] downsize(int[] second, int offset) {
		final int length = first.length / 2;
		if (resultCache == null || resultCache.length != length) {
			resultCache = new int [length];
		}
		
		for (int i = 0; i < resultCache.length; ++i) {
			final int index2 = i * 2;
			
			final int pixel;
			if (filter) {
				pixelCount = 0;
				a = r = g = b = 0;
				
				addPixel(first, index2);
				addPixel(first, index2 + 1);
				addPixel(second, offset + index2);
				addPixel(second, offset + index2 + 1);
				
				pixel = Color.argb(
						a / pixelCount,
						r / pixelCount,
						g / pixelCount,
						b / pixelCount);
			} else {
				pixel = first[index2];
			}
			
			resultCache[i] = pixel;
		}
		
		return resultCache;
	}

	@Override
	public int getSampleSize() {
		if (sampleSize == 0) {
			if (another == null) {
				sampleSize = 2;
			} else {
				sampleSize = 2 * another.getSampleSize();
			}
		}
		return sampleSize;
	}

	@Override
	public int[] finish() {
		if (!hasFirst) {
			return null;
		} else {
			final int[] result = downsize(null, 0);
			
			hasFirst = false;
			first = null;
			
			return result;
		}
	}
	
	@Override
	public void setUseFilter(boolean filter) {
		super.setUseFilter(filter);
		if (another != null) {
			another.setUseFilter(filter);
		}
	}
}
