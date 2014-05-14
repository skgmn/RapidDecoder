package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class StreamDecoder extends ExternalBitmapDecoder {
	private TwoPhaseBufferedInputStream mIn;
	
	public StreamDecoder(InputStream is) {
		if (is instanceof TwoPhaseBufferedInputStream &&
				!((TwoPhaseBufferedInputStream) is).isSecondPhase()) {
					
			mIn = (TwoPhaseBufferedInputStream) is;
		} else {
			mIn = new TwoPhaseBufferedInputStream(is);
		}
	}
	
	protected StreamDecoder(StreamDecoder other) {
		super(other);
		
		InputStream is = mIn.getStream();
		if (is instanceof LazyInputStream) {
			is = new LazyInputStream(((LazyInputStream) is).getStreamOpener());
		}
		mIn = new TwoPhaseBufferedInputStream(is);
	}
	
	void setCacheOutputStream(OutputStream out) {
		mIn.setCacheOutputStream(out);
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeStream(mIn, null, opts);
	}

	@Override
	protected InputStream getInputStream() {
		return mIn;
	}
	
	@Override
	protected void onDecodingStarted(boolean builtInDecoder) {
		if (!builtInDecoder) {
			mIn.startSecondPhase();
		}
		mIn.seekToBeginning();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(mIn, false);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public ExternalBitmapDecoder clone() {
		return new StreamDecoder(this);
	}
	
	@Override
	protected boolean isMemCacheSupported() {
		return false;
	}
}
