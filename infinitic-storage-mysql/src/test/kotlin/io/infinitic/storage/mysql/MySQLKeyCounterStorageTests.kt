/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.storage.mysql

import com.sksamuel.hoplite.Secret
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.MySQLContainer

@EnabledIf(DockerOnly::class)
class MySQLKeyCounterStorageTests : StringSpec({

    val mysql = MySQLContainer<Nothing>("mysql:5.7")
        .apply {
            startupAttempts = 1
            withUsername("test")
            withPassword("password")
            withDatabaseName("infinitic")
        }
        .also { it.start() }

    val storage = MySQLKeyCounterStorage.of(
        MySQL(
            host = mysql.host,
            port = mysql.firstMappedPort,
            user = mysql.username,
            password = Secret(mysql.password),
            database = mysql.databaseName
        )
    )

    afterSpec {
        mysql.stop()
    }

    beforeTest {
        storage.incr("foo", 42)
    }

    afterTest {
        storage.flush()
    }

    "getCounter should return 0 on unknown key" {
        storage.get("unknown") shouldBe 0
    }

    "getCounter should return value on known key" {
        storage.get("foo") shouldBe 42
    }

    "incrCounter on unknown key should incr value from 0" {
        storage.incr("unknown", 42)

        storage.get("unknown") shouldBe 42
    }

    "incrCounter on known key should incr value from current" {
        storage.incr("foo", -7)

        storage.get("foo") shouldBe 35
    }
})
