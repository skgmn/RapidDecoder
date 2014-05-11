package agu.scaling;

import android.graphics.Rect;


public class AspectRatioCalculator {
	/**
	 * Scale the given size keeping the aspect ratio.
	 * @param width Width to scale in/out.
	 * @param height Height to scale in/out.
	 * @param frameWidth Frame width.
	 * @param frameHeight Frame height.
	 * @param align Direction to align when there are remaining spaces after scaling.
	 * @param fitIn True if the scaled size should not overflow the frame size. False otherwise.
	 * @param out A Rect instance to retrieve the scaled size and adjusted position. 
	 */
	public static void frame(int width, int height, int frameWidth, int frameHeight,
			FrameAlignment align, boolean fitIn, Rect out) {

		double ratio = (double) height / width;

		final int height2 = (int) (frameWidth * ratio);
		if (height2 == frameHeight) {
			out.set(0, 0, frameWidth, frameHeight);
			return;
		}

		final boolean fitWidth = ((height2 > frameHeight && !fitIn) || (height2 < frameHeight && fitIn));
		if (fitWidth) {
			out.left = 0;
			out.right = frameWidth;

			if (align == FrameAlignment.LEFT_OR_TOP) {
				out.top = 0;
				out.bottom = height2;
			} else if (align == FrameAlignment.RIGHT_OR_BOTTOM) {
				out.top = frameHeight - height2;
				out.bottom = frameHeight;
			} else {
				out.top = (frameHeight - height2) / 2;
				out.bottom = out.top + height2;
			}
		} else {
			ratio = (double) width / height;
			int width2 = (int) (frameHeight * ratio);

			out.top = 0;
			out.bottom = frameHeight;

			if (align == FrameAlignment.LEFT_OR_TOP) {
				out.left = 0;
				out.right = width2;
			} else if (align == FrameAlignment.RIGHT_OR_BOTTOM) {
				out.left = frameWidth - width2;
				out.right = frameWidth;
			} else {
				out.left = (frameWidth - width2) / 2;
				out.right = out.left + width2;
			}
		}
	}
	
	public static int fitWidth(int width, int height, int targetWidth) {
		final double ratio = (double) height / width;
		return (int) (ratio * targetWidth);
	}
	
	public static int fitHeight(int width, int height, int targetHeight) {
		final double ratio = (double) width / height;
		return (int) (ratio * targetHeight);
	}
}
