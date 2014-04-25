package agu.bitmap.async;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class ImageTurningOutEffect {
	public abstract void visit(ImageView iv, Bitmap bitmap);
	public abstract void visit(TextView tv, int place, Bitmap bitmap);
}
