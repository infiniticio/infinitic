package com.zenaton.engine.tasks.data

import com.zenaton.engine.interfaces.data.IdInterface
import java.util.UUID

data class TaskId(override val id: String = UUID.randomUUID().toString()) : IdInterface
