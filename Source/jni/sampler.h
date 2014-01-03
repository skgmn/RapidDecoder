#ifndef SAMPLER_H
#define SAMPLER_H

#include "pixelcomposer.h"

class opaque_sampler
{
public:
    opaque_sampler(int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format);
    virtual ~opaque_sampler();

    virtual bool sample(const unsigned char* pixels, int offset, int count, unsigned char* out);
    void finish(unsigned char* out);

private:
    unsigned int* m_red;
    unsigned int* m_green;
    unsigned int* m_blue;
    unsigned int m_sample_size;
    unsigned int m_shift_count;
    unsigned int m_shift_count_2;
    int m_width;
    int m_rows;
    bool m_filter;
    pixel_composer m_composer;

    virtual void emit_square(unsigned char* out);
    virtual void emit(unsigned char* out);
    int get_shift_count(int rows);

    static unsigned int get_shift_count(unsigned int n);
};

class sampler : public opaque_sampler
{
};

#endif // SAMPLER_H