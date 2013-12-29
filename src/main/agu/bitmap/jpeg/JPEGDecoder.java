/*
 * Copyright (c) 2008-2012, Matthias Mann
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * A pure Java JPEG decoder
 *
 * Partly based on code from Sean Barrett
 *
 * @author Matthias Mann
 */
public class JPEGDecoder {
    
    static final int MARKER_NONE = 0xFF;
    
    private final InputStream is;
    private final byte[] inputBuffer;
    private int inputBufferPos;
    private int inputBufferValid;
    private boolean ignoreIOerror;

    private boolean headerDecoded;
    private boolean insideSOS;
    private boolean foundEOI;
    private int currentMCURow;
    
    private final IDCT_2D idct2D;
    private final short[] data;
    private final Huffman[] huffmanTables;
    private final byte[][] dequant;

    private Component[] components;
    private Component[] order;
    
    private int codeBuffer;
    private int codeBits;
    private int marker = MARKER_NONE;
    private int restartInterval;
    private int todo;
    private int mcuCountX;
    private int mcuCountY;
    private int imageWidth;
    private int imageHeight;
    private int imgHMax;
    private int imgVMax;
    private boolean nomore;

    private byte[][] decodeTmp;
    private byte[][] upsampleTmp;

    /**
     * Constructs a new JPEGDecoder for the specified InputStream.
     * The input stream is not closed by this decoder and must be closed by the
     * calling code.
     * The JPEG header is only read when calling {@link #decodeHeader() } or
     * {@link #startDecode() }
     *
     * @param is the InputStream containing the JPG data
     */
    public JPEGDecoder(InputStream is) {
        this.is = is;
        this.inputBuffer = new byte[4096];
        
        this.idct2D = new IDCT_2D();
        this.data = new short[64];
        this.huffmanTables = new Huffman[8];
        this.dequant = new byte[4][64];
    }

    public boolean isIgnoreIOerror() {
        return ignoreIOerror;
    }

    /**
     * Controls the behavior on IO errors.
     * This must be called before {@link #decodeHeader() }
     *
     * @param ignoreIOerror if true IO errors are ignored
     */
    public void setIgnoreIOerror(boolean ignoreIOerror) {
        if(headerDecoded) {
            throw new IllegalStateException("header already decoded");
        }
        this.ignoreIOerror = ignoreIOerror;
    }

    /**
     * Decodes the JPEG header. This must be called before the image size can be queried.
     * 
     * @throws IOException if an IO error occurred
     */
    public void decodeHeader() throws IOException {
        if(!headerDecoded) {
            headerDecoded = true;

            int m = getMarker();
            if(m != 0xD8) {
                throw new IOException("no SOI");
            }
            m = getMarker();
            while(m != 0xC0 && m != 0xC1) { // SOF
                processMarker(m);
                m = getMarker();
                while(m == MARKER_NONE) {
                    m = getMarker();
                }
            }

            processSOF();
        }
    }

    /**
     * Returns the width of the image.
     * {@link #decodeHeader() } must be called before the image width can be queried.
     *
     * @return the width of the JPEG.
     */
    public int getImageWidth() {
        ensureHeaderDecoded();
        return imageWidth;
    }

    /**
     * Returns the height of the image.
     * {@link #decodeHeader() } must be called before the image height can be queried.
     *
     * @return the height of the JPEG.
     */
    public int getImageHeight() {
        ensureHeaderDecoded();
        return imageHeight;
    }

    /**
     * Returns the number of color components.
     * {@link #decodeHeader() } must be called before the color components can be queried.
     *
     * @return 1 for gray scale, 3 for color
     */
    public int getNumComponents() {
        ensureHeaderDecoded();
        return components.length;
    }

    /**
     * Returns the informations about the specific color component.
     * {@link #decodeHeader() } must be called before the color components can be queried.
     *
     * @param idx the color component. Must be &lt; then {@link #getNumComponents() }
     * @return the component information
     */
    public Component getComponent(int idx) {
        ensureHeaderDecoded();
        return components[idx];
    }

    /**
     * Returns the height of a MCU row. This is the smallest granularity for
     * the raw decode API.
     * {@link #decodeHeader() } must be called before the MCU row height can be queried.
     *
     * @return the height of an MCU row.
     */
    public int getMCURowHeight() {
        ensureHeaderDecoded();
        return imgVMax * 8;
    }

