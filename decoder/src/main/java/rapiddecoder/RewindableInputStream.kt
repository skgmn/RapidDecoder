package rapiddecoder

import java.io.IOException
import java.io.InputStream

internal class RewindableInputStream(private val stream: InputStream) : InputStream() {
    private var buffer: ByteArray? = ByteArray(INITIAL_BUFFER_CAPACITY)
    private var bufferLength = 0
    private var bufferOffset = 0
    private var bufferLimit = -1
    private var markOffset = -1
    private var rewinded = false
    private val lock = Any()

    override fun mark(readlimit: Int) {
        synchronized(lock) {
            if (buffer == null) {
                stream.mark(readlimit)
            } else {
                markOffset = bufferOffset
                bufferLimit = Math.max(bufferLength, bufferOffset + readlimit)
            }
        }
    }

    override fun reset() {
        synchronized(lock) {
            if (buffer == null) {
                stream.reset()
            } else if (markOffset != -1) {
                bufferOffset = markOffset
            } else {
                throw IOException()
            }
        }
    }

    private fun ensureCapacity(buffer: ByteArray, extraLength: Int): ByteArray {
        val requiredLength = bufferLength + extraLength
        if (requiredLength > buffer.size) {
            val newBuffer = ByteArray(Integer.highestOneBit(requiredLength) shl 1)
            System.arraycopy(buffer, 0, newBuffer, 0, bufferLength)
            return newBuffer
        } else {
            return buffer
        }
    }

    private fun bufferExceeded(extraLength: Int): Boolean =
            bufferLimit != -1 && bufferOffset + extraLength > bufferLimit

    override fun read(): Int {
        synchronized(lock) {
            buffer?.let { currentBuffer ->
                if (bufferOffset < bufferLength) {
                    return currentBuffer[bufferOffset++].toInt() and 0xff
                } else if (!bufferExceeded(1)) {
                    val oneByte = stream.read()
                    if (oneByte >= 0) {
                        val newBuffer = ensureCapacity(currentBuffer, 1).also { this.buffer = it }
                        newBuffer[bufferLength++] = oneByte.toByte()
                        bufferOffset = bufferLength
                    }
                    return oneByte
                } else {
                    this.buffer = null
                }
            }
        }
        return stream.read()
    }

    override fun markSupported(): Boolean = stream.markSupported()

    override fun read(b: ByteArray, byteOffset: Int, byteCount: Int): Int {
        synchronized(lock) {
            buffer?.let { currentBuffer ->
                var totalBytesRead = 0
                var offset = byteOffset
                var count = byteCount
                if (bufferOffset < bufferLength) {
                    val bytesToRead = Math.min(bufferLength - bufferOffset, count)
                    System.arraycopy(currentBuffer, bufferOffset, b, offset, bytesToRead)

                    bufferOffset += bytesToRead
                    offset += bytesToRead
                    count -= bytesToRead
                    totalBytesRead += bytesToRead
                }
                if (count > 0) {
                    val bytesRead = stream.read(b, offset, count)
                    if (bytesRead == -1) {
                        return if (totalBytesRead != 0) totalBytesRead else -1
                    }
                    if (bufferExceeded(bytesRead)) {
                        this.buffer = null
                    } else {
                        val newBuffer = ensureCapacity(currentBuffer, bytesRead).also {
                            this.buffer = it
                        }
                        System.arraycopy(b, offset, newBuffer, bufferLength, bytesRead)
                        bufferLength += bytesRead
                        bufferOffset = bufferLength
                    }
                    return totalBytesRead + bytesRead
                } else {
                    return totalBytesRead
                }
            }
        }
        return stream.read(b, byteOffset, byteCount)
    }

    override fun close() {
        stream.close()
    }

    fun rewind() {
        if (!rewinded) {
            synchronized(lock) {
                if (!rewinded) {
                    bufferOffset = 0
                    bufferLimit = bufferLength
                    rewinded = true
                    markOffset = -1
                }
            }
        }
    }

    companion object {
        private const val INITIAL_BUFFER_CAPACITY = 1024
    }
}
