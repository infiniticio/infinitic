package io.infinitic.common.tasks.data

import io.infinitic.common.tasks.avro.AvroConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TaskMetaTests : StringSpec({
    "Serialization should be updated if meta is updated" {
        // get meta from serialisation
        var meta = AvroConverter.convertJson<TaskMeta>(TaskMeta(mapOf("a" to "b")))
        // update meta
        meta = meta.with("a", "c")
        // restore from serialization
        val meta2 = AvroConverter.convertJson<TaskMeta>(meta)

        println(meta2)
        println(meta)
        meta2 shouldBe meta
    }
})