    /**
     * Returns the number of MCU rows.
     * {@link #decodeHeader() } must be called before the number of MCU rows can be queried.
     *
     * @return the number of MCU rows.
     * @see #getMCURowHeight()
     */
    public int getNumMCURows() {
        ensureHeaderDecoded();
        return mcuCountY;
    }

    /**
     * Returns the number of MCU columns.
     * {@link #decodeHeader() } must be called before the number of MCU columns can be queried.
     *
     * @return the number of MCU columns.
     * @see #decodeDCTCoeffs(java.nio.ShortBuffer[], int) 
     */
    public int getNumMCUColumns() {
        ensureHeaderDecoded();
        return mcuCountX;
    }

    /**
     * Starts the decode process. This will advance the JPEG stream to the start
     * of the image data. It also checks if that JPEG file can be decoded by this
     * library.
     *
     * @return true if the JPEG can be decoded.
     * @throws IOException if an IO error occurred
     */
    public boolean startDecode() throws IOException {
        if(insideSOS) {
            throw new IllegalStateException("decode already started");
        }
        if(foundEOI) {
            return false;
        }

        decodeHeader();
        int m = getMarker();
        while(m != 0xD9) {  // EOI
            if(m == 0xDA) { // SOS
                processScanHeader();
                insideSOS = true;
                currentMCURow = 0;
                reset();
                return true;
            } else {
                processMarker(m);
            }
            m = getMarker();
        }

        foundEOI = true;
        return false;
    }

    /**
     * Decodes a number of MCU rows into the specified ByteBuffer as RGBA data.
     * {@link #startDecode() } must be called before this method.
     *
     * <p>The first decoded line is placed at {@code dst.position() },
     * the second line at {@code dst.position() + stride } and so on. After decoding
     * the buffer position is at {@code dst.position() + n*stride } where n is
     * the number of decoded lines which might be less than
     * {@code numMCURows * getMCURowHeight() } at the end of the image.</p>
     * 
     * @param dst the target ByteBuffer
     * @param stride the distance in bytes from the start of one line to the start of the next.
     *               The absolute value should be &gt;= {@link #getImageWidth() }*4, can also be negative.
     * @param numMCURows the number of MCU rows to decode.
     * @throws IOException if an IO error occurred
     * @throws IllegalArgumentException if numMCURows is invalid
     * @throws IllegalStateException if {@link #startDecode() } has not been called
     * @throws UnsupportedOperationException if the JPEG is not a color JPEG
     * @see #getNumComponents()
     * @see #getNumMCURows() 
     */
    public void decodeRGB(IntBuffer dst, int stride, int x, int width, int skipRows, int numMCURows) throws IOException {
        if(!insideSOS) {
            throw new IllegalStateException("decode not started");
        }

        if(numMCURows <= 0 || currentMCURow + numMCURows > mcuCountY) {
            throw new IllegalArgumentException("numMCURows");
        }

        if(order.length != 3) {
            throw new UnsupportedOperationException("RGB decode only supported for 3 channels");
        }

        final int YUVstride = mcuCountX * imgHMax * 8;
        final boolean requiresUpsampling = allocateDecodeTmp(YUVstride);

        final byte[] YtoRGB = (order[0].upsampler != 0) ? upsampleTmp[0] : decodeTmp[0];
        final byte[] UtoRGB = (order[1].upsampler != 0) ? upsampleTmp[1] : decodeTmp[1];
        final byte[] VtoRGB = (order[2].upsampler != 0) ? upsampleTmp[2] : decodeTmp[2];
        
        for(int j=0 ; j<numMCURows ; j++) {
            decodeMCUrow();

            if(requiresUpsampling) {
                doUpsampling(YUVstride);
            }
            
            int outPos = dst.position();
            int n = imgVMax*8;
            n = Math.min(imageHeight - (currentMCURow-1)*n, n);
            for(int i=0 ; i<n ; i++) {
            	if (skipRows > 0) {
            		--skipRows;
            	} else {
            		YUVtoRGB(dst, outPos, YtoRGB, UtoRGB, VtoRGB, i * YUVstride + x, width);
            	}
                outPos += stride;
            }
            dst.position(outPos);

            if(marker != MARKER_NONE) {
                break;
            }
        }
        
        checkDecodeEnd();
    }

