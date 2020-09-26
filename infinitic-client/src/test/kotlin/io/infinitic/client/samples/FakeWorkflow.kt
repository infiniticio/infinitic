package io.infinitic.client.samples

internal interface FakeWorkflow {
    fun m1()
    fun m1(i: Int?): String
    fun m1(str: String): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: FakeInterface): String
}
