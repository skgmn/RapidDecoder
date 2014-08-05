package rapid.decoder.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquaredImageView extends ImageView {
    public SquaredImageView(Context context) {
        super(context);
    }

    public SquaredImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquaredImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
//                MeasureSpec.getSize(widthMeasureSpec),
//                MeasureSpec.getMode(heightMeasureSpec)
//        );
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
