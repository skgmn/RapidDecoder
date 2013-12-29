/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package agu.bitmap.jpeg;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Matthias Mann
 */
class Huffman {

    static final int FAST_BITS = 9;
    static final int FAST_MASK = (1 << FAST_BITS) - 1;

    final byte[] fast;
    final byte[] values;
    final byte[] size;
    final int[] maxCode;
    final int[] delta;

    public Huffman(int[] count) throws IOException {
        int numSymbols = 0;
        for(int i=0 ; i<16 ; i++) {
            numSymbols += count[i];
        }

        fast = new byte[1 << FAST_BITS];
        values = new byte[numSymbols];
        size = new byte[numSymbols];
        maxCode = new int[18];
        delta = new int[17];

        for(int i=0,k=0 ; i<16 ; i++) {
            for(int j=0 ; j<count[i] ; j++) {
                size[k++] = (byte)(i+1);
            }
        }

        final int[] code = new int[256];

        int i = 1;
        int k = 0;
        for(int c=0 ; i<=16 ; i++) {
            delta[i] = k - c;
            if(k < numSymbols && size[k] == i) {
                do {
                    code[k++] = c++;
                }while(k < numSymbols && size[k] == i);
                if(c-1 >= (1<<i)) {
                    throw new IOException("Bad code length");
                }
            }
            maxCode[i] = c << (16 - i);
            c <<= 1;
        }
        maxCode[i] = Integer.MAX_VALUE;

        Arrays.fill(fast, (byte)-1);
        for(i=0 ; i<k ; i++) {
            int s = size[i];
            if(s <= FAST_BITS) {
                int c = code[i] << (FAST_BITS - s);
                int m = 1 << (FAST_BITS - s);
                for(int j=0 ; j<m ; j++) {
                    fast[c+j] = (byte)i;
                }
            }
        }
    }

    public int getNumSymbols() {
        return values.length;
    }
}
