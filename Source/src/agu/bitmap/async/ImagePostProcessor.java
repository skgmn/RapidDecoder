package agu.bitmap.async;

import android.graphics.Bitmap;

public interface ImagePostProcessor {
	Bitmap postProcess(Bitmap bitmap);
}
