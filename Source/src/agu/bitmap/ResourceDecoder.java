package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class ResourceDecoder extends ExternalBitmapDecoder {
	Resources res;
	int id;
	
	private float densityRatio;

	public ResourceDecoder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}
	
	protected ResourceDecoder(ResourceDecoder other) {
		super(other);
		res = other.res;
		id = other.id;
		densityRatio = other.densityRatio;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeResource(res, id, opts);
	}
	
	@Override
	protected InputStream getInputStream() {
		return res.openRawResource(id);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(getInputStream(), false);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected float getDensityRatio() {
		if (densityRatio == 0) {
			decodeBounds();

			if (opts.inDensity != 0 && opts.inTargetDensity != 0) {
				densityRatio = (float) opts.inTargetDensity / opts.inDensity;
			} else {
				densityRatio = 1;
			}
		}
		return densityRatio;
	}

	@Override
	public ExternalBitmapDecoder clone() {
		return new ResourceDecoder(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ res.hashCode() ^ id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ResourceDecoder) || !super.equals(o)) return false;
		
		final ResourceDecoder d = (ResourceDecoder) o;
		return res.equals(d.res) && id == d.id;
	}

	@Override
	protected boolean isMemCacheSupported() {
		return true;
	}
}
