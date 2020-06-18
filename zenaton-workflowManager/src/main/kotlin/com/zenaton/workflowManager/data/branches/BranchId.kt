package com.zenaton.workflowManager.data.branches

import com.zenaton.common.data.interfaces.IdInterface
import java.util.UUID

data class BranchId(override val id: String = UUID.randomUUID().toString()) : IdInterface
