package com.zenaton.engine.workflows.state

import com.zenaton.engine.common.attributes.DateTime

class Branch(
    val type: BranchType,
    val data: String,
    val startedAt: DateTime,
    val storeHashAtStart: StoreHash? = null,
    val steps: List<Step> = listOf()
)
