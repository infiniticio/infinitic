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

package io.infinitic.workflows

class With0<R>(val f: () -> Deferred<R>) {
    fun with() = f()
}

class With1<P1, R>(
    val f: (p1: P1) -> Deferred<R>
) {
    fun with(p1: P1) =
        f(p1)
}

class With2<P1, P2, R>(
    val f: (p1: P1, p2: P2) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2) =
        f(p1, p2)
}

class With3<P1, P2, P3, R>(
    val f: (p1: P1, p2: P2, p3: P3) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3) =
        f(p1, p2, p3)
}

class With4<P1, P2, P3, P4, R>(
    val f: (p1: P1, p2: P2, p3: P3, p4: P4) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3, p4: P4) =
        f(p1, p2, p3, p4)
}

class With5<P1, P2, P3, P4, P5, R>(
    val f: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) =
        f(p1, p2, p3, p4, p5)
}

class With6<P1, P2, P3, P4, P5, P6, R>(
    val f: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) =
        f(p1, p2, p3, p4, p5, p6)
}

class With7<P1, P2, P3, P4, P5, P6, P7, R>(
    val f: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) =
        f(p1, p2, p3, p4, p5, p6, p7)
}

class With8<P1, P2, P3, P4, P5, P6, P7, P8, R>(
    val f: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) =
        f(p1, p2, p3, p4, p5, p6, p7, p8)
}

class With9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>(
    val f: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) -> Deferred<R>
) {
    fun with(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) =
        f(p1, p2, p3, p4, p5, p6, p7, p8, p9)
}
