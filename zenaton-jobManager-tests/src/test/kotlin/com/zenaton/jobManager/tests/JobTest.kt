package com.zenaton.jobManager.tests

class JobTest {
    lateinit var behavior: (index: Int, retry: Int) -> Status

    enum class Status {
        COMPLETED,
        FAILED_WITH_RETRY,
        FAILED_WITHOUT_RETRY
    }

//    fun mult(i: Int, j: String) = when (behavior(avro)) {
//        InMemoryWorker.Status.COMPLETED -> (i * j.toInt()).toString()
//        InMemoryWorker.Status.FAILED_WITH_RETRY -> sendJobFailed(avro, Exception("Will Try Again"), 0.1F)
//        InMemoryWorker.Status.FAILED_WITHOUT_RETRY -> sendJobFailed(avro, Exception("Job Failed"))
//    }
}
