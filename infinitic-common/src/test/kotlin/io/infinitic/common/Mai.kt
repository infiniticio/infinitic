package io.infinitic.common

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.TestFactory.random
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.workflows.data.commands.DispatchInstantTimer
import kotlinx.serialization.Serializable
import java.time.Duration

fun main() {

    repeat(40) {
        val d = TestFactory.random<TaskAttemptFailed>()
        println(d)
    }

    val c = DispatchInstantTimer(
        MillisInstant(Duration.ofMillis(2345678).toMillis())
    )

    val b = Avro.default.encodeToByteArray(Foo.serializer(), Foo(c))

    println(b)

    val c2 = Avro.default.decodeFromByteArray(Foo.serializer(), b)

    println(c2)
    println(c2.d)

//    val json = Json.stringify(c)
//    println(json)
//    println(Json.parse(json, DispatchDurationTimer::class.java))
}

@Serializable
data class Foo(
    val d: DispatchInstantTimer
)
