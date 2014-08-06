package rapid.decoder.frame;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;

public abstract class FramingAlgorithm {
    public abstract Decodable createFramedDecoder(BitmapDecoder decoder, int frameWidth,
                                                  int frameHeight);
}
