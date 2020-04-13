package com.zenaton.engine.workflows.state

import java.time.LocalDateTime

class Branch(
    val type: BranchType,
    val data: String,
    val startedAt: LocalDateTime,
    val storeHashAtStart: StoreHash? = null,
    val steps: List<Step> = listOf()
)
