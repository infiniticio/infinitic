# Infinitic Engine for Apache Pulsar

The engine is responsible of running Infinitic workflows and tasks.

## Development

### Requirements

- Java JDK 8

### Building

```shell script
./gradlew build
```

You can find the build JAR files in the following directories:

- `./build/libs` for the Pulsar Function version of the engine
- `./build/distributions` for the standalone version of the engine

## Usage

### Standalone

Get the archive `infinitic-engine-pulsar-1.0.0-SNAPSHOT.zip` from the `./build/distributions` directory
and extract it somewhere. Start the engine using the following command:

```shell script
./bin/infinitic-engine-pulsar
```

By default, it will try to connect to a local Pulsar cluster using the default port. If you want to connect
to a different Pulsar cluster and/or using a different port, use the `--pulsar-url` option:

```shell script
./bin/infinitic-engine-pulsar --pulsar-url=my-pulsar-cluster.somewhere.com:16650
```

#### State storage

The storage to use to persist states of workflows and tasks can be configured using the `--storage` option.
Possible values are:

- `memory`: The engine keeps states in memory. This storage is not persistent and is not suitable for production. Stopping
the engine will make it loose all states.
- `redis`: The engine keeps states using a redis instance. Connection settings can be configured using the various
`--redis-*` options. You can find the list of all available options [here](#available-options)".

#### Available options

The complete list of options can be displayed using the `-h` option:

```shell script
$ ./bin/infinitic-engine-pulsar -h
usage: [-h] [--storage STORAGE] [--pulsar-url PULSAR_URL]
       [--redis-host REDIS_HOST] [--redis-port REDIS_PORT]
       [--redis-timeout REDIS_TIMEOUT] [--redis-user REDIS_USER]
       [--redis-password REDIS_PASSWORD] [--redis-database REDIS_DATABASE]

optional arguments:
  -h, --help                        show this help message and exit

  --storage STORAGE                 The storage adapter to use

  --pulsar-url PULSAR_URL           The Pulsar cluster URL

  --redis-host REDIS_HOST           Redis hostname

  --redis-port REDIS_PORT           Redis port

  --redis-timeout REDIS_TIMEOUT     Redis timeout

  --redis-user REDIS_USER           Redis user

  --redis-password REDIS_PASSWORD   Redis password

  --redis-database REDIS_DATABASE   Redis database

```

### Pulsar Functions

The engine can also be deployed as Pulsar Functions.
If you intend to deploy your Pulsar Functions manually, you will need to use the
`infinitic-engine-pulsar-1.0.0-SNAPSHOT-all.jar` JAR file in the `./build/libs` directory.

We also provide some Gradle tasks to ease this process during development:

#### Install the engine

```shell script
gradle install
```

#### Update the engine (dev only)

```shell script
gradle update
```

#### Delete the engine

```shell script
gradle delete
```

> Note:  all consumers (especially functions) must be removed before being able to delete topics,
