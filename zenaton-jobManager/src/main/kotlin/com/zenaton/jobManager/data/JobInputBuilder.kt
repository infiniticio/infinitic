package com.zenaton.jobManager.data

import com.zenaton.common.avro.AvroSerDe
import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedData
import com.zenaton.common.json.Json
import org.apache.avro.specific.SpecificRecord
import java.nio.ByteBuffer

class JobInputBuilder {
    private var input: MutableList<SerializedData> = mutableListOf()

    fun add(value: Any?): JobInputBuilder {
        when (value) {
            null -> input.add(
                SerializedData(
                    serializedData = ByteArray(0),
                    serializationType = SerializationType.NULL
                )
            )
            is ByteArray -> input.add(
                SerializedData(
                    serializedData = value,
                    serializationType = SerializationType.BYTES
                )
            )
            is ByteBuffer -> {
                val p = value.position()
                val bytes = ByteArray(value.remaining())
                value.get(bytes, 0, bytes.size)
                value.position(p)

                add(bytes)
            }
            is SpecificRecord -> {
                input.add(
                    SerializedData(
                        serializedData = AvroSerDe.serializeToByteArray(value),
                        serializationType = SerializationType.AVRO
                    )
                )
            }
            else -> {
                input.add(
                    SerializedData(
                        serializedData = Json.stringify(value).toByteArray(Charsets.UTF_8),
                        serializationType = SerializationType.JSON
                    )
                )
            }
        }

        return this
    }

    fun build() = JobInput(input)
}
