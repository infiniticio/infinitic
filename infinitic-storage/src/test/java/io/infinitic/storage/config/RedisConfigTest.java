/**
 * "Commons Clause" License Condition v1.0
 * <p>
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 * <p>
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * <p>
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 * <p>
 * Software: Infinitic
 * <p>
 * License: MIT License (https://opensource.org/licenses/MIT)
 * <p>
 * Licensor: infinitic.io
 */

package io.infinitic.storage.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Protocol;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {
    RedisConfig.RedisConfigBuilder builder;

    @BeforeEach
    void setUp() {
        builder = RedisConfig.builder()
                .setHost("localhost")
                .setPort(6379);
    }

    @Test
    void testDefaultParameters() {
        RedisConfig config = builder.build();

        assertEquals("localhost", config.getHost());
        assertEquals(6379, config.getPort());
        assertNull(config.getUser());
        assertNull(config.getPassword());
        assertEquals(Protocol.DEFAULT_DATABASE, config.getDatabase());
        assertEquals(Protocol.DEFAULT_TIMEOUT, config.getTimeout());
        assertFalse(config.getSsl());

        RedisConfig.PoolConfig poolConfig = config.getPoolConfig();
        assertEquals(-1, poolConfig.getMaxTotal());
        assertEquals(8, poolConfig.getMaxIdle());
        assertEquals(0, poolConfig.getMinIdle());
    }

    @Test
    void testMandatoryHostParameters() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> MySQLConfig.builder()
                        .setPort(3306)
                        .build()
        );
        assertTrue(e.getMessage().contains("host"));
    }

    @Test
    void testMandatoryPortParameters() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> MySQLConfig.builder()
                        .setHost("localhost")
                        .build()
        );
        assertTrue(e.getMessage().contains("port"));
    }

    @Test
    void testOptionalParameters() {
        RedisConfig config = builder
                .setUser("user")
                .setPassword("password")
                .setDatabase(1)
                .setTimeout(42)
                .setSsl(true)
                .setPoolConfig(RedisConfig.PoolConfig.builder()
                        .setMaxIdle(1)
                        .setMaxTotal(2)
                        .setMinIdle(3)
                        .build())
                .build();

        assertEquals("user", config.getUser());
        assertEquals("password", config.getPassword());
        assertEquals(1, config.getDatabase());
        assertEquals(42, config.getTimeout());
        assertTrue(config.getSsl());
        RedisConfig.PoolConfig poolConfig = config.getPoolConfig();
        assertEquals(1, poolConfig.getMaxIdle());
        assertEquals(2, poolConfig.getMaxTotal());
        assertEquals(3, poolConfig.getMinIdle());
    }

    @Test
    void testInvalidHost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setHost(" ").build()
        );
    }

    @Test
    void testInvalidPort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setPort(0).build()
        );
    }

    @Test
    void testInvalidDatabase() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setDatabase(-1).build()
        );
    }

    @Test
    void testInvalidTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setTimeout(0).build()
        );
    }
}