    /**
     * Decodes each color component of the JPEG file separately into a separate
     * ByteBuffer. The number of buffers must match the number of color channels.
     * Each color channel can have a different sub sampling factor.
     *
     * @param buffer the ByteBuffers for each color component
     * @param strides the distance in bytes from the start of one line to the start of the next for each color component
     * @param numMCURows the number of MCU rows to decode.
     * @throws IOException if an IO error occurred
     * @throws IllegalArgumentException if numMCURows is invalid, or if the number of buffers / strides is not enough
     * @throws IllegalStateException if {@link #startDecode() } has not been called
     * @throws UnsupportedOperationException if the color components are not in the same SOS chunk
     * @see #getNumComponents()
     * @see #getNumMCURows()
     */
    public void decodeRAW(ByteBuffer[] buffer, int[] strides, int numMCURows) throws IOException {
        if(!insideSOS) {
            throw new IllegalStateException("decode not started");
        }

        if(numMCURows <= 0 || currentMCURow + numMCURows > mcuCountY) {
            throw new IllegalArgumentException("numMCURows");
        }
        
        int scanN = order.length;
        if(scanN != components.length) {
            throw new UnsupportedOperationException("for RAW decode all components need to be decoded at once");
        }
        if(scanN > buffer.length || scanN > strides.length) {
            throw new IllegalArgumentException("not enough buffers");
        }

        for(int compIdx=0 ; compIdx<scanN ; compIdx++) {
            order[compIdx].outPos = buffer[compIdx].position();
        }

        outer: for(int j=0 ; j<numMCURows ; j++) {
            ++currentMCURow;
            for(int i=0 ; i<mcuCountX ; i++) {
                for(int compIdx=0 ; compIdx<scanN ; compIdx++) {
                    Component c = order[compIdx];
                    int outStride = strides[compIdx];
                    int outPosY = c.outPos + 8*(i*c.blocksPerMCUHorz + j*c.blocksPerMCUVert*outStride);

                    for(int y=0 ; y<c.blocksPerMCUVert ; y++,outPosY+=8*outStride) {
                        for(int x=0,outPos=outPosY ; x<c.blocksPerMCUHorz ; x++,outPos+=8) {
                            try {
                                decodeBlock(data, c);
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                throwBadHuffmanCode();
                            }
                            idct2D.compute(buffer[compIdx], outPos, outStride, data);
                        }
                    }
                }
                if(--todo <= 0) {
                    if(!checkRestart()) {
                        break outer;
                    }
                }
            }
        }

        checkDecodeEnd();

        for(int compIdx=0 ; compIdx<scanN ; compIdx++) {
            Component c = order[compIdx];
            buffer[compIdx].position(c.outPos + numMCURows * c.blocksPerMCUVert * 8 * strides[compIdx]);
        }
    }

    /**
     * Decodes the dequantizied DCT coefficients into a buffer per color component.
     * The number of buffers must match the number of color channels.
     * Each color channel can have a different sub sampling factor.
     * 
     * @param buffer the ShortBuffers for each color component
     * @param numMCURows the number of MCU rows to decode.
     * @throws IOException if an IO error occurred
     * @throws IllegalArgumentException if numMCURows is invalid, or if the number of buffers / strides is not enough
     * @throws IllegalStateException if {@link #startDecode() } has not been called
     * @throws UnsupportedOperationException if the color components are not in the same SOS chunk
     * @see #getNumComponents()
     * @see #getNumMCURows()
     */
    public void decodeDCTCoeffs(ShortBuffer[] buffer, int numMCURows) throws IOException {
        if(!insideSOS) {
            throw new IllegalStateException("decode not started");
        }

        if(numMCURows <= 0 || currentMCURow + numMCURows > mcuCountY) {
            throw new IllegalArgumentException("numMCURows");
        }

        int scanN = order.length;
        if(scanN != components.length) {
            throw new UnsupportedOperationException("for RAW decode all components need to be decoded at once");
        }
        if(scanN > buffer.length) {
            throw new IllegalArgumentException("not enough buffers");
        }

        for(int compIdx=0 ; compIdx<scanN ; compIdx++) {
            order[compIdx].outPos = buffer[compIdx].position();
        }

        outer: for(int j=0 ; j<numMCURows ; j++) {
            ++currentMCURow;
            for(int i=0 ; i<mcuCountX ; i++) {
                for(int compIdx=0 ; compIdx<scanN ; compIdx++) {
                    Component c = order[compIdx];
                    ShortBuffer sb = buffer[compIdx];
                    int outStride = 64 * c.blocksPerMCUHorz * mcuCountX;
                    int outPos = c.outPos + 64*i*c.blocksPerMCUHorz + j*c.blocksPerMCUVert*outStride;

                    for(int y=0 ; y<c.blocksPerMCUVert ; y++) {
                        sb.position(outPos);
                        for(int x=0 ; x<c.blocksPerMCUHorz ; x++) {
                            try {
                                decodeBlock(data, c);
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                throwBadHuffmanCode();
                            }
                            sb.put(data);
                        }
                        outPos += outStride;
                    }
                }
                if(--todo <= 0) {
                    if(!checkRestart()) {
                        break outer;
                    }
                }
            }
        }

        checkDecodeEnd();

        for(int compIdx=0 ; compIdx<scanN ; compIdx++) {
            Component c = order[compIdx];
            int outStride = 64 * c.blocksPerMCUHorz * mcuCountX;
            buffer[compIdx].position(c.outPos + numMCURows * c.blocksPerMCUVert * outStride);
        }
    }

