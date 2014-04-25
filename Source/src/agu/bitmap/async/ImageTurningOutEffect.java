package agu.bitmap.async;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class ImageTurningOutEffect {
	public abstract void visit(ImageView iv, Drawable d);
	public abstract void visit(TextView tv, int index, int width, int height, Drawable d);

	protected static void setDrawableSize(Drawable d, int width, int height) {
		if (d == null) return;
		
		d.setBounds(0, 0,
				width == 0 ? d.getIntrinsicWidth() : width,
				height == 0 ? d.getIntrinsicHeight() : height);
	}
}
