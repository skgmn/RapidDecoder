package rapid.decoder;

import android.content.Context;
import android.graphics.Bitmap;

public abstract class BitmapPostProcessor {
    private Context mContext;

    public BitmapPostProcessor() {
        this(null);
    }

    public BitmapPostProcessor(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    public abstract Bitmap process(Bitmap bitmap);
}
