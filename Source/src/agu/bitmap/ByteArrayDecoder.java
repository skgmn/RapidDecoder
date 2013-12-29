package agu.bitmap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

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
	protected InputStream openInputStream() {
		return new ByteArrayInputStream(data, offset, length);
	}
}
