package com.zenaton.workflowManager.data.branches

import com.zenaton.commons.data.AvrosSerializationType
import com.zenaton.commons.data.SerializedOutput

data class BranchOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedOutput(serializedData, serializationType)
