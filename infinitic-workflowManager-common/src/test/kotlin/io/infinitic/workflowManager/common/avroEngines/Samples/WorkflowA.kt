package com.zenaton.workflowManager.avroEngines.Samples

class WorkflowA {
    private lateinit var taskA: TaskA

    fun handle(
        sourceBucketName: String,
        sourceFilename: String,
        targetBucketName: String,
        targetFilename: String
    ) {
        val localName = taskA.download(sourceBucketName, sourceFilename)
        val processedName = taskA.processFile(localName)
        taskA.upload(targetBucketName, targetFilename, processedName)
        taskA.deleteLocalFile(processedName)
    }
}
