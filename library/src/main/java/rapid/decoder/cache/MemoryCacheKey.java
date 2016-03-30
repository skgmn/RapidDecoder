package rapid.decoder.cache;

import android.support.annotation.NonNull;

import rapid.decoder.BitmapLoader;

public class MemoryCacheKey {
    private static MemoryCacheKey sTempKey;

    @NonNull
    public final BitmapLoader loader;
    public final boolean approximately;

    public MemoryCacheKey(@NonNull BitmapLoader loader, boolean approximately) {
        this.loader = loader;
        this.approximately = approximately;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryCacheKey that = (MemoryCacheKey) o;
        return approximately == that.approximately && loader.equals(that.loader);
    }

    @Override
    public int hashCode() {
        int result = loader.hashCode();
        result = 31 * result + (approximately ? 1 : 0);
        return result;
    }
}
