package agu.bitmap.async;

import android.graphics.Bitmap;

public interface AsyncBitmapCallback {
	void onBitmapLoaded(Bitmap bitmap);
	void onBitmapCancelled();
}
