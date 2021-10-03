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

interface Consumer0 {
    fun apply()
}

interface Consumer1<P1> {
    fun apply(p1: P1)
}

interface Consumer2<P1, P2> {
    fun apply(p1: P1, p2: P2)
}

interface Consumer3<P1, P2, P3> {
    fun apply(p1: P1, p2: P2, p3: P3)
}

interface Consumer4<P1, P2, P3, P4> {
    fun apply(p1: P1, p2: P2, p3: P3, p4: P4)
}

interface Consumer5<P1, P2, P3, P4, P5> {
    fun apply(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5)
}

interface Consumer6<P1, P2, P3, P4, P5, P6> {
    fun apply(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6)
}

interface Consumer7<P1, P2, P3, P4, P5, P6, P7> {
    fun apply(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7)
}

interface Consumer8<P1, P2, P3, P4, P5, P6, P7, P8> {
    fun apply(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8)
}

interface Consumer9<P1, P2, P3, P4, P5, P6, P7, P8, P9> {
    fun apply(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9)
}