    private void checkDecodeEnd() throws IOException {
        if(currentMCURow >= mcuCountY || marker != MARKER_NONE) {
            insideSOS = false;
            if(marker == MARKER_NONE) {
                skipPadding();
            }
        }
    }

    private void fetch() throws IOException {
        try {
            inputBufferPos = 0;
            inputBufferValid = is.read(inputBuffer);
            
            if(inputBufferValid <= 0) {
                throw new EOFException();
            }
        } catch (IOException ex) {
            inputBufferValid = 2;
            inputBuffer[0] = (byte)0xFF;
            inputBuffer[1] = (byte)0xD9;    // EOI

            if(!ignoreIOerror) {
                throw ex;
            }
        }
    }

    private void read(byte[] buf, int off, int len) throws IOException {
        while(len > 0) {
            int avail = inputBufferValid - inputBufferPos;
            if(avail == 0) {
                fetch();
                continue;
            }
            int copy = (avail > len) ? len : avail;
            System.arraycopy(inputBuffer, inputBufferPos, buf, off, copy);
            off += copy;
            len -= copy;
            inputBufferPos += copy;
        }
    }

    private int getU8() throws IOException {
        if(inputBufferPos == inputBufferValid) {
            fetch();
        }
        return inputBuffer[inputBufferPos++] & 255;
    }

    private int getU16() throws IOException {
        int t = getU8();
        return (t << 8) | getU8();
    }

    private void skip(int amount) throws IOException {
        while(amount > 0) {
            int inputBufferRemaining = inputBufferValid - inputBufferPos;
            if(amount > inputBufferRemaining) {
                amount -= inputBufferRemaining;
                fetch();
            } else {
                inputBufferPos += amount;
                return;
            }
        }
    }

    private void growBufferCheckMarker() throws IOException {
        int c = getU8();
        if(c != 0) {
            marker = c;
            nomore = true;
        }
    }
    
    private void growBufferUnsafe() throws IOException {
        do {
            int b = 0;
            if(!nomore) {
                b = getU8();
                if(b == 0xff) {
                    growBufferCheckMarker();
                }
            }
            codeBuffer |= b << (24 - codeBits);
            codeBits   += 8;
        } while(codeBits <= 24);
    }

    private int decode(Huffman h) throws IOException {
        if(codeBits < 16) {
            growBufferUnsafe();
        }
        int k = h.fast[codeBuffer >>> (32 - Huffman.FAST_BITS)] & 255;
        if(k < 0xFF) {
            int s = h.size[k];
            codeBuffer <<= s;
            codeBits    -= s;
            return h.values[k] & 255;
        }
        return decodeSlow(h);
    }

    private int decodeSlow(Huffman h) throws IOException {
        int temp = codeBuffer >>> 16;
        int s = Huffman.FAST_BITS + 1;

        while(temp >= h.maxCode[s]) {
            s++;
        }

        int k = (temp >>> (16 - s)) + h.delta[s];
        codeBuffer <<= s;
        codeBits    -= s;
        return h.values[k] & 255;
    }

