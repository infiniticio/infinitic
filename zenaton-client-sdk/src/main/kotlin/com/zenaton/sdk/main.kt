package com.zenaton.sdk

fun main() {
    val p = proxyTask<FileProcessor>()

    fun handle(bucketName: String, targetName: String) {
        val localName = p.download(bucketName, targetName)
        val processedName = p.processFile(localName)
        p.upload(bucketName, localName, processedName)
    }

    handle("myBucket", "myFile")
}

interface FileProcessor {
    fun upload(bucketName: String, localName: String, targetName: String)
    fun download(bucketName: String, remoteName: String): String
    fun processFile(localName: String): String
    fun deleteLocalFile(fileName: String?)
}
