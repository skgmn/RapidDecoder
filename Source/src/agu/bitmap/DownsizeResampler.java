package agu.bitmap;

import android.graphics.Color;

public class DownsizeResampler extends Resampler {
	private int[] a;
	private int[] r;
	private int[] g;
	private int[] b;
	private int[] output;
	
	private int sampleSize;
	private int shiftCount;
	private int shiftCount2;
	private int width;
	private int rows;
	
	public DownsizeResampler(int sampledWidth, int sampleSize, boolean filter) {
		this.sampleSize = sampleSize;
		this.shiftCount = Integer.SIZE - Integer.numberOfLeadingZeros(sampleSize) - 1;
		this.shiftCount2 = shiftCount * 2;
		this.width = sampledWidth;
		this.rows = 0;
		this.filter = filter;
		
		this.a = new int [width];
		this.r = new int [width];
		this.g = new int [width];
		this.b = new int [width];
		this.output = new int [width];
	}
	
	@Override
	public int[] resample(int[] pixels, int offset, int count) {
		if (filter) {
			for (int i = 0; i < count; ++i) {
				final int col = i >>> shiftCount;
				if (col >= width) break;
				
				final int pixel = pixels[offset + i];
				 
				a[col] += ((pixel & 0xff000000) >> 24);
				r[col] += ((pixel & 0x00ff0000) >> 16);
				g[col] += ((pixel & 0x0000ff00) >> 8);
				b[col] += (pixel & 0x000000ff);
			}
	
			if (++rows == sampleSize) {
				calculateAverage();
				rows = 0;
				
				return output;
			} else {
				return null;
			}
		} else {
			if (rows == 0) {
				for (int i = 0; i < count; i += sampleSize) {
					final int col = i >>> shiftCount;
					if (col >= width) break;

					output[col] = pixels[offset + i];
				}
			}

			if (++rows == sampleSize) {
				rows = 0;
				return output;
			} else {
				return null;
			}
		}
	}
	
	private void calculateAverage() {
		for (int i = 0; i < width; ++i) {
			output[i] = Color.argb(a[i] >>> shiftCount2,
					r[i] >>> shiftCount2,
					g[i] >>> shiftCount2,
					b[i] >>> shiftCount2);
			a[i] = r[i] = g[i] = b[i] = 0;
		}
	}

	@Override
	public int getSampleSize() {
		return sampleSize;
	}

	@Override
	public int[] finish() {
		if (rows == 0) {
			return null;
		} else {
			if (filter) calculateAverage();
			return output;
		}
	}
}
