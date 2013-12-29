package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
	protected Bitmap decodeImpl() {
		return BitmapFactory.decodeByteArray(data, offset, length, opts);
	}
}
