package rapid.decoder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class ByteArrayLoader extends BitmapLoader {
	private byte[] data;
	private int offset;
	private int length;
	
	public ByteArrayLoader(byte[] data, int offset, int length) {
		if (data == null) {
			throw new NullPointerException();
		}
		
		this.data = data;
		this.offset = offset;
		this.length = length;
	}
	
	protected ByteArrayLoader(ByteArrayLoader other) {
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

	@NonNull
    @Override
	public BitmapLoader mutate() {
		return new ByteArrayLoader(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ data.hashCode() ^ offset ^ length;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!super.equals(o) || !(o instanceof ByteArrayLoader)) return false;
		
		final ByteArrayLoader d = (ByteArrayLoader) o;
		return data.equals(d.data) &&
				offset == d.offset &&
				length == d.length;
	}

	@Override
	public boolean isMemoryCacheSupported() {
		return false;
	}
}
