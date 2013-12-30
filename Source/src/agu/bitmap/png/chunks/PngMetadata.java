package agu.bitmap.png.chunks;

import java.util.ArrayList;
import java.util.List;

/**
 * We consider "image metadata" every info inside the image except for the most
 * basic image info (IHDR chunk - ImageInfo class) and the pixels values.
 * <p>
 * This includes the palette (if present) and all the ancillary chunks
 * <p>
 * This class provides a wrapper over the collection of chunks of a image (read
 * or to write) and provides some high level methods to access them
 */
public class PngMetadata {
	private final ChunksList chunkList;

	public PngMetadata(ChunksList chunks) {
		this.chunkList = chunks;
	}

	// ///// high level utility methods follow ////////////

	// //////////// DPI

	/**
	 * returns -1 if not found or dimension unknown
	 */
	public double[] getDpi() {
		PngChunk c = chunkList.getById1(ChunkHelper.pHYs, true);
		if (c == null)
			return new double[] { -1, -1 };
		else
			return ((PngChunkPHYS) c).getAsDpi2();
	}

	// //////////// TIME

	/**
	 * null if not found
	 */
	public PngChunkTIME getTime() {
		return (PngChunkTIME) chunkList.getById1(ChunkHelper.tIME);
	}

	public String getTimeAsString() {
		PngChunkTIME c = getTime();
		return c == null ? "" : c.getAsString();
	}

	// //////////// TEXT

	/**
	 * gets all text chunks with a given key
	 * <p>
	 * returns null if not found
	 * <p>
	 * Warning: this does not check the "lang" key of iTxt
	 */
	@SuppressWarnings("unchecked")
	public List<? extends PngChunkTextVar> getTxtsForKey(String k) {
		@SuppressWarnings("rawtypes")
		List c = new ArrayList();
		c.addAll(chunkList.getById(ChunkHelper.tEXt, k));
		c.addAll(chunkList.getById(ChunkHelper.zTXt, k));
		c.addAll(chunkList.getById(ChunkHelper.iTXt, k));
		return c;
	}

	/**
	 * Returns empty if not found, concatenated (with newlines) if multiple! -
	 * and trimmed
	 * <p>
	 * Use getTxtsForKey() if you don't want this behaviour
	 */
	public String getTxtForKey(String k) {
		List<? extends PngChunkTextVar> li = getTxtsForKey(k);
		if (li.isEmpty())
			return "";
		StringBuilder t = new StringBuilder();
		for (PngChunkTextVar c : li)
			t.append(c.getVal()).append("\n");
		return t.toString().trim();
	}

	/**
	 * Returns the palette chunk, if present
	 * 
	 * @return null if not present
	 */
	public PngChunkPLTE getPLTE() {
		return (PngChunkPLTE) chunkList.getById1(PngChunkPLTE.ID);
	}

	/**
	 * Returns the TRNS chunk, if present
	 * 
	 * @return null if not present
	 */
	public PngChunkTRNS getTRNS() {
		return (PngChunkTRNS) chunkList.getById1(PngChunkTRNS.ID);
	}
}
