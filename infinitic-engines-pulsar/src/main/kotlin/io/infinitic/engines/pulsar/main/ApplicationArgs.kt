// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.pulsar.main

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import io.infinitic.engines.pulsar.main.storage.StorageType
import io.infinitic.storage.redis.RedisStorageConfig

class ApplicationArgs(parser: ArgParser) {
    val storage by parser.storing("The storage adapter to use") { StorageType.of(this) }.default(StorageType.InMemory)

    // Pulsar specific options
    val pulsarUrl by parser.storing("The Pulsar cluster URL").default("pulsar://localhost:6650/")

    // Redis specific options
    val redisHost by parser.storing("Redis hostname").default(RedisStorageConfig.defaultConfig.host)
    val redisPort by parser.storing("Redis port") { toInt() }.default(RedisStorageConfig.defaultConfig.port)
    val redisTimeout by parser.storing("Redis timeout") { toInt() }.default(RedisStorageConfig.defaultConfig.timeout)
    val redisUser by parser.storing("Redis user").default(RedisStorageConfig.defaultConfig.user)
    val redisPassword by parser.storing("Redis password").default(RedisStorageConfig.defaultConfig.password)
    val redisDatabase by parser.storing("Redis database") { toInt() }.default(RedisStorageConfig.defaultConfig.database)
}
