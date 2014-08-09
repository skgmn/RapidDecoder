package rapid.decoder;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

class ResourceLoader extends BitmapLoader {
	Resources res;
	int id;
	
	private float densityRatio;

	public ResourceLoader(Resources res, int id) {
		this.res = res;
		this.id = id;
	}
	
	protected ResourceLoader(ResourceLoader other) {
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

			if (mOptions.inDensity != 0 && mOptions.inTargetDensity != 0) {
				densityRatio = (float) mOptions.inTargetDensity / mOptions.inDensity;
			} else {
				densityRatio = 1;
			}
		}
		return densityRatio;
	}

	@NonNull
    @Override
	public BitmapLoader mutate() {
		return new ResourceLoader(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ res.hashCode() ^ id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ResourceLoader) || !super.equals(o)) return false;
		
		final ResourceLoader d = (ResourceLoader) o;
		return res.equals(d.res) && id == d.id;
	}

	@Override
	protected boolean isMemCacheSupported() {
		return true;
	}
}
