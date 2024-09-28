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

package io.infinitic.common.utils.java;

import io.infinitic.annotations.Batch;

import java.util.List;
import java.util.Set;

// 1 parameter - Batched method with Collection parameter
class FooBatch1 {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch
    public List<String> bar(List<Integer> p) {
        return p.stream().map(Object::toString).toList();
    }
}

// 1 parameter - Batched method with vararg parameters
class FooBatch1bis {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch
    public List<String> bar(int... p) {
        return java.util.Arrays.stream(p).mapToObj(Integer::toString).toList();
    }
}

// 2 parameters - Batched method with Collection parameter
class FooBatch2 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch
    public List<String> bar(List<PairInt> l) {
        return l.stream().map(pair -> bar(pair.p(), pair.q())).toList();
    }
}

// 2 parameters - Batched method with vararg parameter
class FooBatch2bis {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch
    public List<String> bar(PairInt... l) {
        return java.util.Arrays.stream(l).map(pair -> bar(pair.p(), pair.q())).toList();
    }
}

// 1 parameter - Batched method with Collection parameter
class FooBatch3 {
    public String bar(Set<Integer> p) {
        return p.toString();
    }

    @Batch
    public List<String> bar(List<Set<Integer>> p) {
        return p.stream().map(Set::toString).toList();
    }
}

// 1 parameter - No return
class FooBatch5 {
    public void bar(int p, int q) {
        // do nothing
    }

    @Batch
    public void bar(List<PairInt> p) {
        // do nothing
    }
}

// Converted code
class FooBatch6 {
    public void bar(MyPair<Integer> pair) {
        // do nothing
    }

    @Batch
    public void bar(List<MyPair<Integer>> pairs) {
        // do nothing
    }
}

// annotation @Batch without corresponding single method with the right parameters
class FooBatchError1 {
    public String bar(int p) {
        return Integer.toString(p);
    }

    @Batch
    public List<String> bar(List<Integer> p, int q) {
        return p.stream().map(Object::toString).toList();
    }
}

// annotation @Batch without corresponding single method with the right parameters
class FooBatchError2 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch
    public List<String> bar(List<Integer> p) {
        return p.stream().map(Object::toString).toList();
    }
}

// double annotation @Batch for the same single method
class FooBatchError3 {
    public String bar(int p, int q) {
        return Integer.toString(p) + q;
    }

    @Batch
    public List<String> bar(List<PairInt> p) {
        return p.stream().map(Object::toString).toList();
    }

    @Batch
    public List<String> bar(PairInt... p) {
        return java.util.Arrays.stream(p).map(Object::toString).toList();
    }
}

// Not the right return type
class FooBatchError4 {
    public String bar(int p, int q) {
        return "?";
    }

    @Batch
    public List<Integer> bar(List<PairInt> p) {
        return p.stream().map(pair -> pair.p() + pair.q()).toList();
    }
}

// Not a List in return type
class FooBatchError5 {
    public String bar(int p, int q) {
        return "?";
    }

    @Batch
    public String bar(List<PairInt> p) {
        return "?";
    }
}

record PairInt(int p, int q) {
}

record MyPair<T>(T p, T q) {
}

