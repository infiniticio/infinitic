package com.zenaton.common.data.interfaces

import com.zenaton.common.json.Json
import java.io.Serializable

interface StateInterface : Serializable

inline fun <reified T : Any> StateInterface.deepCopy(): T = Json.parse(Json.stringify(this))
