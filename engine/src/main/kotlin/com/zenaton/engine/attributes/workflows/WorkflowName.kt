package com.zenaton.engine.attributes.workflows

import com.zenaton.engine.attributes.types.Name

data class WorkflowName(override val name: String) : Name(name)
