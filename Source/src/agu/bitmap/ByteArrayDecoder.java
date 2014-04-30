package agu.bitmap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class ByteArrayDecoder extends BitmapDecoder {
	private byte[] data;
	private int offset;
	private int length;
	
	public ByteArrayDecoder(byte[] data, int offset, int length) {
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	@SuppressLint("NewApi")
	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeByteArray(data, offset, length, opts);
	}
	
	@Override
	protected InputStream getInputStream() {
		return new ByteArrayInputStream(data, offset, length);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(data, offset, length, false);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected BitmapDecoder clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}
}
