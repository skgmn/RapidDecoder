package agu.bitmap.png;

import agu.bitmap.png.chunks.PngChunkPLTE;
import agu.bitmap.png.chunks.PngChunkTRNS;

/**
 * Bunch of utility static methods to proces an image line at the pixel level.
 * <p>
 * WARNING: this has little testing/optimizing, and this API is not stable. some
 * methods will probably be changed or removed if future releases.
 * <p>
 * WARNING: most methods for getting/setting values work currently only for
 * ImageLine or ImageLineByte
 */
public class ImageLineHelper {

	static int[] DEPTH_UNPACK_1;
	static int[] DEPTH_UNPACK_2;
	static int[] DEPTH_UNPACK_4;
	static int[][] DEPTH_UNPACK;

	private static void initDepthScale() {
		DEPTH_UNPACK_1 = new int[2];
		for (int i = 0; i < 2; i++)
			DEPTH_UNPACK_1[i] = i * 255;
		DEPTH_UNPACK_2 = new int[4];
		for (int i = 0; i < 4; i++)
			DEPTH_UNPACK_2[i] = (i * 255) / 3;
		DEPTH_UNPACK_4 = new int[16];
		for (int i = 0; i < 16; i++)
			DEPTH_UNPACK_4[i] = (i * 255) / 15;
		DEPTH_UNPACK = new int[][] { null, DEPTH_UNPACK_1, DEPTH_UNPACK_2, null, DEPTH_UNPACK_4 };
	}

	public static void scaleUp(IImageLineArray line) {
		if (line.getImageInfo().indexed || line.getImageInfo().bitDepth >= 8)
			return;
		if (DEPTH_UNPACK_1 == null || DEPTH_UNPACK == null)
			initDepthScale();
		final int[] scaleArray = DEPTH_UNPACK[line.getImageInfo().bitDepth];
		if (line instanceof ImageLineByte) {
			ImageLineByte iline = (ImageLineByte) line;
			for (int i = 0; i < iline.getSize(); i++)
				iline.scanline[i] = (byte) scaleArray[iline.scanline[i]];
		} else
			throw new PngjException("not implemented");
	}

	public static void scaleDown(IImageLineArray line) {
		if (line.getImageInfo().indexed || line.getImageInfo().bitDepth >= 8)
			return;
		
		final int scalefactor = 8 - line.getImageInfo().bitDepth;
		if (line instanceof ImageLineByte) {
			ImageLineByte iline = (ImageLineByte) line;
			for (int i = 0; i < line.getSize(); i++)
				iline.scanline[i] = (byte) ((iline.scanline[i] & 0xFF) >> scalefactor);
		} else {
			throw new PngjException("not implemented");
		}
	}

	/**
	 * warning: this only works with byte format, and alters ImageLine!
	 */
	static int[] lineToARGB32(ImageLineByte line, PngChunkPLTE pal, PngChunkTRNS trns, int[] buf) {
		boolean alphachannel = line.imgInfo.alpha;
		int cols = line.getImageInfo().cols;
		if (line.getImageInfo().packed)
			scaleUp(line);
		if (buf == null || buf.length < cols)
			buf = new int[cols];
		int index, rgb, alpha, ga, g;
		if (line.getImageInfo().indexed) {// palette
			int nindexesWithAlpha = trns != null ? trns.getPalletteAlpha().length : 0;
			for (int c = 0; c < cols; c++) {
				index = line.scanline[c] & 0xFF;
				rgb = pal.getEntry(index);
				alpha = index < nindexesWithAlpha ? trns.getPalletteAlpha()[index] : 255;
				buf[c] = (alpha << 24) | rgb;
			}
		} else if (line.imgInfo.greyscale) { //
			ga = trns != null ? trns.getGray() : -1;
			for (int c = 0, c2 = 0; c < cols; c++) {
				g = (line.scanline[c2++] & 0xFF);
				alpha = alphachannel ? line.scanline[c2++] & 0xFF : (g != ga ? 255 : 0);
				buf[c] = (alpha << 24) | g | (g << 8) | (g << 16);
			}
		} else { // true color
			ga = trns != null ? trns.getRGB888() : -1;
			for (int c = 0, c2 = 0; c < cols; c++) {
				rgb = ((line.scanline[c2++] & 0xFF) << 16) | ((line.scanline[c2++] & 0xFF) << 8)
						| (line.scanline[c2++] & 0xFF);
				alpha = alphachannel ? line.scanline[c2++] & 0xFF : (rgb != ga ? 255 : 0);
				buf[c] = (alpha << 24) | rgb;
			}
		}
		return buf;
	}

