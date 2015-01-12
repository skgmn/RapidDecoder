package rapid.decoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

import rapid.decoder.cache.TransactionOutputStream;

class StreamBitmapLoader extends BitmapLoader {
	private TwiceReadableInputStream mIn;
	
	public StreamBitmapLoader(InputStream is) {
		if (is instanceof TwiceReadableInputStream &&
				!((TwiceReadableInputStream) is).isSecondReading()) {
			mIn = (TwiceReadableInputStream) is;
		} else {
			mIn = new TwiceReadableInputStream(is);
		}
	}
	
	protected StreamBitmapLoader(StreamBitmapLoader other) {
		super(other);

		InputStream is = other.mIn.getStream();
		if (is instanceof LazyInputStream) {
			is = new LazyInputStream(((LazyInputStream) is).getStreamOpener());
		}
		mIn = new TwiceReadableInputStream(is);
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			mIn.close();
		} finally {
			super.finalize();
		}
	}
	
	void setCacheOutputStream(TransactionOutputStream out) {
		mIn.setCacheOutputStream(out);
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		try {
			return BitmapFactory.decodeStream(mIn, null, opts);
		} catch (Throwable ignored) {
			mIn.setTransactionSucceeded(false);
            return null;
		}
	}

    @Override
    public void cancel() {
        super.cancel();
        try {
            mIn.close();
        } catch (IOException ignored) {
        }
    }

    @Override
	protected InputStream openInputStream() {
		return mIn;
	}
	
	@Override
	protected void onDecodingStarted(boolean builtInDecoder) {
		if (!builtInDecoder) {
			mIn.startSecondRead();
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

	@NonNull
    @Override
	public BitmapLoader fork() {
		return new StreamBitmapLoader(this);
	}

    @Override
    public BitmapLoader reset() {
        throw new UnsupportedOperationException();
    }
}
