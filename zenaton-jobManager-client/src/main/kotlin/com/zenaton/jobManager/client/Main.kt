package com.zenaton.jobManager.client

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.client.avro.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine

fun main() {
    val client = Client()
    client.dispatcher = Dispatcher(FakeAvroDispatcher())

    var j = client.dispatch<FileProcessor> { upload("a", "b", "c") }
}

interface FileProcessor {
    fun upload(bucketName: String, localName: String, targetName: String = "default")
    fun download(bucketName: String, remoteName: String): String
    fun processFile(localName: String): String
    fun deleteLocalFile(fileName: String?)
}

class FakeAvroDispatcher : AvroDispatcher {
    override fun toJobEngine(msg: AvroEnvelopeForJobEngine) {
        println("FakeAvroDispatcher: ${msg.type} ${AvroConverter.removeEnvelopeFromJobEngineMessage(msg)}")
    }
}