	/**
	 * warning: this only works with byte format, and alters ImageLine! For
	 * packed formats, the line should be unpacked,
	 */
	static byte[] lineToRGBA8888(ImageLineByte line, PngChunkPLTE pal, PngChunkTRNS trns, byte[] buf) {
		boolean alphachannel = line.imgInfo.alpha;
		int cols = line.imgInfo.cols;
		if (line.imgInfo.packed)
			scaleUp(line);
		int bytes = cols * 4;
		if (buf == null || buf.length < bytes)
			buf = new byte[bytes];
		int index, rgb, ga;
		byte val;
		if (line.imgInfo.indexed) {// palette
			int nindexesWithAlpha = trns != null ? trns.getPalletteAlpha().length : 0;
			for (int c = 0, b = 0; c < cols; c++) {
				index = line.scanline[c] & 0xFF;
				rgb = pal.getEntry(index);
				buf[b++] = (byte) ((rgb >> 16) & 0xFF);
				buf[b++] = (byte) ((rgb >> 8) & 0xFF);
				buf[b++] = (byte) (rgb & 0xFF);
				buf[b++] = (byte) (index < nindexesWithAlpha ? trns.getPalletteAlpha()[index] : 255);
			}
		} else if (line.imgInfo.greyscale) { //
			ga = trns != null ? trns.getGray() : -1;
			for (int c = 0, b = 0; b < bytes;) {
				val = line.scanline[c++];
				buf[b++] = val;
				buf[b++] = val;
				buf[b++] = val;
				buf[b++] = alphachannel ? line.scanline[c++] : ((int) (val & 0xFF) == ga) ? (byte) 0 : (byte) 255;
			}
		} else { // true color
			if (alphachannel) // same format!
				System.arraycopy(line.scanline, 0, buf, 0, bytes);
			else {
				for (int c = 0, b = 0; b < bytes;) {
					buf[b++] = line.scanline[c++];
					buf[b++] = line.scanline[c++];
					buf[b++] = line.scanline[c++];
					buf[b++] = (byte) (255); // tentative (probable)
					if (trns != null && buf[b - 3] == (byte) trns.getRGB()[0] && buf[b - 2] == (byte) trns.getRGB()[1]
							&& buf[b - 1] == (byte) trns.getRGB()[2]) // not very efficient, but not frecuent
						buf[b - 1] = 0;
				}
			}
		}
		return buf;
	}

	static byte[] lineToRGB888(ImageLineByte line, PngChunkPLTE pal, byte[] buf) {
		boolean alphachannel = line.imgInfo.alpha;
		int cols = line.imgInfo.cols;
		int bytes = cols * 3;
		if (buf == null || buf.length < bytes)
			buf = new byte[bytes];
		byte val;
		int[] rgb = new int[3];
		if (line.imgInfo.indexed) {// palette
			for (int c = 0, b = 0; c < cols; c++) {
				pal.getEntryRgb(line.scanline[c] & 0xFF, rgb);
				buf[b++] = (byte) rgb[0];
				buf[b++] = (byte) rgb[1];
				buf[b++] = (byte) rgb[2];
			}
		} else if (line.imgInfo.greyscale) { //
			for (int c = 0, b = 0; b < bytes;) {
				val = line.scanline[c++];
				buf[b++] = val;
				buf[b++] = val;
				buf[b++] = val;
				if (alphachannel)
					c++; // skip alpha
			}
		} else { // true color
			if (!alphachannel) // same format!
				System.arraycopy(line.scanline, 0, buf, 0, bytes);
			else {
				for (int c = 0, b = 0; b < bytes;) {
					buf[b++] = line.scanline[c++];
					buf[b++] = line.scanline[c++];
					buf[b++] = line.scanline[c++];
					c++;// skip alpha
				}
			}
		}
		return buf;
	}

	public static int interpol(int a, int b, int c, int d, double dx, double dy) {
		// a b -> x (0-1)
		// c d
		double e = a * (1.0 - dx) + b * dx;
		double f = c * (1.0 - dx) + d * dx;
		return (int) (e * (1 - dy) + f * dy + 0.5);
	}

	public static int clampTo_0_255(int i) {
		return i > 255 ? 255 : (i < 0 ? 0 : i);
	}

	public static int clampTo_0_65535(int i) {
		return i > 65535 ? 65535 : (i < 0 ? 0 : i);
	}

	public static int clampTo_128_127(int x) {
		return x > 127 ? 127 : (x < -128 ? -128 : x);
	}

	static int getMaskForPackedFormats(int bitDepth) { // Utility function for pack/unpack
		if (bitDepth == 4)
			return 0xf0;
		else if (bitDepth == 2)
			return 0xc0;
		else
			return 0x80; // bitDepth == 1
	}

	static int getMaskForPackedFormatsLs(int bitDepth) { // Utility function for pack/unpack
		if (bitDepth == 4)
			return 0x0f;
		else if (bitDepth == 2)
			return 0x03;
		else
			return 0x01; // bitDepth == 1
	}

}
