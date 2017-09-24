package rapid.decoder.binder;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

public interface DrawableInflater {
    @Nullable
    Drawable inflate(Context context);
}
