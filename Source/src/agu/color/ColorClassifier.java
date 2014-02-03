package agu.color;

import android.graphics.Color;

public class ColorClassifier {
	private static final float[] colors = new float[] {
		0.0f, 1.0f, 1.0f,
		16.904762f, 1.0f, 0.9882353f,
		24.0f, 1.0f, 1.0f,
		48.0f, 1.0f, 1.0f,
		60.0f, 1.0f, 1.0f,
		79.99999f, 0.6f, 1.0f,
		83.414635f, 1.0f, 0.8039216f,
		102.29508f, 1.0f, 0.7176471f,
		120.0f, 1.0f, 0.6f,
		180.0f, 1.0f, 0.5019608f,
		180.0f, 1.0f, 0.4f,
		181.98676f, 1.0f, 0.5921569f,
		237.2034f, 0.9752066f, 0.9490196f,
		217.4026f, 1.0f, 0.6039216f,
		240.0f, 1.0f, 0.49411765f,
		270.0f, 1.0f, 0.6039216f,
		300.46875f, 1.0f, 0.5019608f,
		317.14285f, 1.0f, 0.46666667f,
		330.0f, 1.0f, 0.9098039f,
		336.0f, 1.0f, 1.0f
	};
	
	// From munsell color wheel.
	public static final int COLOR_CLASS_RED = 0;
	public static final int COLOR_CLASS_SCARLET = 1;
	public static final int COLOR_CLASS_ORANGE = 2;
	public static final int COLOR_CLASS_TANGERINE = 3;
	public static final int COLOR_CLASS_YELLOW = 4;
	public static final int COLOR_CLASS_BRIGHT_YELLOW_GREEN = 5;
	public static final int COLOR_CLASS_DARK_YELLOW_GREEN = 6;
	public static final int COLOR_CLASS_GRASS = 7;
	public static final int COLOR_CLASS_BRIGHT_GREEN = 8;
	public static final int COLOR_CLASS_DARK_GREEN = 9;
	public static final int COLOR_CLASS_BLUISH_GREEN = 10;
	public static final int COLOR_CLASS_SEA = 11;
	public static final int COLOR_CLASS_BLUE = 12;
	public static final int COLOR_CLASS_BRIGHT_NAVY = 13;
	public static final int COLOR_CLASS_DARK_NAVY = 14;
	public static final int COLOR_CLASS_BLUE_VIOLET = 15;
	public static final int COLOR_CLASS_VIOLET = 16;
	public static final int COLOR_CLASS_RED_VIOLET = 17;
	public static final int COLOR_CLASS_WINE = 18;
	public static final int COLOR_CLASS_CRIMSON = 19;
	public static final int COLOR_CLASS_ACHROMATIC = colors.length / 3;
	
	private static float getColorDistance(float hue1, float sat1, float val1,
			float hue2, float sat2, float val2) {
		
		final float hueDiff;
		if (hue1 > hue2) {
			hueDiff = Math.min(hue1 - hue2, hue2 - hue1 + 360);
		} else {
			hueDiff = Math.min(hue2 - hue1, hue1 - hue2 + 360);
		}
		
		final float satDiff = (sat1 - sat2) * 160;
		final float valDiff = (val1 - val2) * 160;
		
		return (float) Math.sqrt(hueDiff * hueDiff +
				satDiff * satDiff +
				valDiff * valDiff);
	}
	
	public static int classifyColor(int color, float[] hsv) {
		Color.colorToHSV(color, hsv);
		if (hsv[1] == 0) return COLOR_CLASS_ACHROMATIC;
		
		float minDistance = Float.POSITIVE_INFINITY;
		int minIndex = -1;
		
		for (int i = 0; i < colors.length; i += 3) {
			final float hue = colors[i];
			final float sat = colors[i + 1];
			final float val = colors[i + 2];
			
			final float distance = getColorDistance(hue, sat, val, hsv[0], hsv[1], hsv[2]);
			if (distance < minDistance) {
				minDistance = distance;
				minIndex = i / 3;
			}
		}
		
		return minIndex;
	}
	
	public static int classifyColor(int color) {
		return classifyColor(color, new float [3]);
	}
}
