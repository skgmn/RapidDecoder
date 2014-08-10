package rapid.decoder;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

class LazyInputStream extends InputStream {
	private StreamOpener mOpener;
	protected InputStream mIn;
	
	public LazyInputStream(StreamOpener opener) {
		mOpener = opener;
	}

	public StreamOpener getStreamOpener() {
		return mOpener;
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
	public int read(@NonNull byte[] buffer) throws IOException {
		return getStream().read(buffer);
	}
	
	@Override
	public int read(@NonNull byte[] buffer, int byteOffset, int byteCount)
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

	protected InputStream getStream() {
		if (mIn == null) {
			mIn = mOpener.openInputStream();
			mOpener = null;
		}
		return mIn;
	}
}
