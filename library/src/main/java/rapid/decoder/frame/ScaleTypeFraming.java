package rapid.decoder.frame;

import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;

public class ScaleTypeFraming extends FramingAlgorithm {
    private ImageView.ScaleType mScaleType;

    public ScaleTypeFraming(ImageView.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    @Override
    public Decodable createFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        return FramedDecoder.newInstance(decoder, frameWidth, frameHeight, mScaleType);
    }
}
