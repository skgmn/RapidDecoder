package agu.bitmap;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;

class PendingStreamDecoder extends BitmapDecoder {
	private StreamOpener mOpener;
	private BitmapDecoder mDecoder;
	
	public PendingStreamDecoder(StreamOpener opener) {
		mOpener = opener;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return getDecoder().decode(opts);
	}

	@Override
	protected InputStream getInputStream() {
		return getDecoder().getInputStream();
	}

	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		return getDecoder().createBitmapRegionDecoder();
	}
	
	@Override
	protected void onDecodingStarted(boolean builtInDecoder) {
		getDecoder().onDecodingStarted(builtInDecoder);
	}
	
	@Override
	protected void onDecodingFinished() {
		getDecoder().onDecodingFinished();
	}

	private synchronized BitmapDecoder getDecoder() {
		if (mDecoder == null) {
			mDecoder = new StreamDecoder(mOpener.openInputStream());
		}
		return mDecoder;
	}
}
