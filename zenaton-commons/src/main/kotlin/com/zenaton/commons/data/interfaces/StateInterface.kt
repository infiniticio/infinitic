package com.zenaton.commons.data.interfaces

import com.zenaton.commons.utils.json.Json
import java.io.Serializable

interface StateInterface : Serializable

inline fun <reified T : Any> StateInterface.deepCopy(): T = Json.parse(Json.stringify(this))
