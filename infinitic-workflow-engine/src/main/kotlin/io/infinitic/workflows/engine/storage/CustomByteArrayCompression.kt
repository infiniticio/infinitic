package io.infinitic.workflows.engine.storage

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

interface CustomByteArrayCompression {
  /**
   * Compress a byte array using GZIP. withContext to avoid thread pin
   *
   * @return an UTF-8 encoded byte array.
   */
  fun ByteArray.gzipCompress(): ByteArray {
    val bos = ByteArrayOutputStream()
    val gzip = GZIPOutputStream(bos)
    gzip.write(this)
    gzip.close()
    return bos.toByteArray()
  }

  /**
   * Decompress a byte array using GZIP.
   *
   * @return an UTF-8 encoded byte array.
   */
  fun ByteArray.gzipDecompress(): ByteArray {
    val bos = ByteArrayOutputStream()
    val gzipInputStream: GZIPInputStream = GZIPInputStream(this.inputStream())
    val buffer = ByteArray(1024)
    var len = 0
    while (gzipInputStream.read(buffer).also { len = it } >= 0) {
      bos.write(buffer, 0, len)
    }
    gzipInputStream.close()
    bos.close()
    return bos.toByteArray()
  }
}
