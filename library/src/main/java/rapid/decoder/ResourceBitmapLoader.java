package rapid.decoder;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

class ResourceBitmapLoader extends BitmapLoader {
	Resources res;

	private float densityRatio;

	public ResourceBitmapLoader(Resources res, int id) {
		this.res = res;
        id(id);
	}
	
	protected ResourceBitmapLoader(ResourceBitmapLoader other) {
		super(other);
		res = other.res;
        id(other.id());
		densityRatio = other.densityRatio;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeResource(res, (Integer) id(), opts);
	}
	
	@Override
	protected InputStream getInputStream() {
		return res.openRawResource((Integer) id());
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
            InputStream in = getInputStream();
            if (in == null) {
                return null;
            }
            return BitmapRegionDecoder.newInstance(in, false);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected float densityRatio() {
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
		return new ResourceBitmapLoader(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ res.hashCode() ^ id().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ResourceBitmapLoader) || !super.equals(o)) return false;
		
		final ResourceBitmapLoader d = (ResourceBitmapLoader) o;
		return res.equals(d.res) && id().equals(d.id());
	}
}
