package rapid.decoder;

class SimpleBitmapMeta implements BitmapMeta {
    public int width;
    public int height;

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }
}
