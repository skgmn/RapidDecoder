package rapid.decoder;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

public class DecodeFromUriTest extends AndroidTestCase {
    public void testInvalidUri() {
        Bitmap bitmap = BitmapDecoder.from(getContext(), "android.resource://a.b.c/12345").decode();
        assertNull(bitmap);
    }

    public void testValidUri() {
        Bitmap bitmap = BitmapDecoder.from(getContext(), "android.resource://rapid.decoder" +
                ".test/drawable/android").decode();
        assertNotNull(bitmap);

        bitmap = BitmapDecoder.from(getContext(), "android.resource://rapid.decoder" +
                ".test/2130837504").decode();
        assertNotNull(bitmap);
    }
}
