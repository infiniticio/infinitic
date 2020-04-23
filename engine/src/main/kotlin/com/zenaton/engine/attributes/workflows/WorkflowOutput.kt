package com.zenaton.engine.attributes.workflows

import com.zenaton.engine.attributes.types.Data

data class WorkflowOutput(override val data: ByteArray) : Data(data)
