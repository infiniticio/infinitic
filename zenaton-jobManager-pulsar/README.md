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


#### Installing

```shell script
gradle install -Prefix=tasks
```
or

```shell script
gradle install -Prefix=decisions
```

>> Note: -Prefix option can be used once and is remembered after

#### Update job manager (dev only)

```shell script
gradle update
```

#### Delete job manager

```shell script
gradle delete
```

>> Note:  all consumers (especially functions) must be removed before being able to delete topics,
