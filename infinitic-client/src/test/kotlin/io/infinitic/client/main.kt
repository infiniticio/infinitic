package io.infinitic.client

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    launch {
        println("1")
        delay(10)
        println("2")
    }

    coroutineScope { delay(10000) }
//        GlobalScope.future {  delay(10000) }.join()
//    launch {
//        Thread.sleep(10000)
//    }
}
