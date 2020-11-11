package io.infinitic.engines.pulsar

fun main() {
//    val schemaDefinition = SchemaDefinition.builder<TaskEngineEnvelope>()
//        .withAlwaysAllowNull(true)
//        .withJSR310ConversionEnabled(true)
//        .withJsonDef(Avro.default.schema(TaskEngineEnvelope.serializer()).toString())
//        .withSchemaReader(TaskEngineEnvelopeReader())
//        .withSchemaWriter(TaskEngineEnvelopeWriter())
//        .withSupportSchemaVersioning(true)
//        .withPojo(null)
//        .build()
//
//    val client = PulsarClient.builder()
//        .serviceUrl("pulsar://localhost:6650")
//        .build()
//
//    val consumer = client
//        .newConsumer(Schema.AVRO(schemaDefinition))
//        .topic("tasks-engine")
//        .subscriptionName("tasks-engine")
//        .subscriptionType(SubscriptionType.Key_Shared)
//        .subscribe()
//
//
//
//
//    val producer = client
//        .newProducer(Schema.AUTO_PRODUCE_BYTES())
//        .topic("tasks-engine")
//        .create()
//
//    val msg = TestFactory.random<TaskEngineEnvelope>()
//    producer.send(writeBinary(msg, TaskEngineEnvelope.serializer()))
//
//    val ss = runBlocking {
//        launch {
//            startConsumer(consumer) {
//                println((it.value as GenericAvroRecord).avroRecord)
//                val record = (it.value as GenericAvroRecord).avroRecord as GenericRecord
//                val msg = Avro.default.fromRecord(TaskEngineEnvelope.serializer(), record)
//                println(msg)
//            }
//        }
//    }
//
//    runBlocking {
//        ss.join()
//    }
//
//    producer.close()
//    client.close()
}

// class TaskEngineEnvelopeWriter: SchemaWriter<TaskEngineEnvelope> {
//    override fun write(message: TaskEngineEnvelope): ByteArray {
//        println("writing msg:$message")
//        return writeBinary(message, TaskEngineEnvelope.serializer())
//    }
// }
//
// class TaskEngineEnvelopeReader: SchemaReader<TaskEngineEnvelope> {
//    override fun read(bytes: ByteArray, offset: Int, length: Int) = read(bytes.inputStream(offset,length))
//    override fun read(inputStream: InputStream) = readBinary(inputStream.readBytes(), TaskEngineEnvelope.serializer())
// }
//
// fun <T> writeBinary(msg: T, serializer: SerializationStrategy<T>): ByteArray {
//    val schema = Avro.default.schema(serializer)
//    val out = ByteArrayOutputStream()
//    Avro.default.openOutputStream(serializer) {
//        format = AvroFormat.BinaryFormat
//        this.schema = schema
//    }.to(out).write(msg).close()
//    return out.toByteArray()
// }
//
// fun <T> readBinary(bytes: ByteArray, serializer: KSerializer<T>): T {
//    val schema = Avro.default.schema(serializer)
//    val datumReader = GenericDatumReader<GenericRecord>(schema)
//    val decoder = org.apache.avro.io.DecoderFactory.get().binaryDecoder(SeekableByteArrayInput(bytes), null)
//    val genericRecord =  datumReader.read(null, decoder)
//    return  Avro.default.fromRecord(serializer, genericRecord)
// }
