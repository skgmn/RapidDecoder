package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import agu.bitmap.jpeg.JPEGDecoder;
import agu.bitmap.png.ImageLineByte;
import agu.bitmap.png.PngReaderByte;
import agu.bitmap.png.PngjException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.os.Build;

public class PrivateDecoder {
	private InputStream in;
	private Rect region;
	private Resampler resampler;
	private Config config;
	
	public PrivateDecoder(InputStream in) {
		this.in = in;
		this.resampler = new IdenticalResampler();
	}

	public InputStream getInputStream() {
		return in;
	}
	
	public void setRegion(Rect region) {
		this.region = region;
	}
	
	public void setConfig(Config config) {
		this.config = config;
	}
	
	public Bitmap decode() {
		Bitmap bitmap = decodePng();
		if (bitmap == null) {
			try {
				in.reset();
				bitmap = decodeJpeg();
			} catch (IOException e) {
			}
		}

		return bitmap;
	}
	
	private static Config getDefaultConfig(boolean hasAlpha) {
		if (hasAlpha) {
			return Config.ARGB_8888;
		} else {
			return (Build.VERSION.SDK_INT >= 9 ? Config.ARGB_8888 : Config.RGB_565);
		}
	}
	
	public void setSampleSize(int sampleSize) {
		resampler = null;
		while (sampleSize > 1) {
			resampler = new HalfsizeResampler(resampler);
			sampleSize >>= 1;
		}
		
		if (resampler == null) {
			resampler = new IdenticalResampler();
		}
	}
	
	private Bitmap decodeJpeg() {
		final JPEGDecoder d = new JPEGDecoder(in);
		try {
			if (!d.startDecode()) {
				return null;
			}
		} catch (IOException e) {
			return null;
		}
		
		final int width = d.getImageWidth();
		final int height = d.getImageHeight();
		
		final int left = (region == null ? 0 : Math.min(Math.max(0, region.left), width));
		final int top = (region == null ? 0 : Math.min(Math.max(0, region.top), height));
		final int right = (region == null ? width : Math.min(Math.max(left, region.right), width));
		final int bottom = (region == null ? height : Math.min(Math.max(top, region.bottom), height));
		
		final int w = right - left;
		final int h = bottom - top;
		
		final int sampleSize = resampler.getSampleSize();
		final int sampledWidth = w / sampleSize;
		final int sampledHeight = h / sampleSize;
		
		final Config config = (this.config != null ? this.config : getDefaultConfig(false));
		final Bitmap bitmap = Bitmap.createBitmap(sampledWidth, sampledHeight, config);

		final int mcuRowHeight = d.getMCURowHeight();
		final IntBuffer buffer = IntBuffer.allocate(w * mcuRowHeight);

		try {
			d.skipMCURows(top / mcuRowHeight);
		} catch (IOException e) {
			return null;
		}
		
		int srcY = -(top % mcuRowHeight);
		int targetY = 0;
		
		while (srcY < h) {
			buffer.rewind();
			
			final int skipRows = (srcY < 0 ? -srcY : 0);
			try {
				d.decodeRGB(buffer, w, left, w, skipRows, 1);
			} catch (IOException e) {
				break;
			}
			
			final int[] pixels = buffer.array();

			final int heightToCopy = Math.min(mcuRowHeight, h - srcY) - skipRows;
			if (heightToCopy > 0) {
				final int offset = skipRows * w;
				
				if (sampleSize == 1) {
					bitmap.setPixels(pixels, offset, w, 0, targetY, w, heightToCopy);
					
					targetY += heightToCopy;
				} else {
					for (int i = 0; i < heightToCopy; ++i) {
						final int[] sampled = resampler.resample(pixels, offset + i * w, w);
						if (sampled == null) continue;
						
						bitmap.setPixels(sampled, 0, sampledWidth, 0, targetY++, sampledWidth, 1);
					}
				}
			}

			srcY += mcuRowHeight;
		}
		
		final int[] remain = resampler.finish();
		if (remain != null && targetY < sampledHeight) {
			bitmap.setPixels(remain, 0, sampledWidth, 0, targetY, sampledWidth, 1);
		}
		
		return bitmap;
	}

	private Bitmap decodePng() {
		final PngReaderByte pr;
		try {
			pr = new PngReaderByte(in);
		} catch (PngjException e) {
			return null;
		}
		
		final int channels = pr.imgInfo.channels;
		final boolean alpha = pr.imgInfo.alpha;
		
		if (channels != 1 && channels != 3 && channels != 4) {
			return null;
		}
		
		final int width = pr.imgInfo.cols;
		final int height = pr.imgInfo.rows;
		
		final int left = (region == null ? 0 : Math.min(Math.max(0, region.left), width));
		final int top = (region == null ? 0 : Math.min(Math.max(0, region.top), height));
		final int right = (region == null ? width : Math.min(Math.max(left, region.right), width));
		final int bottom = (region == null ? height : Math.min(Math.max(top, region.bottom), height));
		
		final int sampleSize = resampler.getSampleSize();
		
		final int w = right - left;
		final int h = bottom - top;

		final int sampledWidth = w / sampleSize;
		final int sampledHeight = h / sampleSize;
		
		final Config config = (this.config != null ? this.config : getDefaultConfig(alpha));
		
		final Bitmap bitmap = Bitmap.createBitmap(sampledWidth, sampledHeight, config);
		final int[] pixels = new int [w];
		
		int y = 0;
		
		for (int i = top; i < bottom; ++i) {
			final ImageLineByte row = (ImageLineByte) pr.readRow(i);
			final byte[] scanline = row.getScanline();
			
			for (int j = left; j < right; ++j) {
				final int offset = j * channels;

				switch (channels) {
				case 1: {
					final int pixel = scanline[offset];
					pixels[j - left] = Color.argb(0xff, pixel, pixel, pixel);
					break;
				}
					
				case 3:
					pixels[j - left] = 0xff000000 | ((scanline[offset] & 0xff) << 16)
							| ((scanline[offset + 1] & 0xff) << 8) | ((scanline[offset + 2] & 0xff));
					break;
					
				case 4:
					pixels[j - left] = (((scanline[offset + 3] & 0xff) << 24) | ((scanline[offset] & 0xff) << 16)
							| ((scanline[offset + 1] & 0xff) << 8) | ((scanline[offset + 2] & 0xff)));
					break;
				}
			}
			
			final int[] sampled = resampler.resample(pixels, 0, pixels.length);
			if (sampled != null) {
				bitmap.setPixels(sampled, 0, sampledWidth, 0, y, sampledWidth, 1);
				++y;
			}
		}
		
		final int[] remain = resampler.finish();
		if (remain != null) {
			bitmap.setPixels(remain, 0, sampledWidth, 0, y, sampledWidth, 1);
		}
		
		return bitmap;
	}
	
	public void close() {
		try {
			in.close();
		} catch (IOException e) {
		}
	}
}
