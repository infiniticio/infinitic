package io.infinitic.serDe.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.infinitic.common.serDe.SerializedData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

class SerDeTest {

    @Test
    public void nullObjShouldBeSerializableDeserializable() {
        SerializedData data = SerializedData.from(null, null);

        Assertions.assertNull(data.deserialize(null));
    }

    @Test
    public void simpleObjectShouldBeSerializableDeserializable() {
        Pojo1 val1 = new Pojo1("42", 42, JType.TYPE_1);
        SerializedData data = SerializedData.from(val1, null);
        System.out.println(data.toJsonString());

        Assertions.assertEquals(val1, data.deserialize(null));
    }

    @Test
    public void objectShouldBeDeserializableEvenWithMoreProperties() {
        Pojo1 val1 = new Pojo1("42", 42, JType.TYPE_1);
        Pojo2 val2 = new Pojo2("42", 42);
        HashMap<String, byte[]> meta = new HashMap<>();
        meta.put(SerializedData.META_JAVA_CLASS, Pojo2.class.getName().getBytes(StandardCharsets.UTF_8));
        SerializedData original = SerializedData.from(val1, null);
        SerializedData data = original.copy(original.toJsonString().replace("Pojo1", "Pojo2").getBytes(), original.getType(), meta);
        System.out.println(data.toJsonString());

        Assertions.assertEquals(val2, data.deserialize(null));
    }

    @Test
    public void objectShouldBeDeserializableEvenWithLessProperties() {
        Pojo1 val1 = new Pojo1("42", 42, JType.TYPE_1);
        Pojo2 val2 = new Pojo2("42", 42);
        HashMap<String, byte[]> meta = new HashMap<>();
        meta.put(SerializedData.META_JAVA_CLASS, Pojo1.class.getName().getBytes(StandardCharsets.UTF_8));
        SerializedData original = SerializedData.from(val2, null);
        SerializedData data = original.copy(original.toJsonString().replace("Pojo2", "Pojo1").getBytes(), original.getType(), meta);
        System.out.println(data.toJsonString());

        Assertions.assertEquals(val1, data.deserialize(null));
    }

    @Test
    public void testPolymorphicDeserialize() {
        Pojo2 pojo = new Pojo2("42", 42);
        PojoWrapper val1 = new PojoWrapper(pojo, pojo);
        
        SerializedData data = SerializedData.from(val1, null);
        System.out.println(data.toJsonString());

        Assertions.assertEquals(val1, data.deserialize(null));
    }

//    @Test
//    public void ListOfSimpleObjectShouldBeSerializableDeserializable() {
//        Pojo1 pojoa = new Pojo1("42", 42, JType.TYPE_1);
//        Pojo1 pojob = new Pojo1("24", 24, JType.TYPE_2);
//        List<Pojo1> pojos = new ArrayList<>();
//        pojos.add(pojoa);
//        pojos.add(pojob);
//        SerializedData data = SerializedData.from(pojos, null);
//
//        System.out.println(data.toJsonString());
//
//        Assertions.assertEquals(pojos, data.deserialize(null));
//    }
}

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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "klass")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Pojo1.class, name = "Pojo1"),
        @JsonSubTypes.Type(value = Pojo2.class, name = "Pojo2")
})
abstract class Pojo {
    protected static String klass;
}

class Pojo1 extends Pojo implements WithFoo {
    private static final String klass = Pojo1.class.getSimpleName();
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
