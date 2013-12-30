package agu.bitmap;

class IdentityResampler extends Resampler {
	private int[] resultCache;
	
	@Override
	public int getSampleSize() {
		return 1;
	}

	@Override
	public int[] resample(int[] pixels, int offset, int count) {
		if (offset == 0 && count == pixels.length) {
			return pixels;
		} else {
			if (resultCache == null || resultCache.length != count) {
				resultCache = new int [count];
			}
			System.arraycopy(pixels, offset, resultCache, 0, count);
			return resultCache;
		}
	}

	@Override
	public int[] finish() {
		return null;
	}
}
