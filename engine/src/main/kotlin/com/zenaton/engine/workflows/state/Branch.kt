package com.zenaton.engine.workflows.state

import java.time.LocalDateTime

class Branch(
    val type: BranchType,
    val startedAt: LocalDateTime,
    val storeHashAtStart: StoreHash,
    val steps: List<Step>
)
