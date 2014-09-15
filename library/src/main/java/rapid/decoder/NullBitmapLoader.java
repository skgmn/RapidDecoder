package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;

import rapid.decoder.cache.CacheSource;

public class NullBitmapLoader extends BitmapLoader {
    @Override
    public int sourceWidth() {
        return 0;
    }

    @Override
    public int sourceHeight() {
        return 0;
    }

    @Override
    public void draw(Canvas cv, Rect rectDest) {

    }

    @Override
    public BitmapLoader config(Bitmap.Config config) {
        return null;
    }

    @Override
    public Bitmap.Config config() {
        return null;
    }

    @Override
    public Bitmap createAndDraw(int width, int height, @NonNull Rect rectDest, @Nullable Drawable
            background) {
        return null;
    }

    @Override
    public BitmapLoader useBuiltInDecoder(boolean force) {
        return null;
    }

    @Override
    public BitmapLoader filterBitmap(boolean filter) {
        return null;
    }

    @Override
    public CacheSource cacheSource() {
        return null;
    }

    @Override
    public void cancel() {

    }

    @Override
    public Bitmap decode() {
        return null;
    }

    @NonNull
    @Override
    public BitmapDecoder fork() {
        return this;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public BitmapLoader mutable(boolean mutable) {
        return null;
    }

    @Nullable
    @Override
    protected Bitmap decode(BitmapFactory.Options opts) {
        return null;
    }

    @Nullable
    @Override
    protected InputStream getInputStream() {
        return null;
    }

    @Override
    protected BitmapRegionDecoder createBitmapRegionDecoder() {
        return null;
    }
}
