package agu.bitmap;

abstract class Resampler {
	protected boolean filter;
	
	public abstract int[] resample(int[] pixels, int offset, int length);
	public abstract int[] finish();
	public abstract int getSampleSize();
	
	public void setUseFilter(boolean filter) {
		this.filter = filter;
	}
}
