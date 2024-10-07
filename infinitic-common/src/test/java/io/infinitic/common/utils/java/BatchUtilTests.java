/*
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.utils.java;

import io.infinitic.annotations.Batch;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 1 parameter - Batched method with Collection parameter
@SuppressWarnings("unused")
class FooBatch1 {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, String> bar(Map<String, Integer> p) {
        return p.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> bar(entry.getValue())));
    }
}

// 2 parameters - Batched method with Collection parameter
@SuppressWarnings("unused")
class FooBatch2 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, String> bar(Map<String, PairInt> l) {
        return l.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> bar(entry.getValue().p(), entry.getValue().q())));
    }
}

// 1 parameter - Batched method with Collection parameter
@SuppressWarnings("unused")
class FooBatch3 {
    public String bar(Set<Integer> p) {
        return p.toString();
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, String> bar(Map<String, Set<Integer>> p) {
        return p.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> bar(entry.getValue())));
    }
}

// 1 parameter - No return
@SuppressWarnings("unused")
class FooBatch5 {
    public void bar(int p, int q) {
        // do nothing
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public void bar(Map<String, PairInt> p) {
        // do nothing
    }
}

// 1 parameter - Parameter with Generic
@SuppressWarnings("unused")
class FooBatch6 {
    public void bar(MyPair<Integer> pair) {
        // do nothing
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public void bar(Map<String, MyPair<Integer>> pairs) {
        // do nothing
    }
}

// Single method with parent return value
@SuppressWarnings("unused")
class FooBatch7 implements FooBatch {

    @Override
    public PairInt bar(int p) {
        return new PairInt(p, p);
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, PairInt> bar(Map<String, Integer> list) {
        return list.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> bar(entry.getValue())));
    }
}

@SuppressWarnings("unused")
interface FooBatch {
    PairInt bar(int p);
}

// vararg not accepted
@SuppressWarnings("unused")
class FooBatchError0 {
    String bar(int p) {
        return Integer.toString(p);
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    Map<String, String> bar(int... p) {
        return Arrays.stream(p).boxed().collect(Collectors.toMap(
                Object::toString,
                Object::toString));
    }
}

// annotation @Batch without corresponding single method with the right parameters
@SuppressWarnings("unused")
class FooBatchError1 {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, String> bar(Map<String, Integer> p, int q) {
        return p.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> bar(entry.getValue())));
    }
}

// annotation @Batch without corresponding single method with the right parameters
@SuppressWarnings("unused")
class FooBatchError2 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, String> bar(Map<String, Integer> p) {
        return p.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> bar(entry.getValue(), entry.getValue())));
    }
}

// Not the right return type
@SuppressWarnings("unused")
class FooBatchError4 {
    public String bar(int p, int q) {
        return "?";
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public Map<String, Integer> bar(Map<String, PairInt> p) {
        return p.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Integer.parseInt(bar(entry.getValue().p(), entry.getValue().q()))));
    }
}

// Not a Map in return type
@SuppressWarnings("unused")
class FooBatchError5 {
    public String bar(int p, int q) {
        return "?";
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public String bar(Map<String, PairInt> p) {
        return "?";
    }
}

record PairInt(int p, int q) {
}

record MyPair<T>(T p, T q) {
}

