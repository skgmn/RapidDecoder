#include "sampler.h"

#include <stdlib.h>

// opaque_sampler

opaque_sampler::opaque_sampler(int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format)
{
    m_sample_size = sample_size;
    m_shift_count = get_shift_count(sample_size);
    m_shift_count_2 = m_shift_count * 2;
    m_width = sampled_width;
    m_rows = 0;
    m_filter = filter;
    m_composer = format.composer;

    m_red = new unsigned int [sampled_width];
    m_green = new unsigned int [sampled_width];
    m_blue = new unsigned int [sampled_width];

    memset(m_red, 0, sampled_width * sizeof(unsigned int));
    memset(m_green, 0, sampled_width * sizeof(unsigned int));
    memset(m_blue, 0, sampled_width * sizeof(unsigned int));
}

unsigned int opaque_sampler::get_shift_count(unsigned int n)
{
    unsigned int zeros = 0;
    while (n > 1)
    {
        n >>= 1;
        ++zeros;
    }

    return zeros;
}

bool opaque_sampler::sample(const unsigned char* pixels, int offset, int count, unsigned char* out)
{
    pixels = &pixels[offset * 3];

    if (m_filter)
    {
        for (int i = 0; i < count; ++i)
        {
            int col = i >> m_shift_count;
            if (col >= m_width) break;

            m_red[col] += pixels[0];
            m_green[col] += pixels[1];
            m_blue[col] += pixels[2];
            pixels += 3;
        }

        if (++m_rows == m_sample_size)
        {
            emit_square(out);
            m_rows = 0;

            return true;
        }
        else
        {
            return false;
        }
    }
    else
    {
        bool result;

        if (m_rows == 0)
        {
            for (int i = 0; i < m_width; ++i)
            {
                m_composer(out, 0xff, pixels[0], pixels[1], pixels[2]);
                pixels += 3 * m_sample_size;
            }
            result = true;
        }
        else
        {
            result = false;
        }

        if (++m_rows == m_sample_size)
        {
            m_rows = 0;
        }

        return result;
    }
}

void opaque_sampler::emit_square(unsigned char* out)
{
    for (int i = 0; i < m_width; ++i)
    {
        m_composer(out, 0xff,
            m_red[i] >> m_shift_count_2,
            m_green[i] >> m_shift_count_2,
            m_blue[i] >> m_shift_count_2);
        m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

void opaque_sampler::emit(unsigned char* out)
{
    int divisor = m_sample_size * m_rows;
    for (int i = 0; i < m_width; ++i)
    {
        m_composer(out, 0xff,
            m_red[i] / divisor,
            m_green[i] / divisor,
            m_blue[i] / divisor);
        m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

void opaque_sampler::finish(unsigned char* out)
{
    if (m_filter && m_rows > 0)
    {
        emit(out);
        m_rows = 0;
    }
}

opaque_sampler::~opaque_sampler()
{
    delete [] m_red;
    delete [] m_green;
    delete [] m_blue;
}

// sampler

sampler::sampler(int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format)
    : opaque_sampler(sampled_width, sample_size, filter, format)
{
    m_alpha = new unsigned int [sampled_width];
    memset(m_alpha, 0, sampled_width * sizeof(unsigned int));
}

bool sampler::sample(const unsigned char* pixels, int offset, int count, unsigned char* out)
{
    pixels = &pixels[offset * 4];

    if (m_filter)
    {
        for (int i = 0; i < count; ++i)
        {
            int col = i >> m_shift_count;
            if (col >= m_width) break;

            m_red[col] += pixels[0];
            m_green[col] += pixels[1];
            m_blue[col] += pixels[2];
            m_alpha[col] += pixels[3];
            pixels += 4;
        }

        if (++m_rows == m_sample_size)
        {
            emit_square(out);
            m_rows = 0;

            return true;
        }
        else
        {
            return false;
        }
    }
    else
    {
        bool result;

        if (m_rows == 0)
        {
            for (int i = 0; i < m_width; ++i)
            {
                m_composer(out, pixels[3], pixels[0], pixels[1], pixels[2]);
                pixels += 4 * m_sample_size;
            }
            result = true;
        }
        else
        {
            result = false;
        }

        if (++m_rows == m_sample_size)
        {
            m_rows = 0;
        }

        return result;
    }
}

void sampler::emit_square(unsigned char* out)
{
    for (int i = 0; i < m_width; ++i)
    {
        m_composer(out, m_alpha[i] >> m_shift_count_2,
            m_red[i] >> m_shift_count_2,
            m_green[i] >> m_shift_count_2,
            m_blue[i] >> m_shift_count_2);
        m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

void sampler::emit(unsigned char* out)
{
    int divisor = m_sample_size * m_rows;
    for (int i = 0; i < m_width; ++i)
    {
        m_composer(out, m_alpha[i] / divisor,
            m_red[i] / divisor,
            m_green[i] / divisor,
            m_blue[i] / divisor);
        m_alpha[i] = m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

sampler::~sampler()
{
    delete [] m_alpha;
}
