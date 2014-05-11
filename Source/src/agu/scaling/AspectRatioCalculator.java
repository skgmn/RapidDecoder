package agu.scaling;


public class AspectRatioCalculator {
	public static int fitWidth(int width, int height, int targetWidth) {
		final double ratio = (double) height / width;
		return (int) (ratio * targetWidth);
	}
	
	public static int fitHeight(int width, int height, int targetHeight) {
		final double ratio = (double) width / height;
		return (int) (ratio * targetHeight);
	}
}
