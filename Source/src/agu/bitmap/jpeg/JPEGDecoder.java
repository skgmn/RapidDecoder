package agu.bitmap.jpeg;

import java.io.InputStream;

public class JpegDecoder {
	static {
		System.loadLibrary("jpgd");
		init();
	}
	
	private static native void init();
	private static native long createNativeDecoder(InputStream in);
	private static native void destroyNativeDecoder(long decoder);
	private static native boolean nativeBegin(long decoder);
	private static native int nativeGetBytesPerPixel(long decoder);
	private static native int nativeGetWidth(long decoder);
	private static native int nativeGetHeight(long decoder);
	private static native int nativeDecode(long decoder, int[] outPixels);
	private static native int nativeSkipLine(long decoder);
	private static native int nativeSliceColumn(long decoder, int offset, int length);
	
	private long decoder;
	private boolean eof = false;
	
	public JpegDecoder(InputStream in) {
		decoder = createNativeDecoder(in);
	}
	
	public void close() {
		if (decoder == 0) return;
		
		destroyNativeDecoder(decoder);
		decoder = 0;
	}
	
	public boolean begin() {
		if (decoder == 0) {
			throw new IllegalStateException();
		}
		
		return nativeBegin(decoder);
	}
	
	public int getWidth() {
		if (decoder == 0) {
			throw new IllegalStateException();
		}

		return nativeGetWidth(decoder);
	}
	
	public int getHeight() {
		if (decoder == 0) {
			throw new IllegalStateException();
		}

		return nativeGetHeight(decoder);
	}
	
	public boolean isEndOfStream() {
		return eof;
	}
	
	public void readLine(int[] buffer) {
		if (decoder == 0) {
			throw new IllegalStateException();
		}

		final int bytesRead = nativeDecode(decoder, buffer);
		if (bytesRead < 0)
		{
			throw new UnknownError();
		}

		eof = (bytesRead == 0);
	}
	
	public int getBytesPerPixel() {
		if (decoder == 0) {
			throw new IllegalStateException();
		}
		
		return nativeGetBytesPerPixel(decoder);
	}
	
	public void skipLine() {
		if (decoder == 0) {
			throw new IllegalStateException();
		}

		final int bytesRead = nativeSkipLine(decoder);
		switch (bytesRead) {
		case -1: throw new UnknownError();
		}

		eof = (bytesRead == 0);
	}
	
	public void cancelSlice() {
		sliceColumn(0, -1);
	}
	
	public void sliceColumn(int offset, int length) {
		if (decoder == 0) {
			throw new IllegalStateException();
		}

		nativeSliceColumn(decoder, offset, length);
	}
	
	@Override
	@FindBugsSuppressWarnings("FI_EMPTY")
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