    private int extendReceive(int n) throws IOException {
        if(codeBits < 24) {
            growBufferUnsafe();
        }

        int k = codeBuffer >>> (32 - n);
        codeBuffer <<= n;
        codeBits    -= n;

        int limit = 1 << (n-1);
        if(k < limit) {
            k -= limit*2 - 1;
        }
        return k;
    }

    private void decodeBlock(short[] data, Component c) throws IOException {
        Arrays.fill(data, (short)0);

        final byte[] dq = c.dequant;

        {
            int t = decode(c.huffDC);
            int dc = c.dcPred;
            if(t > 0) {
                dc += extendReceive(t);
                c.dcPred = dc;
            }

            data[0] = (short)(dc * (dq[0] & 0xFF));
        }

        final Huffman hac = c.huffAC;

        int k = 1;
        do {
            int rs = decode(hac);
            k += rs >> 4;
            int s = rs & 15;
            if(s != 0) {
                int v = extendReceive(s) * (dq[k] & 0xFF);
                data[dezigzag[k]] = (short)v;
            } else if(rs != 0xF0) {
                break;
            }
        } while(++k < 64);
    }


    private void skipBlock(Component c) throws IOException {
        final int t = decode(c.huffDC);
        if(t > 0) {
        	c.dcPred += extendReceive(t);
        }

        final Huffman hac = c.huffAC;

        int k = 1;
        do {
            int rs = decode(hac);
            k += rs >> 4;
            int s = rs & 15;
            if(s != 0) {
                extendReceive(s);
            } else if(rs != 0xF0) {
                break;
            }
        } while(++k < 64);
    }

    private static void throwBadHuffmanCode() throws IOException {
        throw new IOException("Bad huffman code");
    }

    private int getMarker() throws IOException {
        int m = marker;
        if(m != MARKER_NONE) {
            marker = MARKER_NONE;
            return m;
        }
        m = getU8();
        if(m != 0xFF) {
            return MARKER_NONE;
        }
        do {
            m = getU8();
        }while(m == 0xFF);
        return m;
    }

    private void reset() {
        codeBits = 0;
        codeBuffer = 0;
        nomore = false;
        marker = MARKER_NONE;

        if(restartInterval != 0) {
            todo = restartInterval;
        } else {
            todo = Integer.MAX_VALUE;
        }

        for(Component c : components) {
            c.dcPred = 0;
        }
    }

    private boolean checkRestart() throws IOException {
        if(codeBits < 24) {
            growBufferUnsafe();
        }
        if(marker >= 0xD0 && marker <= 0xD7) {
            reset();
            return true;
        }
        return false;
    }

    private void processMarker(int marker) throws IOException {
        if(marker >= 0xE0 && (marker <= 0xEF || marker == 0xFE)) {
            int l = getU16() - 2;
            if(l < 0) {
                throw new IOException("bad length");
            }
            skip(l);
            return;
        }

        switch(marker) {
            case MARKER_NONE:
                throw new IOException("Expected marker");

            case 0xC2:      // SOF - progressive
                throw new IOException("Progressive JPEG not supported");

            case 0xDD:      // DRI - specify restart interval
                if(getU16() != 4) {
                    throw new IOException("bad DRI length");
                }
                restartInterval = getU16();
                break;

            case 0xDB: {    // DQT - define dequant table
                int l = getU16() - 2;
                while(l >= 65) {
                    int q = getU8();
                    int p = q >> 4;
                    int t = q & 15;
                    if(p != 0) {
                        throw new IOException("bad DQT type");
                    }
                    if(t > 3) {
                        throw new IOException("bad DQT table");
                    }
                    read(dequant[t], 0, 64);
                    l -= 65;
                }
                if(l != 0) {
                    throw new IOException("bad DQT length");
                }
                break;
            }

            case 0xC4: {    // DHT - define huffman table
                int l = getU16() - 2;
                while(l > 17) {
                    int q = getU8();
                    int tc = q >> 4;
                    int th = q & 15;
                    if(tc > 1 || th > 3) {
                        throw new IOException("bad DHT header");
                    }
                    int[] tmp = idct2D.tmp2D;   // reuse memory
                    for(int i=0 ; i<16 ; i++) {
                        tmp[i] = getU8();
                    }
                    Huffman h = new Huffman(tmp);
                    int m = h.getNumSymbols();
                    l -= 17 + m;
                    if(l < 0) {
                        throw new IOException("bad DHT length");
                    }
                    read(h.values, 0, m);
                    huffmanTables[tc*4 + th] = h;
                }
                if(l != 0) {
                    throw new IOException("bad DHT length");
                }
                break;
            }
            
            default:
                throw new IOException("Unknown marker: " + Integer.toHexString(marker));
        }
    }

