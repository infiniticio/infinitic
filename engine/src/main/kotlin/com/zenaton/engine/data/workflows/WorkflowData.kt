package com.zenaton.engine.data.workflows

import com.zenaton.engine.data.types.Data

data class WorkflowData(override val data: ByteArray) : Data(data)
