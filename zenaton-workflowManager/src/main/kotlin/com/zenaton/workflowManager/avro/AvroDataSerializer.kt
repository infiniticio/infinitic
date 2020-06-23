package com.zenaton.workflowManager.avro

import com.zenaton.common.avro.AvroSerDe
import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedParameter
import com.zenaton.workflowManager.data.DecisionInput

object AvroDataSerializer {
    fun serialize(input: DecisionInput) = SerializedParameter(
        serializationType = SerializationType.AVRO,
        serializedData = AvroSerDe.serializeToByteArray(AvroConverter.toAvroDecisionInput(input))
    )
}
