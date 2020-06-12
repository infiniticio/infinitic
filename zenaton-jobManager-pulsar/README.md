# Job Engine on Pulsar

An Apache Pulsar function responsible for running Zenaton jobs.

## Development

### Requirements

- Java JDK 13

### Building

```shell script
./gradlew build
```

You can find the built JAR files in `./build/libs`.

### Usage

Job Manager can be used for different usages, by prefixing certain topics

> Note: -Prefix option can be used once and is remembered after

#### Installing job manager for Task processing

```shell script
gradle install -Prefix=tasks
```

#### Installing job manager for Decision processing

```shell script
gradle install -Prefix=decisions
```

#### Update job manager (dev only)

```shell script
gradle update
```

#### Delete job manager

```shell script
gradle delete
```

> Note:  all consumers (especially functions) must be removed before being able to delete topics,
