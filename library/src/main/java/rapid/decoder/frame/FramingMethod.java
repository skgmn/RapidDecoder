package rapid.decoder.frame;

import rapid.decoder.BitmapDecoder;

public abstract class FramingMethod {
    public abstract FramedDecoder createFramedDecoder(BitmapDecoder decoder, int frameWidth,
                                                  int frameHeight);
}