    private void skipPadding() throws IOException {
        int x;
        do {
            x = getU8();
        } while(x == 0);

        if(x == 0xFF) {
            marker = getU8();
        }
    }

    private void processScanHeader() throws IOException {
        int ls = getU16();
        int scanN = getU8();

        if(scanN < 1 || scanN > 4) {
            throw new IOException("bad SOS component count");
        }
        if(ls != 6+2*scanN) {
            throw new IOException("bad SOS length");
        }

        order = new Component[scanN];
        for(int i=0 ; i<scanN ; i++) {
            int id = getU8();
            int q = getU8();
            for(Component c : components) {
                if(c.id == id) {
                    int hd = q >> 4;
                    int ha = q & 15;
                    if(hd > 3 || ha > 3) {
                        throw new IOException("bad huffman table index");
                    }
                    c.huffDC = huffmanTables[hd];
                    c.huffAC = huffmanTables[ha + 4];
                    if(c.huffDC == null || c.huffAC == null) {
                        throw new IOException("bad huffman table index");
                    }
                    order[i] = c;
                    break;
                }
            }
            if(order[i] == null) {
                throw new IOException("unknown color component");
            }
        }
        
        if(getU8() != 0) {
            throw new IOException("bad SOS");
        }
        getU8();
        if(getU8() != 0) {
            throw new IOException("bad SOS");
        }
    }

    private void processSOF() throws IOException {
        int lf = getU16();
        if(lf < 11) {
            throw new IOException("bad SOF length");
        }

        if(getU8() != 8) {
            throw new IOException("only 8 bit JPEG supported");
        }

        imageHeight = getU16();
        imageWidth  = getU16();

        if(imageWidth <= 0 || imageHeight <= 0) {
            throw new IOException("Invalid image size");
        }

        int numComps = getU8();
        if(numComps != 3 && numComps != 1) {
            throw new IOException("bad component count");
        }

        if(lf != 8+3*numComps) {
            throw new IOException("bad SOF length");
        }

        int hMax = 1;
        int vMax = 1;

        components = new Component[numComps];
        for(int i=0 ; i<numComps ; i++) {
            Component c = new Component(getU8());
            int q = getU8();
            int tq = getU8();

            c.blocksPerMCUHorz = q >> 4;
            c.blocksPerMCUVert = q & 15;

            if(c.blocksPerMCUHorz == 0 || c.blocksPerMCUHorz > 4) {
                throw new IOException("bad H");
            }
            if(c.blocksPerMCUVert == 0 || c.blocksPerMCUVert > 4) {
                throw new IOException("bad V");
            }
            if(tq > 3) {
                throw new IOException("bad TQ");
            }
            c.dequant = dequant[tq];

            hMax = Math.max(hMax, c.blocksPerMCUHorz);
            vMax = Math.max(vMax, c.blocksPerMCUVert);

            components[i] = c;
        }

        int mcuW = hMax * 8;
        int mcuH = vMax * 8;

        imgHMax = hMax;
        imgVMax = vMax;
        mcuCountX = (imageWidth + mcuW - 1) / mcuW;
        mcuCountY = (imageHeight + mcuH - 1) / mcuH;

        for(int i=0 ; i<numComps ; i++) {
            Component c = components[i];
            c.width = (imageWidth * c.blocksPerMCUHorz + hMax - 1) / hMax;
            c.height = (imageHeight * c.blocksPerMCUVert + vMax - 1) / vMax;
            c.minReqWidth = mcuCountX * c.blocksPerMCUHorz * 8;
            c.minReqHeight = mcuCountY * c.blocksPerMCUVert * 8;

            if(c.blocksPerMCUHorz < hMax) {
                c.upsampler |= 1;
            }
            if(c.blocksPerMCUVert < vMax) {
                c.upsampler |= 2;
            }
        }
    }

