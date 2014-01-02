#ifndef PIXELCOMPOSER_H
#define PIXELCOMPOSER_H

typedef void (*pixel_composer)(unsigned char*& dest, unsigned char a, unsigned char r, unsigned char g, unsigned char b);

class pixel_format
{
public:
    pixel_composer composer;
    int bpp;

    pixel_format(pixel_composer composer, int bpp)
    {
        this->composer = composer;
        this->bpp = bpp;
    }
};

extern pixel_format RGB565;
extern pixel_format ARGB4444;
extern pixel_format RGB888;
extern pixel_format ARGB;
extern pixel_format RGBA;

#endif // PIXELCOMPOSER_H