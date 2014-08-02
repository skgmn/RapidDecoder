package rapid.decoder;

import android.graphics.Bitmap;

public interface BitmapDecoderDelegate {
	Bitmap decode(BitmapDecoder decoder);
}
