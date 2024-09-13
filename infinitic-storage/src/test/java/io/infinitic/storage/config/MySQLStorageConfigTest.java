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

import static org.junit.jupiter.api.Assertions.*;

class MySQLStorageConfigTest {
    MySQLStorageConfig.MySQLStorageConfigBuilder builder;

    @BeforeEach
    void setUp() {
        builder = MySQLStorageConfig.builder()
                .setHost("localhost")
                .setPort(3306)
                .setUsername("root")
                .setPassword("password");
    }

    @Test
    void testDefaultParameters() {
        MySQLStorageConfig config = builder.build();

        assertEquals("localhost", config.getHost());
        assertEquals(3306, config.getPort());
        assertEquals("root", config.getUsername());
        assertEquals("password", config.getPassword());
        assertEquals("infinitic", config.getDatabase());
        assertEquals("key_set_storage", config.getKeySetTable());
        assertEquals("key_value_storage", config.getKeyValueTable());
    }

    @Test
    void testMandatoryHostParameters() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> MySQLStorageConfig.builder()
                        .setHost("localhost")
                        .setUsername("root")
                        .build()
        );
        assertTrue(e.getMessage().contains("port"));
    }

    @Test
    void testMandatoryPortParameters() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> MySQLStorageConfig.builder()
                        .setHost("localhost")
                        .setUsername("root")
                        .build()
        );
        assertTrue(e.getMessage().contains("port"));
    }

    @Test
    void testMandatoryUserParameters() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> MySQLStorageConfig.builder()
                        .setHost("localhost")
                        .setPort(3306)
                        .build()
        );
        assertTrue(e.getMessage().contains("user"));
    }

    @Test
    void testOptionalParameters() {
        MySQLStorageConfig config = builder
                .setConnectionTimeout(1L)
                .setIdleTimeout(2L)
                .setMaxLifetime(3L)
                .setMinimumIdle(4)
                .setMaximumPoolSize(5)
                .build();

        assertEquals(1L, config.getConnectionTimeout());
        assertEquals(2L, config.getIdleTimeout());
        assertEquals(3L, config.getMaxLifetime());
        assertEquals(4, config.getMinimumIdle());
        assertEquals(5, config.getMaximumPoolSize());
    }

    @Test
    void testInvalidDatabaseName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setDatabase("d-b").build()
        );
    }

    @Test
    void testInvalidKeyValueTableName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setKeyValueTable("k-s").build()
        );
    }

    @Test
    void testInvalidKeySetTableName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setKeySetTable("k-s").build()
        );
    }

    @Test
    void testInvalidConnectionTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setConnectionTimeout(-1).build()
        );
    }

    @Test
    void testInvalidIdleTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setIdleTimeout(0).build()
        );
    }

    @Test
    void testInvalidMaxLifetime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setMaxLifetime(0).build()
        );
    }

    @Test
    void testInvalidMinimumIdle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setMinimumIdle(-1).build()
        );
    }

    @Test
    void testInvalidMaximumPoolSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.setMaximumPoolSize(0).build()
        );
    }
}
