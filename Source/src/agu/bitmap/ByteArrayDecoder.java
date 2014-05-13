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

class ByteArrayDecoder extends ExternalBitmapDecoder {
	private byte[] data;
	private int offset;
	private int length;
	
	public ByteArrayDecoder(byte[] data, int offset, int length) {
		if (data == null) {
			throw new NullPointerException();
		}
		
		this.data = data;
		this.offset = offset;
		this.length = length;
	}
	
	protected ByteArrayDecoder(ByteArrayDecoder other) {
		super(other);
		data = other.data;
		offset = other.offset;
		length = other.length;
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
	public ExternalBitmapDecoder clone() {
		return new ByteArrayDecoder(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ data.hashCode() ^ offset ^ length;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!super.equals(o) || !(o instanceof ByteArrayDecoder)) return false;
		
		final ByteArrayDecoder d = (ByteArrayDecoder) o;
		return data.equals(d.data) &&
				offset == d.offset &&
				length == d.length;
	}
}
