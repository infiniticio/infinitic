package com.zenaton.jobManager.data

import com.zenaton.common.avro.AvroSerDe
import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedParameter
import com.zenaton.common.json.Json
import org.apache.avro.specific.SpecificRecord
import java.nio.ByteBuffer

class JobInputBuilder {
    private var input: MutableList<SerializedParameter> = mutableListOf()

    fun add(value: Any?): JobInputBuilder {
        when (value) {
            null -> input.add(
                SerializedParameter(
                    serializedData = ByteArray(0),
                    serializationType = SerializationType.NULL
                )
            )
            is ByteArray -> input.add(
                SerializedParameter(
                    serializedData = value,
                    serializationType = SerializationType.BYTES
                )
            )
            is ByteBuffer -> {
                value.rewind()
                val bytes = ByteArray(value.remaining())
                value.get(bytes, 0, bytes.size)
                add(bytes)
            }
            is SpecificRecord -> {
                input.add(
                    SerializedParameter(
                        serializedData = AvroSerDe.serializeToByteArray(value),
                        serializationType = SerializationType.AVRO
                    )
                )
            }
            else -> {
                input.add(
                    SerializedParameter(
                        serializedData = Json.stringify(value).toByteArray(Charsets.UTF_8),
                        serializationType = SerializationType.JSON
                    )
                )
            }
        }

        return this
    }

    fun get() = JobInput(input)
}
