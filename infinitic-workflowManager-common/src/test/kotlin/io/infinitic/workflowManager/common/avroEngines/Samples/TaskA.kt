package com.zenaton.workflowManager.avroEngines.Samples

interface TaskA {
    fun upload(bucketName: String?, localName: String?, targetName: String?)
    fun download(bucketName: String?, remoteName: String): String?
    fun processFile(localName: String?): String?
    fun deleteLocalFile(fileName: String?)
}
