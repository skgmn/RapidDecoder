package agu.bitmap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import agu.bitmap.jpeg.JpegDecoder;
import agu.bitmap.png.ImageLineByte;
import agu.bitmap.png.PngReaderByte;
import agu.bitmap.png.PngjException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;

public class AguDecoder {
	private static final int MARK_READ_LIMIT = 16384;
	
	private static final String MESSAGE_INVALID_REGION = "rectangle is outside the image";
	
	private InputStream in;
	private Rect region;
	private Config config;
	private boolean useFilter = true;
	private int sampleSize;
	
	public AguDecoder(InputStream in) {
		this.in = in;
		
		if (!in.markSupported()) {
			in = new BufferedInputStream(in, MARK_READ_LIMIT + 1);
		}
		in.mark(MARK_READ_LIMIT);
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
	
	public void setUseFilter(boolean filter) {
		useFilter = filter;
	}

	public Bitmap decode(Options opts) {
		final String mimeType = opts.outMimeType;
		Bitmap bitmap = null;
		try {
			if (mimeType == null) {
				in.reset();
				bitmap = decodePng(opts);
				if (bitmap == null) {
					in.reset();
					bitmap = decodeJpeg(opts);
				}
			} else if (mimeType.equals("image/png")) {
				in.reset();
				bitmap = decodePng(opts);
			} else if (mimeType.equals("image/jpeg")) {
				in.reset();
				bitmap = decodeJpeg(opts);
			}
		} catch (IOException e1) {
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
		this.sampleSize = sampleSize;
	}
	
	private void validateRegion(int width, int height) {
		if (region != null &&
				(region.left < 0 || region.top < 0 || region.right > width || region.bottom > height)) {
			
			throw new IllegalArgumentException(MESSAGE_INVALID_REGION);
		}
	}
	
	private Bitmap decodeJpeg(Options opts) {
		final JpegDecoder d = new JpegDecoder(in);
		try {
			if (!d.begin()) {
				return null;
			}
			
			if (opts.mCancel) return null;
			
			final int width = d.getWidth();
			final int height = d.getHeight();

			validateRegion(width, height);
			
			final int left = (region == null ? 0 : region.left);
			final int top = (region == null ? 0 : region.top);
			final int right = (region == null ? width : region.right);
			final int bottom = (region == null ? height : region.bottom);
			
			final int w = right - left;
			final int h = bottom - top;
			
			final int sampledWidth = w / sampleSize;
			final int sampledHeight = h / sampleSize;
			
			final Resampler resampler;
			if (sampleSize > 1) {
				resampler = new DownsizeResampler(sampledWidth, sampleSize, useFilter);
			} else {
				resampler = new IdentityResampler();
			}
			
			final Config config = (this.config != null ? this.config : getDefaultConfig(false));
			final Bitmap bitmap = Bitmap.createBitmap(sampledWidth, sampledHeight, config);
			
			final int[] scanline = new int [w];
	
			int y = 0;
			
			d.sliceColumn(left, w);
			for (int i = 0; i < top; ++i) {
				if (opts.mCancel) return null;
				d.skipLine();
			}
			
			for (int i = top; i < bottom; ++i) {
				d.readLine(scanline);
				
				if (opts.mCancel) return null;
				
				final int[] sampled = resampler.resample(scanline, 0, w);
				if (sampled != null) {
					bitmap.setPixels(sampled, 0, sampledWidth, 0, y, sampledWidth, 1);
					++y;
				}
			}
			
			final int[] remain = resampler.finish();
			if (remain != null && y < sampledHeight) {
				bitmap.setPixels(remain, 0, sampledWidth, 0, y, sampledWidth, 1);
			}
			
			return bitmap;
		} finally {
			d.close();
		}
	}
	
	private Bitmap decodePng(Options opts) {
		final PngReaderByte pr;
		try {
			pr = new PngReaderByte(in);
		} catch (PngjException e) {
			return null;
		}
		
		if (opts.mCancel) return null;
		
		final int channels = pr.imgInfo.channels;
		final boolean alpha = pr.imgInfo.alpha;
		
		if (channels != 1 && channels != 3 && channels != 4) {
			return null;
		}
		
		final int width = pr.imgInfo.cols;
		final int height = pr.imgInfo.rows;
		
		validateRegion(width, height);
		
		final int left = (region == null ? 0 : region.left);
		final int top = (region == null ? 0 : region.top);
		final int right = (region == null ? width : region.right);
		final int bottom = (region == null ? height : region.bottom);
		
		final int w = right - left;
		final int h = bottom - top;

		final int sampledWidth = w / sampleSize;
		final int sampledHeight = h / sampleSize;
		
		final Resampler resampler;
		if (sampleSize > 1) {
			resampler = new DownsizeResampler(sampledWidth, sampleSize, useFilter);
		} else {
			resampler = new IdentityResampler();
		}
		
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
			
			if (opts.mCancel) return null;

			final int[] sampled = resampler.resample(pixels, 0, pixels.length);
			if (sampled != null) {
				bitmap.setPixels(sampled, 0, sampledWidth, 0, y, sampledWidth, 1);
				++y;
			}
		}
		
		final int[] remain = resampler.finish();
		if (remain != null && y < sampledHeight) {
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
