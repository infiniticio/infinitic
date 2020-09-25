# Task Engine on Pulsar

An Apache Pulsar function responsible for running Zenaton tasks.

## Development

### Requirements

- Java JDK 8

### Building

```shell script
./gradlew build
```

You can find the built JAR files in `./build/libs`.

## Usage

Task Manager can be used for different usages, by prefixing certain topics

> Note: -Prefix option can be used once and is remembered after

### Install task manager for Tasks processing

```shell script
gradle install -Prefix=tasks
```

### Install task manager for Decisions processing

```shell script
gradle install -Prefix=decisions
```

## Update task manager (dev only)

```shell script
gradle update -Prefix=tasks
gradle update -Prefix=decisions

```

## Delete task manager

```shell script
gradle delete  -Prefix=tasks
gradle delete  -Prefix=decisions
```

> Note:  all consumers (especially functions) must be removed before being able to delete topics,
