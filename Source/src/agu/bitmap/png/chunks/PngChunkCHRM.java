package agu.bitmap.png.chunks;

import agu.bitmap.png.ImageInfo;
import agu.bitmap.png.PngHelperInternal;
import agu.bitmap.png.PngjException;

/**
 * cHRM chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11cHRM
 */
public class PngChunkCHRM extends PngChunkSingle {
	public final static String ID = ChunkHelper.cHRM;

	// http://www.w3.org/TR/PNG/#11cHRM
	private double whitex, whitey;
	private double redx, redy;
	private double greenx, greeny;
	private double bluex, bluey;

	public PngChunkCHRM(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.AFTER_PLTE_BEFORE_IDAT;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		if (c.len != 32)
			throw new PngjException("bad chunk " + c);
		whitex = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 0));
		whitey = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 4));
		redx = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 8));
		redy = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 12));
		greenx = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 16));
		greeny = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 20));
		bluex = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 24));
		bluey = PngHelperInternal.intToDouble100000(PngHelperInternal.readInt4fromBytes(c.data, 28));
	}

	@Override
	public PngChunk cloneForWrite(ImageInfo imgInfo) {
		PngChunkCHRM other = new PngChunkCHRM(imgInfo);
		other.whitex = whitex;
		other.whitey = whitex;
		other.redx = redx;
		other.redy = redy;
		other.greenx = greenx;
		other.greeny = greeny;
		other.bluex = bluex;
		other.bluey = bluey;
		return other;
	}

	public void setChromaticities(double whitex, double whitey, double redx, double redy, double greenx, double greeny,
			double bluex, double bluey) {
		invalidateRaw();
		this.whitex = whitex;
		this.redx = redx;
		this.greenx = greenx;
		this.bluex = bluex;
		this.whitey = whitey;
		this.redy = redy;
		this.greeny = greeny;
		this.bluey = bluey;
	}

	public double[] getChromaticities() {
		return new double[] { whitex, whitey, redx, redy, greenx, greeny, bluex, bluey };
	}

}
