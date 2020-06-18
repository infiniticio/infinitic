package com.zenaton.workflowManager.data.branches

import com.zenaton.common.data.AvrosSerializationType
import com.zenaton.common.data.SerializedOutput

data class BranchOutput(
    override val serializedData: ByteArray,
    override val serializationType: AvrosSerializationType
) : SerializedOutput(serializedData, serializationType)
