package agu.bitmap.png.chunks;

import agu.bitmap.png.ImageInfo;

/**
 * IEND chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11IEND
 */
public class PngChunkIEND extends PngChunkSingle {
	public final static String ID = ChunkHelper.IEND;

	// http://www.w3.org/TR/PNG/#11IEND
	// this is a dummy placeholder
	public PngChunkIEND(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NA;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		// this is not used
	}

	@Override
	public PngChunkIEND cloneForWrite(ImageInfo imgInfo) {
		PngChunkIEND other = new PngChunkIEND(imgInfo);
		return other;
	}
}
