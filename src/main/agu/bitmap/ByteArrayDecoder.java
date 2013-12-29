package agu.bitmap;

import java.io.ByteArrayInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;

class ByteArrayDecoder extends BitmapDecoder {
	private byte[] data;
	private int offset;
	private int length;
	
	public ByteArrayDecoder(byte[] data, int offset, int length) {
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeByteArray(data, offset, length, opts);
	}

	@Override
	protected Bitmap decodePartial(Options opts, Rect region) {
		final ByteArrayInputStream in = new ByteArrayInputStream(data, offset, length);
		return aguDecode(in, opts, region);
	}
}
