package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

public enum Quality {
    @SuppressWarnings("UnusedDeclaration")
    UNDEFINED {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = null;
            options.inDither = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = false;
            }
        }
    },
    HIGH {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inDither = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = false;
            }
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    HIGH_OPAQUE {
        @Override
        void applyTo(BitmapFactory.Options options) {
            HIGH.applyTo(options);
        }

        @Override
        boolean shouldConvertToOpaqueOnScale() {
            return true;
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    MID {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.ARGB_4444;
            options.inDither = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = true;
            }
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    MID_OPAQUE {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = true;
            }
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    LOW {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.ARGB_4444;
            options.inDither = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = false;
            }
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    LOW_OPAQUE {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = false;
            }
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    LOWEST {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.ARGB_4444;
            options.inDither = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = false;
            }
        }
    },
    @SuppressWarnings("UnusedDeclaration")
    LOWEST_OPAQUE {
        @Override
        void applyTo(BitmapFactory.Options options) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                options.inPreferQualityOverSpeed = false;
            }
        }
    };

    abstract void applyTo(BitmapFactory.Options options);

    boolean shouldConvertToOpaqueOnScale() {
        return false;
    }
}