    private void ensureHeaderDecoded() throws IllegalStateException {
        if(!headerDecoded) {
            throw new IllegalStateException("need to decode header first");
        }
    }

    private boolean allocateDecodeTmp(int YUVstride) {
        if(decodeTmp == null) {
            decodeTmp = new byte[3][];
        }

        boolean requiresUpsampling = false;
        for(int compIdx=0 ; compIdx<3 ; compIdx++) {
            Component c = order[compIdx];
            int reqSize = c.minReqWidth * c.blocksPerMCUVert * 8;
            if(decodeTmp[compIdx] == null || decodeTmp[compIdx].length < reqSize) {
                decodeTmp[compIdx] = new byte[reqSize];
            }
            if(c.upsampler != 0) {
                if(upsampleTmp == null) {
                    upsampleTmp = new byte[3][];
                }
                int upsampleReq = imgVMax * 8 * YUVstride;
                if(upsampleTmp[compIdx] == null || upsampleTmp[compIdx].length < upsampleReq) {
                    upsampleTmp[compIdx] = new byte[upsampleReq];
                }
                requiresUpsampling = true;
            }
        }
        return requiresUpsampling;
    }

    public void skipMCURows(int rows) throws IOException {
        if(!insideSOS) {
            throw new IllegalStateException("decode not started");
        }

        if(order.length != 3) {
            throw new UnsupportedOperationException("RGB decode only supported for 3 channels");
        }

        for(int j=0 ; j< rows; j++) {
        	skipMCUrow();
            
            if(marker != MARKER_NONE) {
                break;
            }
        }
        
        checkDecodeEnd();
    }
    
    private void skipMCUrow() throws IOException {
        ++currentMCURow;
        for(int i=0 ; i<mcuCountX ; i++) {
            for(int compIdx=0 ; compIdx<3 ; compIdx++) {
                final Component c = order[compIdx];

                for(int y=0 ; y<c.blocksPerMCUVert ; y++) {
                    for(int x=0; x<c.blocksPerMCUHorz ; x++) {
                        try {
                        	skipBlock(c);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            throwBadHuffmanCode();
                        }
                    }
                }
            }
            if(--todo <= 0) {
                if(!checkRestart()) {
                    break;
                }
            }
        }
    }
    
    private void decodeMCUrow() throws IOException {
        ++currentMCURow;
        for(int i=0 ; i<mcuCountX ; i++) {
            for(int compIdx=0 ; compIdx<3 ; compIdx++) {
                Component c = order[compIdx];
                int outStride = c.minReqWidth;
                int outPosY = 8*i*c.blocksPerMCUHorz;

                for(int y=0 ; y<c.blocksPerMCUVert ; y++,outPosY+=8*outStride) {
                    for(int x=0,outPos=outPosY ; x<c.blocksPerMCUHorz ; x++,outPos+=8) {
                        try {
                            decodeBlock(data, c);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            throwBadHuffmanCode();
                        }
                        idct2D.compute(decodeTmp[compIdx], outPos, outStride, data);
                    }
                }
            }
            if(--todo <= 0) {
                if(!checkRestart()) {
                    break;
                }
            }
        }
    }
    
    private void doUpsampling(int YUVstride) {
        for(int compIdx=0 ; compIdx<3 ; compIdx++) {
            Component c = order[compIdx];
            int inStride = c.minReqWidth;
            int height = c.blocksPerMCUVert * 8;
            switch(c.upsampler) {
                case 1:
                    for(int i=0 ; i<height ; i++) {
                        upsampleH2(upsampleTmp[compIdx], i*YUVstride, decodeTmp[compIdx], i*inStride, c.width);
                    }
                    break;

                case 2:
                    for(int i=0,inPos0=0,inPos1=0 ; i<height ; i++) {
                        upsampleV2(upsampleTmp[compIdx], (i*2  )*YUVstride, decodeTmp[compIdx], inPos0, inPos1, c.width);
                        upsampleV2(upsampleTmp[compIdx], (i*2+1)*YUVstride, decodeTmp[compIdx], inPos1, inPos0, c.width);
                        inPos0 = inPos1;
                        inPos1 += inStride;
                    }

                case 3:
                    for(int i=0,inPos0=0,inPos1=0 ; i<height ; i++) {
                        upsampleHV2(upsampleTmp[compIdx], (i*2  )*YUVstride, decodeTmp[compIdx], inPos0, inPos1, c.width);
                        upsampleHV2(upsampleTmp[compIdx], (i*2+1)*YUVstride, decodeTmp[compIdx], inPos1, inPos0, c.width);
                        inPos0 = inPos1;
                        inPos1 += inStride;
                    }
                    break;
            }
        }
    }

