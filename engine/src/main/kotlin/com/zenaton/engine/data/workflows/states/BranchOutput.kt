package com.zenaton.engine.data.workflows.states

import com.zenaton.engine.data.types.Data

data class BranchOutput(override val data: ByteArray) : Data(data)
