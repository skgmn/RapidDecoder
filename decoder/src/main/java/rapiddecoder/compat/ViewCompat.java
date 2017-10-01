package rapiddecoder.compat;

import android.os.Build;
import android.view.View;

import java.lang.reflect.Field;

public final class ViewCompat {
    private static Field sFieldMinWidth;
    private static Field sFieldMinHeight;
    
    public static int getMinimumWidth(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return v.getMinimumWidth();
        } else {
            try {
                if (sFieldMinWidth == null) {
                    sFieldMinWidth = View.class.getDeclaredField("mMinWidth");
                }
                sFieldMinWidth.setAccessible(true);
                return sFieldMinWidth.getInt(v);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    public static int getMinimumHeight(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return v.getMinimumHeight();
        } else {
            try {
                if (sFieldMinHeight == null) {
                    sFieldMinHeight = View.class.getDeclaredField("mMinHeight");
                }
                sFieldMinHeight.setAccessible(true);
                return sFieldMinHeight.getInt(v);
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
