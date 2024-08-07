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

package io.infinitic.serDe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

enum JType {
    TYPE_1,
    TYPE_2
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "klass")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Pojo1.class, name = "Pojo1"),
        @JsonSubTypes.Type(value = Pojo2.class, name = "Pojo2"),
        @JsonSubTypes.Type(value = Pojo3.class, name = "Pojo3"),
})
interface WithFoo {
    String getFoo();
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "klass", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Pojo1.class, name = "Pojo1"),
        @JsonSubTypes.Type(value = Pojo2.class, name = "Pojo2")
})
abstract class Pojo {
}

class Pojo1 extends Pojo implements WithFoo {
    private final String foo;
    private final int bar;
    private final JType type;

    @JsonCreator
    public Pojo1(
            @JsonProperty("foo") String foo,
            @JsonProperty("bar") int bar,
            @JsonProperty("type") JType type
    ) {
        this.foo = foo;
        this.bar = bar;
        this.type = type == null ? JType.TYPE_1 : type;
    }

    @JsonProperty("foo")
    public String getFoo() {
        return foo;
    }

    @JsonProperty("bar")
    public int getBar() {
        return bar;
    }

    @JsonProperty("type")
    public JType getType() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Pojo1 that)) return false;
        return foo.equals(that.foo) && bar == that.bar && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = foo.hashCode();
        result = 31 * result + bar;
        result = 31 * result + type.hashCode();
        return result;
    }
}

class Pojo2 extends Pojo implements WithFoo {
    private static final String klass = Pojo2.class.getSimpleName();
    private final String foo;
    private final int bar;

    @JsonCreator
    public Pojo2(
            @JsonProperty("foo") String foo,
            @JsonProperty("bar") int bar
    ) {
        this.foo = foo;
        this.bar = bar;
    }

    @JsonProperty("foo")
    public String getFoo() {
        return foo;
    }

    @JsonProperty("bar")
    public int getBar() {
        return bar;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Pojo2 that)) return false;
        return foo.equals(that.foo) && bar == that.bar;
    }

    @Override
    public int hashCode() {
        int result = foo.hashCode();
        result = 31 * result + bar;
        return result;
    }
}

record Pojo3(String foo, int bar) implements WithFoo {
    private static final String klass = "Pojo3";

    @Override
    public String getFoo() {
        return foo;
    }
}

record PojoWrapper(Pojo obj1, WithFoo obj2) {
}
