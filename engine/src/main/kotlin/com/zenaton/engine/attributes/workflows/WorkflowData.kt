package com.zenaton.engine.attributes.workflows

import com.zenaton.engine.attributes.types.Data

data class WorkflowData(override val data: ByteArray) : Data(data)
