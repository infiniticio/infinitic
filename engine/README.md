# Workflow Engine

An Apache Pulsar function responsible for running Zenaton workflows.

## Development

### Requirements

- Java JDK 13

### Building

```shell script
./gradlew build
```

You can find the built JAR files in `./build/libs`.

### Usage

#### Register the function

```shell script
bin/pulsar-admin functions create --jar /engine/workflow-engine/build/workflow-engine-1.0-SNAPSHOT-all.jar --classname com.zenaton.workflowengine.workflowengine.WorkflowEngineFunction --inputs workflows
```

#### Update the function

```shell script
bin/pulsar-admin functions update --jar /engine/workflow-engine/build/workflow-engine-1.0-SNAPSHOT-all.jar --classname com.zenaton.workflowengine.workflowengine.WorkflowEngineFunction --inputs workflows
```

#### Delete the function

```shell script
bin/pulsar-admin functions delete --name WorkflowEngineFunction
```

## Trigger the function with a specific input value

```shell script
bin/pulsar-admin functions trigger --name WorkflowEngineFunction --trigger-value "{\"type\": \"DispatchWorkflow\"}"
```
