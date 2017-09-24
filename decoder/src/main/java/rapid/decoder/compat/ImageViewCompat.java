package rapid.decoder.compat;

import android.os.Build;
import android.widget.ImageView;

import java.lang.reflect.Field;

public final class ImageViewCompat {
    private static Field sFieldMaxWidth;
    private static Field sFieldMaxHeight;
    
    public static int getMaxWidth(ImageView v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return v.getMaxWidth();
        } else {
            try {
                if (sFieldMaxWidth == null) {
                    sFieldMaxWidth = ImageView.class.getDeclaredField("mMaxWidth");
                }
                sFieldMaxWidth.setAccessible(true);
                return sFieldMaxWidth.getInt(v);
            } catch (Exception e) {
                return Integer.MAX_VALUE;
            }
        }
    }

    public static int getMaxHeight(ImageView v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return v.getMaxHeight();
        } else {
            try {
                if (sFieldMaxHeight == null) {
                    sFieldMaxHeight = ImageView.class.getDeclaredField("mMaxHeight");
                }
                sFieldMaxHeight.setAccessible(true);
                return sFieldMaxHeight.getInt(v);
            } catch (Exception e) {
                return Integer.MAX_VALUE;
            }
        }
    }
}
