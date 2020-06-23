package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.MetaInterface

data class JobMeta
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val meta: Map<String, ByteArray> = mapOf()) : MetaInterface
