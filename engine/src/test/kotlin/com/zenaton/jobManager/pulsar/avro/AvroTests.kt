package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.apache.avro.specific.SpecificRecordBase
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class AvroTests : StringSpec({

    "all items in AvroForEngineMessage should implement ForEngineMessage after conversion" {
        val msg = AvroForEngineMessage()
        msg.schema.getField("type").schema().enumSymbols.forEach {
            // check that type is an union
            msg.schema.getField(it).schema().isUnion shouldBe true
            // check that first type is null
            msg.schema.getField(it).schema().types[0].isNullable shouldBe true
            // check that it implements ForEngineMessage after conversion
            val namespace = msg.schema.getField(it).schema().types[1].namespace
            val name = msg.schema.getField(it).schema().types[1].name
            val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
            (AvroConverter.fromAvro(TestFactory.get(klass)) is ForEngineMessage) shouldBe true
        }
    }

    "all items in AvroForMonitoringGlobalMessage should implement ForMonitoringGlobalMessage after conversion" {
        val msg = AvroForMonitoringGlobalMessage()
        msg.schema.getField("type").schema().enumSymbols.forEach {
            // check that type is an union
            msg.schema.getField(it).schema().isUnion shouldBe true
            // check that first type is null
            msg.schema.getField(it).schema().types[0].isNullable shouldBe true
            // check that it implements ForMonitoringGlobalMessage after conversion
            val namespace = msg.schema.getField(it).schema().types[1].namespace
            val name = msg.schema.getField(it).schema().types[1].name
            val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
            (AvroConverter.fromAvro(TestFactory.get(klass)) is ForMonitoringGlobalMessage) shouldBe true
        }
    }

    "all items in AvroForMonitoringPerNameMessage should implement ForMonitoringPerNameMessage after conversion" {
        val msg = AvroForMonitoringPerNameMessage()
        msg.schema.getField("type").schema().enumSymbols.forEach {
            // check that type is an union
            msg.schema.getField(it).schema().isUnion shouldBe true
            // check that first type is null
            msg.schema.getField(it).schema().types[0].isNullable shouldBe true
            // check that it implements ForMonitoringPerNameMessage after conversion
            val namespace = msg.schema.getField(it).schema().types[1].namespace
            val name = msg.schema.getField(it).schema().types[1].name
            val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
            (AvroConverter.fromAvro(TestFactory.get(klass)) is ForMonitoringPerNameMessage) shouldBe true
        }
    }

    "all items in AvroForMonitoringPerInstanceMessage should implement ForMonitoringPerInstanceMessage after conversion" {
        val msg = AvroForMonitoringPerInstanceMessage()
        msg.schema.getField("type").schema().enumSymbols.forEach {
            // check that type is an union
            msg.schema.getField(it).schema().isUnion shouldBe true
            // check that first type is null
            msg.schema.getField(it).schema().types[0].isNullable shouldBe true
            // check that it implements ForMonitoringPerInstanceMessage after conversion
            val namespace = msg.schema.getField(it).schema().types[1].namespace
            val name = msg.schema.getField(it).schema().types[1].name
            val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
            (AvroConverter.fromAvro(TestFactory.get(klass)) is ForMonitoringPerInstanceMessage) shouldBe true
        }
    }

    "all items in AvroForWorkerMessage should implement ForWorkerMessage after conversion" {
        val msg = AvroForWorkerMessage()
        msg.schema.getField("type").schema().enumSymbols.forEach {
            // check that type is an union
            msg.schema.getField(it).schema().isUnion shouldBe true
            // check that first type is null
            msg.schema.getField(it).schema().types[0].isNullable shouldBe true
            // check that it implements ForWorkerMessage after conversion
            val namespace = msg.schema.getField(it).schema().types[1].namespace
            val name = msg.schema.getField(it).schema().types[1].name
            val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
            (AvroConverter.fromAvro(TestFactory.get(klass)) is ForWorkerMessage) shouldBe true
        }
    }


})
