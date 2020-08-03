package com.zenaton.jobManager.common

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.JobInput

fun main() {
    val s1 = JobInput(listOf(SerializedData.from("a")))
    val s2 = JobInput(listOf(SerializedData.from("a")))
    println(s1)
    println(s2)
    println(s1 == s2)
}
