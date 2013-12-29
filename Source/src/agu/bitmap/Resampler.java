package agu.bitmap;

abstract class Resampler {
	public abstract int[] resample(int[] pixels, int offset, int length);
	public abstract int[] finish();
	public abstract int getSampleSize();
}
