package agu.bitmap;

class IdenticalResampler extends Resampler {
	@Override
	public int getSampleSize() {
		return 1;
	}

	@Override
	public int[] resample(int[] pixels, int offset, int count) {
		return pixels;
	}

	@Override
	public int[] finish() {
		return null;
	}
}
