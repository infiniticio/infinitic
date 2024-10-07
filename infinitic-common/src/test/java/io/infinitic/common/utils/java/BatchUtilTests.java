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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 1 parameter - Batched method with Collection parameter
@SuppressWarnings("unused")
class FooBatch1 {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public List<String> bar(List<Integer> p) {
        return p.stream().map(Object::toString).toList();
    }
}

// 2 parameters - Batched method with Collection parameter
@SuppressWarnings("unused")
class FooBatch2 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public List<String> bar(List<PairInt> l) {
        return l.stream().map(pair -> bar(pair.p(), pair.q())).toList();
    }
}

// 1 parameter - Batched method with Collection parameter
@SuppressWarnings("unused")
class FooBatch3 {
    public String bar(Set<Integer> p) {
        return p.toString();
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public List<String> bar(List<Set<Integer>> p) {
        return p.stream().map(Set::toString).toList();
    }
}

// 1 parameter - No return
@SuppressWarnings("unused")
class FooBatch5 {
    public void bar(int p, int q) {
        // do nothing
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public void bar(List<PairInt> p) {
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
    public void bar(List<MyPair<Integer>> pairs) {
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
    public List<PairInt> bar(List<Integer> list) {
        return list.stream()
                .map(i -> new PairInt(i, i))
                .collect(Collectors.toList());
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
    List<String> bar(int... p) {
        return Arrays.stream(p)
                .mapToObj(Integer::toString)
                .collect(Collectors.toList());
    }
}

// annotation @Batch without corresponding single method with the right parameters
@SuppressWarnings("unused")
class FooBatchError1 {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public List<String> bar(List<Integer> p, int q) {
        return p.stream().map(Object::toString).toList();
    }
}

// annotation @Batch without corresponding single method with the right parameters
@SuppressWarnings("unused")
class FooBatchError2 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public List<String> bar(List<Integer> p) {
        return p.stream().map(Object::toString).toList();
    }
}

// Not the right return type
@SuppressWarnings("unused")
class FooBatchError4 {
    public String bar(int p, int q) {
        return "?";
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public List<Integer> bar(List<PairInt> p) {
        return p.stream().map(pair -> pair.p() + pair.q()).toList();
    }
}

// Not a List in return type
@SuppressWarnings("unused")
class FooBatchError5 {
    public String bar(int p, int q) {
        return "?";
    }

    @Batch(maxMessages = 10, maxSeconds = 1.0)
    public String bar(List<PairInt> p) {
        return "?";
    }
}

record PairInt(int p, int q) {
}

record MyPair<T>(T p, T q) {
}

