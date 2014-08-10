package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;

public class NullBitmapLoader extends BitmapLoader {
    @Override
    protected Bitmap decode(BitmapFactory.Options opts) {
        return null;
    }

    @Nullable
    @Override
    protected InputStream getInputStream() {
        return null;
    }

    @Nullable
    @Override
    protected BitmapRegionDecoder createBitmapRegionDecoder() {
        return null;
    }

    @NonNull
    @Override
    public BitmapDecoder mutate() {
        return new NullBitmapLoader();
    }
}
