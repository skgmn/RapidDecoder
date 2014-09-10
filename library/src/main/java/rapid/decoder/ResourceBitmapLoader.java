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
    private static class Identifier {
        public Resources res;
        public int id;

        private Identifier(Resources res, int id) {
            this.res = res;
            this.id = id;
        }

        @Override
        public int hashCode() {
            return res.hashCode() ^ id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Identifier)) return false;

            Identifier id2 = (Identifier) o;
            return res.equals(id2.res) && id == id2.id;
        }
    }

	private float densityRatio;

	public ResourceBitmapLoader(Resources res, int id) {
        mId = new Identifier(res, id);
	}
	
	protected ResourceBitmapLoader(ResourceBitmapLoader other) {
		super(other);
		densityRatio = other.densityRatio;
	}

	@Override
	protected Bitmap decode(Options opts) {
        Identifier id = (Identifier) mId;
		return BitmapFactory.decodeResource(id.res, id.id, opts);
	}
	
	@Override
	protected InputStream getInputStream() {
		Identifier id = (Identifier) mId;
        return id.res.openRawResource(id.id);
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
	public BitmapLoader fork() {
		return new ResourceBitmapLoader(this);
	}
	
	@Override
	public int hashCode() {
        Identifier id = (Identifier) mId;
		return super.hashCode() ^ id.res.hashCode() ^ id.id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ResourceBitmapLoader) || !super.equals(o)) return false;
		
		final ResourceBitmapLoader d = (ResourceBitmapLoader) o;
        Identifier id = (Identifier) mId;
        return id.equals(d.mId);
	}
}
