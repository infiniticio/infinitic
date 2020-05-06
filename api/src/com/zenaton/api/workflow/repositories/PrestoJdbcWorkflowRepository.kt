package com.zenaton.api.workflow.repositories

import com.zenaton.api.extensions.io.ktor.application.getPath
import com.zenaton.api.workflow.models.Workflow
import io.ktor.application.call
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.Properties

class PrestoJdbcWorkflowRepository : WorkflowRepository {
    private val prestoConnection: Connection by lazy {
        val properties = Properties()
        properties["user"] = "user" // Presto requires setting a user name even if there is no authentication involved

        DriverManager.getConnection("""jdbc:presto://localhost:8081/pulsar""", properties)
    }

    override fun getById(id: String): Workflow {
//        val stmt = prestoConnection.createStatement()
//
//        val names = stmt.use {
//            val sqlQuery = """SELECT * FROM "public/default".workflows LIMIT 10"""
//            val res = stmt.executeQuery(sqlQuery)
//            val names = res.use {
//                //Extract data from result set
//                val names = mutableSetOf<String>()
//                while (res.next()) {
//                    val name = res.getString("workflowdispatched.workflowname.name")
//                    if (name != null) {
//                        names.add(name)
//                    }
//                }
//
//                names.toSet()
//            }
//
//            names
//        }

        return Workflow(
            id,
            "SequentialWorkflow",
            "running",
            listOf("user:123"),
            Instant.now(),
            Instant.now()
        )
    }
}
