#ifndef SAMPLER_H
#define SAMPLER_H

#include "pixelcomposer.h"

class opaque_sampler
{
public:
    opaque_sampler(int width, int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format);
    virtual ~opaque_sampler();

    virtual bool sample(const unsigned char* pixels, int offset, unsigned char* out);
    void finish(unsigned char* out);

protected:
    unsigned int* m_red;
    unsigned int* m_green;
    unsigned int* m_blue;
    unsigned int m_sample_size;
    unsigned int m_shift_count_2;
    unsigned int m_shift_count;
    int m_width;
    int m_sampled_width;
    int m_remainder;
    int m_rows;
    bool m_filter;
    pixel_composer m_composer;

    virtual int get_bytes_per_pixel();
    virtual void emit_square(unsigned char* out);
    virtual void emit(unsigned char* out);

    static unsigned int get_shift_count(unsigned int n);
};

class sampler : public opaque_sampler
{
public:
    sampler(int width, int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format);
    ~sampler();

    bool sample(const unsigned char* pixels, int offset, unsigned char* out);

protected:
    unsigned int* m_alpha;

    int get_bytes_per_pixel();
    void emit_square(unsigned char* out);
    void emit(unsigned char* out);
};

#endif // SAMPLER_H