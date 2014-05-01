package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;

class LazyInputStream extends InputStream {
	private StreamOpener mOpener;
	private InputStream mIn;
	
	public LazyInputStream(StreamOpener opener) {
		mOpener = opener;
	}

	@Override
	public int read() throws IOException {
		return getStream().read();
	}
	
	@Override
	public int available() throws IOException {
		return getStream().available();
	}
	
	@Override
	public void close() throws IOException {
		if (mIn != null) {
			mIn.close();
		}
	}
	
	@Override
	public void mark(int readlimit) {
		getStream().mark(readlimit);
	}
	
	@Override
	public boolean markSupported() {
		return getStream().markSupported();
	}
	
	@Override
	public int read(byte[] buffer) throws IOException {
		return getStream().read(buffer);
	}
	
	@Override
	public int read(byte[] buffer, int byteOffset, int byteCount)
			throws IOException {
		
		return getStream().read(buffer, byteOffset, byteCount);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		getStream().reset();
	}
	
	@Override
	public long skip(long byteCount) throws IOException {
		return getStream().skip(byteCount);
	}

	private InputStream getStream() {
		if (mIn == null) {
			mIn = mOpener.openInputStream();
			mOpener = null;
		}
		return mIn;
	}
}
