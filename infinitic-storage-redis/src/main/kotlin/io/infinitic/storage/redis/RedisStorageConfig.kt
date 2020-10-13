package io.infinitic.storage.redis

import redis.clients.jedis.Protocol

class RedisStorageConfig {
    var host = Protocol.DEFAULT_HOST
    var port = Protocol.DEFAULT_PORT
    val timeout = Protocol.DEFAULT_TIMEOUT
    val user = ""
    val password = ""
    var database = Protocol.DEFAULT_DATABASE
}
