package agu.bitmap.async;

import agu.bitmap.BitmapDecoderDelegate;

public interface AsyncBitmapLoaderJob {
	void setDelegate(BitmapDecoderDelegate d);
	void start();
	boolean isValid();
}
