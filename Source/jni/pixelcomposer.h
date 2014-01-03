#ifndef PIXELCOMPOSER_H
#define PIXELCOMPOSER_H

typedef void (*pixel_composer)(unsigned char*& dest, unsigned char a, unsigned char r, unsigned char g, unsigned char b);

class pixel_format
{
public:
    pixel_composer composer;
    int bpp;

    inline pixel_format() {}

    inline pixel_format(pixel_composer composer, int bpp)
    {
        this->composer = composer;
        this->bpp = bpp;
    }

    inline bool operator==(const pixel_format& rhs)
    {
        return composer == rhs.composer;
    }
};

extern pixel_format RGB565;
extern pixel_format RGBA4444;
extern pixel_format RGB888;
extern pixel_format ARGB8888;
extern pixel_format RGBA8888;

#endif // PIXELCOMPOSER_H