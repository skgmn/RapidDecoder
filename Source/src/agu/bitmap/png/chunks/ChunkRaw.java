package agu.bitmap.png.chunks;

import java.io.ByteArrayInputStream;
import java.util.zip.CRC32;

import agu.bitmap.png.PngHelperInternal;
import agu.bitmap.png.PngjBadCrcException;
import agu.bitmap.png.PngjException;

/**
 * Raw (physical) chunk.
 * <p>
 * Short lived object, to be created while serialing/deserializing Do not reuse
 * it for different chunks. <br>
 * See http://www.libpng.org/pub/png/spec/1.2/PNG-Structure.html
 */
public class ChunkRaw {
	/**
	 * The length counts only the data field, not itself, the chunk type code,
	 * or the CRC. Zero is a valid length. Although encoders and decoders should
	 * treat the length as unsigned, its value must not exceed 231-1 bytes.
	 */
	public final int len;

	/**
	 * A 4-byte chunk type code. uppercase and lowercase ASCII letters
	 */
	public final byte[] idbytes;
	public final String id;

	/**
	 * The data bytes appropriate to the chunk type, if any. This field can be
	 * of zero length. Does not include crc. If it's null, it means that the
	 * data is ot available
	 */
	public byte[] data = null;
	/**
	 * offset in the full PNG stream, only informational, for read (0=NA)
	 */
	private long offset = 0;

	/**
	 * A 4-byte CRC (Cyclic Redundancy Check) calculated on the preceding bytes
	 * in the chunk, including the chunk type code and chunk data fields, but
	 * not including the length field.
	 */
	public byte[] crcval = new byte[4];

	private CRC32 crcengine; // lazily instantiated

	public ChunkRaw(int len, String id, boolean alloc) {
		this.len = len;
		this.id = id;
		this.idbytes = ChunkHelper.toBytes(id);
		for (int i = 0; i < 4; i++) {
			if (idbytes[i] < 65 || idbytes[i] > 122 || (idbytes[i] > 90 && idbytes[i] < 97))
				throw new PngjException("Bad id chunk: must be ascii letters " + id);
		}
		if (alloc)
			allocData();
	}

	public ChunkRaw(int len, byte[] idbytes, boolean alloc) {
		this(len, ChunkHelper.toString(idbytes), alloc);
	}

	public void allocData() { // TODO: not public
		if (data == null || data.length < len)
			data = new byte[len];
	}

	public void checkCrc() {
		int crcComputed = (int) crcengine.getValue();
		int crcExpected = PngHelperInternal.readInt4fromBytes(crcval, 0);
		if (crcComputed != crcExpected)
			throw new PngjBadCrcException("chunk: " + this.toString() + " expected=" + crcExpected + " read="
					+ crcComputed);
	}

	public void updateCrc(byte[] buf, int off, int len) {
		if (crcengine == null)
			crcengine = new CRC32();
		crcengine.update(buf, off, len);
	}

	ByteArrayInputStream getAsByteStream() { // only the data
		return new ByteArrayInputStream(data);
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public String toString() {
		return "chunkid=" + ChunkHelper.toString(idbytes) + " len=" + len;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (int) (offset ^ (offset >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChunkRaw other = (ChunkRaw) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (offset != other.offset)
			return false;
		return true;
	}

}
