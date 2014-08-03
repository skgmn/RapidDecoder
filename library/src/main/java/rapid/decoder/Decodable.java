package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.Nullable;

public interface Decodable {
    @Nullable
    Bitmap decode();
    @SuppressWarnings("UnusedDeclaration")
    void draw(Canvas cv, Rect bounds);
    Decodable mutate();
}
