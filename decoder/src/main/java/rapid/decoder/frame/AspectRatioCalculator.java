package rapid.decoder.frame;

public class AspectRatioCalculator {
	@SuppressWarnings("UnusedDeclaration")
    public static int getHeight(int width, int height, int targetWidth) {
		final double ratio = (double) height / width;
		return (int) Math.round(ratio * targetWidth);
	}
	
	@SuppressWarnings("UnusedDeclaration")
    public static int getWidth(int width, int height, int targetHeight) {
		final double ratio = (double) width / height;
		return (int) Math.round(ratio * targetHeight);
	}
	
	public static float getHeight(float width, float height, float targetWidth) {
		return (height / width) * targetWidth;
	}
	
	public static float getWidth(float width, float height, float targetHeight) {
		return (width / height) * targetHeight;
	}
}
