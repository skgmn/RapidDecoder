package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class StreamDecoder extends BitmapDecoder {
	private TwoPhaseBufferedInputStream mIn;
	
	public StreamDecoder(InputStream is) {
		if (is instanceof TwoPhaseBufferedInputStream &&
				!((TwoPhaseBufferedInputStream) is).isSecondPhase()) {
					
			mIn = (TwoPhaseBufferedInputStream) is;
		} else {
			mIn = new TwoPhaseBufferedInputStream(is);
		}
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeStream(mIn, null, opts);
	}

	@Override
	protected InputStream openInputStream() {
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
}
