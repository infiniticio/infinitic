package com.zenaton.engine.data.tasks

import com.zenaton.engine.data.types.Data

data class TaskOutput(override val data: ByteArray) : Data(data)
