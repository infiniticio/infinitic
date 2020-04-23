package com.zenaton.engine.attributes.tasks

import com.zenaton.engine.attributes.types.Name

data class TaskName(override val name: String) : Name(name)
