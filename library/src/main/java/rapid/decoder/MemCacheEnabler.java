package rapid.decoder;

class MemCacheEnabler<T> {
	private T mExtraKey;
	private BitmapDecoder mDecoder;
	
	public MemCacheEnabler(T extraKey) {
		mExtraKey = extraKey;
	}
	
	void setBitmapDecoder(BitmapDecoder decoder) {
		mDecoder = decoder;
	}

	@Override
	public int hashCode() {
		return mDecoder.hashCode() ^ mExtraKey.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof MemCacheEnabler<?>)) return false;
		
		final MemCacheEnabler<?> e = (MemCacheEnabler<?>) o;
		return mDecoder.equals(e.mDecoder) && mExtraKey.equals(e.mExtraKey);
	}
	
	public MemCacheEnabler<T> clone() {
		return new MemCacheEnabler<T>(mExtraKey);
	}
}
