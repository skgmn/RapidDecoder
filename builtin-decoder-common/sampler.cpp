#include "sampler.h"

#include <stdlib.h>

// opaque_sampler

opaque_sampler::opaque_sampler(int width, int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format)
{
    m_sample_size = sample_size;
    m_shift_count = get_shift_count(sample_size);
    m_shift_count_2 = m_shift_count * 2;
    m_width = width;
    m_sampled_width = sampled_width;
    m_remainder = width % sample_size;
    m_rows = 0;
    m_filter = filter;
    m_composer = format.composer;

    int bypp = get_bytes_per_pixel();
    m_red = new unsigned int [bypp * sampled_width];
    m_green = &m_red[sampled_width];
    m_blue = &m_red[sampled_width * 2];

    memset(m_red, 0, bypp * sampled_width * sizeof(unsigned int));
}

int opaque_sampler::get_bytes_per_pixel()
{
    return 3;
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

bool opaque_sampler::sample(const unsigned char* pixels, int offset, unsigned char* out)
{
    pixels = &pixels[offset * 3];

    if (m_filter)
    {
        for (int i = 0; i < m_width; ++i)
        {
            int col = i >> m_shift_count;
            if (col >= m_sampled_width) break;

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
            for (int i = 0; i < m_sampled_width; ++i)
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

        m_rows = (m_rows + 1) % m_sample_size;

        return result;
    }
}

void opaque_sampler::emit_square(unsigned char* out)
{
    for (int i = 0; i < m_sampled_width; ++i)
    {
        if (m_remainder != 0 && i == m_sampled_width - 1)
        {
            int divisor = m_remainder * m_rows;
            m_composer(out, 0xff,
                    m_red[i] / divisor,
                    m_green[i] / divisor,
                    m_blue[i] / divisor);
        }
        else
        {
            m_composer(out, 0xff,
                    m_red[i] >> m_shift_count_2,
                    m_green[i] >> m_shift_count_2,
                    m_blue[i] >> m_shift_count_2);
        }

        m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

void opaque_sampler::emit(unsigned char* out)
{
    int divisor = m_sample_size * m_rows;
    for (int i = 0; i < m_sampled_width; ++i)
    {
        if (m_remainder != 0 && i == m_sampled_width - 1)
        {
            divisor = m_remainder * m_rows;
        }

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
}

// sampler

sampler::sampler(int width, int sampled_width, unsigned int sample_size, bool filter, const pixel_format& format)
    : opaque_sampler(width, sampled_width, sample_size, filter, format)
{
    m_alpha = &m_red[sampled_width * 3];
}

int sampler::get_bytes_per_pixel()
{
    return 4;
}

bool sampler::sample(const unsigned char* pixels, int offset, unsigned char* out)
{
    pixels = &pixels[offset * 4];

    if (m_filter)
    {
        for (int i = 0; i < m_width; ++i)
        {
            int col = i >> m_shift_count;
            if (col >= m_sampled_width) break;

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
            for (int i = 0; i < m_sampled_width; ++i)
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

        m_rows = (m_rows + 1) % m_sample_size;

        return result;
    }
}

void sampler::emit_square(unsigned char* out)
{
    for (int i = 0; i < m_sampled_width; ++i)
    {
        if (m_remainder != 0 && i == m_sampled_width - 1)
        {
            int divisor = m_remainder * m_rows;
            m_composer(out, m_alpha[i] / divisor,
                    m_red[i] / divisor,
                    m_green[i] / divisor,
                    m_blue[i] / divisor);
        }
        else
        {
            m_composer(out, m_alpha[i] >> m_shift_count_2,
                m_red[i] >> m_shift_count_2,
                m_green[i] >> m_shift_count_2,
                m_blue[i] >> m_shift_count_2);
        }

        m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

void sampler::emit(unsigned char* out)
{
    int divisor = m_sample_size * m_rows;
    for (int i = 0; i < m_sampled_width; ++i)
    {
        if (m_remainder != 0 && i == m_sampled_width - 1)
        {
            divisor = m_remainder * m_rows;
        }

        m_composer(out, m_alpha[i] / divisor,
            m_red[i] / divisor,
            m_green[i] / divisor,
            m_blue[i] / divisor);
        m_alpha[i] = m_red[i] = m_green[i] = m_blue[i] = 0;
    }
}

sampler::~sampler()
{
}