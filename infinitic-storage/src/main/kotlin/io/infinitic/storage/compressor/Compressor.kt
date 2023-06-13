package io.infinitic.storage.compressor

import java.io.ByteArrayOutputStream
import mu.KotlinLogging
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory

@Suppress("EnumEntryName")
enum class Compressor {
  gzip {
    override fun toString() = CompressorStreamFactory.GZIP
  },
  bzip2 {
    override fun toString() = CompressorStreamFactory.BZIP2
  },
  deflate {
    override fun toString() = CompressorStreamFactory.DEFLATE
  };

  fun compress(data: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    // compress
    CompressorStreamFactory().createCompressorOutputStream(toString(), out).use { it.write(data) }
    // return result
    return out.toByteArray().also { out.close() }
  }

  companion object {
    private val logger = KotlinLogging.logger {}

    fun decompress(data: ByteArray): ByteArray {
      val input = data.inputStream()
      // Use the header to detect the compression type:
      // There is a potential issue with data that randomly
      // have the signature of a compression type.
      // As such signature is generally only a few bytes, it should not be that rare.
      // That's why below we return the data if we have an error during the decompression.
      val type =
          try {
            CompressorStreamFactory.detect(input)
          } catch (e: CompressorException) {
            // no compressor type found, return original
            return data
          }

      val out = ByteArrayOutputStream()
      // decompress
      try {
        CompressorStreamFactory().createCompressorInputStream(type, input).use {
          out.write(it.readAllBytes())
        }
      } catch (e: Exception) {
        // see comment above
        logger.warn {
          "Error when decompressing data with '$type' algorithm, fallback to not decompressing\n" +
              e.message
        }
        return data.also { out.close() }
      }
      // return result
      return out.toByteArray().also { out.close() }
    }
  }
}
