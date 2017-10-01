package rapiddecoder

import java.io.IOException
import java.io.InputStream

class RewindableInputStream(private val stream: InputStream) : InputStream() {
    private var buffer: ByteArray? = ByteArray(INITIAL_BUFFER_CAPACITY)
    private var bufferLength = 0
    private var bufferOffset: Int = 0
    private var bufferLimit = -1
    private var markOffset = -1
    private var rewinded = false

    override fun mark(readlimit: Int) {
        if (buffer == null) {
            stream.mark(readlimit)
        } else {
            markOffset = bufferOffset
            bufferLimit = Math.max(bufferLength, bufferOffset + readlimit)
        }
    }

    override fun reset() {
        if (buffer == null) {
            stream.reset()
        } else if (markOffset != -1) {
            bufferOffset = markOffset
        } else {
            throw IOException()
        }
    }

    private fun ensureCapacity(extraLength: Int) {
        val buffer = buffer!!
        val requiredLength = bufferLength + extraLength
        if (requiredLength > buffer.size) {
            val newBuffer = ByteArray(requiredLength * 2)
            System.arraycopy(buffer, 0, newBuffer, 0, bufferLength)
            this.buffer = newBuffer
        }
    }

    private fun bufferExceeded(extraLength: Int): Boolean =
            bufferLimit != -1 && bufferOffset + extraLength > bufferLimit

    override fun read(): Int {
        buffer?.let { buffer ->
            if (bufferOffset < bufferLength) {
                return buffer[bufferOffset++].toInt()
            } else if (!bufferExceeded(1)) {
                val oneByte = stream.read()
                if (oneByte >= 0) {
                    ensureCapacity(1)
                    buffer[bufferLength++] = oneByte.toByte()
                    bufferOffset = bufferLength
                }
                return oneByte
            } else {
                this.buffer = null
            }
        }
        return stream.read()
    }

    override fun markSupported(): Boolean = stream.markSupported()

    override fun read(b: ByteArray, byteOffset: Int, byteCount: Int): Int {
        this.buffer?.let { buffer ->
            var totalBytesRead = 0
            var offset = byteOffset
            var count = byteCount
            if (bufferOffset < bufferLength) {
                val bytesToRead = Math.min(bufferLength - bufferOffset, count)
                System.arraycopy(buffer, bufferOffset, b, offset, bytesToRead)

                bufferOffset += bytesToRead
                offset += bytesToRead
                count -= bytesToRead
                totalBytesRead += bytesToRead
            }
            if (byteCount > 0) {
                val bytesRead = stream.read(buffer, offset, count)
                if (bytesRead == -1) {
                    return if (totalBytesRead != 0) totalBytesRead else -1
                }
                if (bufferExceeded(bytesRead)) {
                    this.buffer = null
                } else {
                    ensureCapacity(bytesRead)
                    System.arraycopy(buffer, byteOffset, this.buffer!!, bufferLength, bytesRead)
                    bufferLength += bytesRead
                    bufferOffset = bufferLength
                }
                return totalBytesRead + bytesRead
            } else {
                return totalBytesRead
            }
        }
        return stream.read(buffer, byteOffset, byteCount)
    }

    override fun close() {
        stream.close()
    }

    fun rewind() {
        if (!rewinded) {
            bufferOffset = 0
            bufferLimit = bufferLength
            rewinded = true
            markOffset = -1
        }
    }

    companion object {
        private const val INITIAL_BUFFER_CAPACITY = 1024
    }
}
