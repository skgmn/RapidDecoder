package rapid.decoder.cache;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;

import rapid.decoder.cache.DiskLruCacheEngine.Editor;

@SuppressWarnings("UnusedDeclaration")
public class TransactionOutputStream extends OutputStream {
	private Editor mEditor;
	private OutputStream mOut;
	private DiskLruCache mCache;
	
	public TransactionOutputStream(DiskLruCache cache, Editor editor, OutputStream out) {
		mCache = cache;
		mEditor = editor;
		mOut = out;
	}
	
	@Override
	public void close() throws IOException {
		mOut.close();
		mEditor.commit();
		mCache.flush();
	}
	
	public void rollback() throws IOException {
		mOut.close();
		mEditor.abort();
		mCache.flush();
	}
	
	@Override
	public void flush() throws IOException {
		mOut.flush();
	}
	
	@Override
	public void write(@NonNull byte[] buffer, int offset, int count)
			throws IOException {
		
		mOut.write(buffer, offset, count);
	}
	
	@Override
	public void write(int oneByte) throws IOException {
		mOut.write(oneByte);
	}
	
	@Override
	public void write(@NonNull byte[] buffer) throws IOException {
		mOut.write(buffer);
	}
}
