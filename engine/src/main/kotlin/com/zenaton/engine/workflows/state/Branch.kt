package com.zenaton.engine.workflows.state

import com.zenaton.engine.common.attributes.BranchData
import com.zenaton.engine.common.attributes.DateTime

data class Branch(
    val type: BranchType,
    val data: BranchData?,
    val decidedAt: DateTime,
    val storeHashAtStart: StoreHash? = null,
    val steps: List<Step> = listOf()
)
