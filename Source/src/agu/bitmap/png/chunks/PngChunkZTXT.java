package agu.bitmap.png.chunks;

import agu.bitmap.png.ImageInfo;
import agu.bitmap.png.PngjException;

/**
 * zTXt chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11zTXt
 */
public class PngChunkZTXT extends PngChunkTextVar {
	public final static String ID = ChunkHelper.zTXt;

	// http://www.w3.org/TR/PNG/#11zTXt
	public PngChunkZTXT(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		int nullsep = -1;
		for (int i = 0; i < c.data.length; i++) { // look for first zero
			if (c.data[i] != 0)
				continue;
			nullsep = i;
			break;
		}
		if (nullsep < 0 || nullsep > c.data.length - 2)
			throw new PngjException("bad zTXt chunk: no separator found");
		key = ChunkHelper.toString(c.data, 0, nullsep);
		int compmet = (int) c.data[nullsep + 1];
		if (compmet != 0)
			throw new PngjException("bad zTXt chunk: unknown compression method");
		byte[] uncomp = ChunkHelper.compressBytes(c.data, nullsep + 2, c.data.length - nullsep - 2, false); // uncompress
		val = ChunkHelper.toString(uncomp);
	}

	@Override
	public PngChunk cloneForWrite(ImageInfo imgInfo) {
		PngChunkZTXT other = new PngChunkZTXT(imgInfo);
		other.key = key;
		other.val = val;
		return other;
	}
}
