#include "pixelcomposer.h"

typedef unsigned char uint8;
typedef unsigned short uint16;
typedef unsigned long uint32;

void rgb565_composer(uint8*& dest, uint8 a, uint8 r, uint8 g, uint8 b)
{
    uint16* p = (uint16*)dest;
    *p = (uint16)(((r & 0xf8) << 8) | ((g & 0xfc) << 3) | ((b & 0xf8) >> 3));
    dest += 2;
}

void argb4444_composer(uint8*& dest, uint8 a, uint8 r, uint8 g, uint8 b)
{
    uint16* p = (uint16*)dest;
    *p = (uint16)(((a & 0xf0) << 8) | (r >> 4) | (g & 0xf0) | ((b & 0xf0) << 4));
    dest += 2;
}

void rgb888_composer(uint8*& dest, uint8 a, uint8 r, uint8 g, uint8 b)
{
    dest[0] = r;
    dest[1] = g;
    dest[2] = b;
    dest += 3;
}

void argb_composer(uint8*& dest, uint8 a, uint8 r, uint8 g, uint8 b)
{
    uint32* p = (uint32*)dest;
    *p = (b << 24) | (g << 16) | (r << 8) | a;
    dest += 4;
}

void rgba_composer(uint8*& dest, uint8 a, uint8 r, uint8 g, uint8 b)
{
    uint32* p = (uint32*)dest;
    *p = (a << 24) | (b << 16) | (g << 8) | r;
    dest += 4;
}

pixel_format RGB565(rgb565_composer, 2);
pixel_format RGBA4444(argb4444_composer, 2);
pixel_format RGB888(rgb888_composer, 3);
pixel_format ARGB8888(argb_composer, 4);
pixel_format RGBA8888(rgba_composer, 4);