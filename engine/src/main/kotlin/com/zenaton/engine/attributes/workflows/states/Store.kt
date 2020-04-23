package com.zenaton.engine.attributes.workflows.states

class Store {
    var properties: Map<PropertyKey, PropertyData> = mapOf()

    fun getHash(): StoreHash {
        // MD5 implementation
        return StoreHash("e")
    }
}
