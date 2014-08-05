package rapid.decoder.frame;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;

public interface FramedDecoderCreator {
    Decodable createFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight);
}
