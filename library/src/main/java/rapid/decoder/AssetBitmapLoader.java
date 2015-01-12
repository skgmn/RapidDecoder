package rapid.decoder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

class AssetBitmapLoader extends BitmapLoader {
    private AssetManager mAssetManager;
    private String mPath;

	public AssetBitmapLoader(Context context, @NonNull String path) {
        mAssetManager = context.getAssets();
        mPath = path;
	}

	protected AssetBitmapLoader(AssetBitmapLoader other) {
		super(other);
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeStream(openInputStream(), null, opts);
	}

    @Override
	protected InputStream openInputStream() {
		try {
            return new TwiceReadableInputStream(mAssetManager.open(mPath));
		} catch (IOException ignored) {
        }
        return null;
    }

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
            InputStream in = openInputStream();
            if (in == null) {
                return null;
            }
            return BitmapRegionDecoder.newInstance(in, false);
		} catch (IOException e) {
			return null;
		}
	}

	@NonNull
    @Override
	public BitmapLoader fork() {
		return new AssetBitmapLoader(this);
	}
	
	@Override
	public boolean equals(Object o) {
        return o == this || o instanceof AssetBitmapLoader && super.equals(o);
    }
}
