package io.infinitic.common.data.interfaces

import io.infinitic.common.json.Json
import java.io.Serializable

interface StateInterface : Serializable

inline fun <reified T : Any> StateInterface.deepCopy(): T = Json.parse(Json.stringify(this))
