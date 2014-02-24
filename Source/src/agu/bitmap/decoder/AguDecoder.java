package agu.bitmap.decoder;

import java.io.IOException;
import java.io.InputStream;

import agu.bitmap.TwoPhaseBufferedInputStream;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.Build;

public class AguDecoder {
	static {
		System.loadLibrary("decoder");
		init();
	}
	
	private static final String MESSAGE_INVALID_REGION = "rectangle is outside the image";
	
	private TwoPhaseBufferedInputStream in;
	private Rect region;
	private boolean useFilter = true;
	private int sampleSize;
	
	private static native void init();
	
	public AguDecoder(InputStream in) {
		if (in instanceof TwoPhaseBufferedInputStream &&
				!((TwoPhaseBufferedInputStream) in).isSecondPhase()) {
			
			this.in = (TwoPhaseBufferedInputStream) in;
		} else {
			this.in = new TwoPhaseBufferedInputStream(in);
		}
	}

	public InputStream getInputStream() {
		return in;
	}
	
	public void setRegion(Rect region) {
		this.region = region;
	}
	
	public void setUseFilter(boolean filter) {
		useFilter = filter;
	}

	public Bitmap decode(Options opts) {
		final String mimeType = opts.outMimeType;
		Bitmap bitmap = null;
		if (mimeType == null) {
			bitmap = decodePng(opts);
			if (bitmap == null && !opts.mCancel) {
				in.seekToFirst();
				bitmap = decodeJpeg(opts);
			}
		} else if (mimeType.equals("image/png")) {
			in.seekToFirst();
			bitmap = decodePng(opts);
		} else if (mimeType.equals("image/jpeg")) {
			in.seekToFirst();
			bitmap = decodeJpeg(opts);
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
			
			in.startSecondPhase();
			
			final int width = d.getWidth();
			final int height = d.getHeight();

			validateRegion(width, height);

			final Config config = (opts.inPreferredConfig != null ? opts.inPreferredConfig : getDefaultConfig(false));

			return d.decode(region, useFilter, config, opts);
		} finally {
			d.close();
		}
	}
	
	private Bitmap decodePng(Options opts) {
		final PngDecoder d = new PngDecoder(in);
		try {
			if (!d.begin()) {
				return null;
			}
			
			if (opts.mCancel) return null;
			
			in.startSecondPhase();
			
			final int width = d.getWidth();
			final int height = d.getHeight();

			validateRegion(width, height);

			final Config config = (opts.inPreferredConfig != null ? opts.inPreferredConfig : getDefaultConfig(d.hasAlpha()));
			
			return d.decode(region, useFilter, config, opts);
		} finally {
			d.close();
		}
	}
	
	public void close() {
		try { in.close(); } catch (IOException e) {}
	}
}