    private static void YUVtoRGB(IntBuffer out, int outPos, byte[] inY, byte[] inU, byte[] inV, int inPos, int count) {
        do {
            int y = (inY[inPos] & 255);
            int u = (inU[inPos] & 255) - 128;
            int v = (inV[inPos] & 255) - 128;
            int r = y + ((32768 + v*91881           ) >> 16);
            int g = y + ((32768 - v*46802 - u* 22554) >> 16);
            int b = y + ((32768           + u*116130) >> 16);
            if(r > 255) r = 255; else if(r < 0) r = 0;
            if(g > 255) g = 255; else if(g < 0) g = 0;
            if(b > 255) b = 255; else if(b < 0) b = 0;
            
            final int color = 0xff000000 | (r << 16) | (g << 8) | b;
            out.put(outPos++, color);
//            out.put(outPos+0, (byte)255);
//            out.put(outPos+1, (byte)r);
//            out.put(outPos+2, (byte)g);
//            out.put(outPos+3, (byte)b);
//            outPos += 4;
            
            inPos++;
        } while(--count > 0);
    }

    private static void upsampleH2(byte[] out, int outPos, byte[] in, int inPos, int width) {
        if(width == 1) {
            out[outPos] = out[outPos+1] = in[inPos];
        } else {
            int i0 = in[inPos  ] & 255;
            int i1 = in[inPos+1] & 255;
            out[outPos  ] = (byte)i0;
            out[outPos+1] = (byte)((i0*3 + i1 + 2) >> 2);
            for(int i=2 ; i<width ; i++) {
                int i2 = in[inPos+i] & 255;
                int n = i1*3 + 2;
                out[outPos+i*2-2] = (byte)((n + i0) >> 2);
                out[outPos+i*2-1] = (byte)((n + i2) >> 2);
                i0 = i1;
                i1 = i2;
            }
            out[outPos+width*2-2] = (byte)((i0*3 + i1 + 2) >> 2);
            out[outPos+width*2-1] = (byte)i1;
        }
    }

    private static void upsampleV2(byte[] out, int outPos, byte[] in, int inPos0, int inPos1, int width) {
        for(int i=0 ; i<width ; i++) {
            out[outPos+i] = (byte)((3*(in[inPos0+i] & 255) + (in[inPos1+i] & 255) + 2) >> 2);
        }
    }
    
    private static void upsampleHV2(byte[] out, int outPos, byte[] in, int inPos0, int inPos1, int width) {
        if(width == 1) {
            int i0 = in[inPos0] & 255;
            int i1 = in[inPos1] & 255;
            out[outPos] = out[outPos+1] = (byte)((i0*3 + i1 + 2) >> 2);
        } else {
            int i1 = 3*(in[inPos0] & 255) + (in[inPos1] & 255);
            out[outPos] = (byte)((i1 + 2) >> 2);
            for(int i=1 ; i<width ; i++) {
                int i0 = i1;
                i1 = 3*(in[inPos0+i] & 255) + (in[inPos1+i] & 255);
                out[outPos+i*2-1] = (byte)((3*i0 + i1 + 8) >> 4);
                out[outPos+i*2  ] = (byte)((3*i1 + i0 + 8) >> 4);
            }
            out[outPos+width*2-1] = (byte)((i1 + 2) >> 2);
        }
    }

    static final char dezigzag[] = (
        "\0\1\10\20\11\2\3\12" +
        "\21\30\40\31\22\13\4\5" +
        "\14\23\32\41\50\60\51\42" +
        "\33\24\15\6\7\16\25\34" +
        "\43\52\61\70\71\62\53\44" +
        "\35\26\17\27\36\45\54\63" +
        "\72\73\64\55\46\37\47\56" +
        "\65\74\75\66\57\67\76\77" +
        "\77\77\77\77\77\77\77\77" +
        "\77\77\77\77\77\77\77").toCharArray();
    
}
