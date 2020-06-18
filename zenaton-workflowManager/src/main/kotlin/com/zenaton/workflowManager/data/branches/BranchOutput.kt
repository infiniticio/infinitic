package com.zenaton.workflowManager.data.branches

import com.zenaton.common.data.AvroSerializationType
import com.zenaton.common.data.SerializedOutput

data class BranchOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvroSerializationType
) : SerializedOutput(serializedData, serializationType)
