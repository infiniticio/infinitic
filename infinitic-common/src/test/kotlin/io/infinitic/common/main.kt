package io.infinitic.common

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState

fun main() {
    println(1)
    val state = TestFactory.random<WorkflowState>(mapOf("workflowId" to WorkflowId("testId")))
    println(2)
    val json = SerializedData.from(state).getJson()
    println(json)
    val authors: List<Any> = JsonPath.read(json, "[?($.workflowId == \"testId\")]")
    println(authors)

    Configuration.setDefaults(object : Configuration.Defaults {
        override fun jsonProvider() = JacksonJsonProvider()
        override fun mappingProvider() = JacksonMappingProvider()
        override fun options() = setOf(Option.ALWAYS_RETURN_LIST)
    })

    val json2 = "[\n" +
        "   {\n" +
        "      \"name\" : \"john\",\n" +
        "      \"gender\" : \"male\"\n" +
        "   },\n" +
        "   {\n" +
        "      \"name\" : \"ben\"\n" +
        "   }\n" +
        "]"
    val ok: Boolean = JsonPath.parse(json2).read<List<Any>>("$[0]['gender']").isNotEmpty()
}
