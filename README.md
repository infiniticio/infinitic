![scarf pixel](https://static.scarf.sh/a.png?x-pxid=b7a9c0b3-ae8b-4e19-838b-36a40ee1cf96)

# Infinitic

Infinitic is still in active development. Subscribe [here](https://infinitic.substack.com) to follow the progress.

## What is it?

Infinitic is a framework based on [Apache Pulsar](https://pulsar.apache.org/) that considerably eases building
asynchronous distributed apps.

Many issues arise when we build and scale a distributed system - issues we don’t have in a single-process one. Infinitic
provides simple but powerful libraries that let developers build distributed applications as if they were run on an
infallible single-process system. It does this on top of Pulsar to benefit from its scalability and reliability.

Infinitic is very good at orchestrating workflows, ie. at managing the execution of tasks on distributed servers
according to any complex scenario. Moreover, Infinitic ensures that a failure somewhere will never break your workflows.

At last, Infinitic lets us monitor everything occurring inside your app through dashboards.

Possible use cases are:

- microservices orchestration
- distributed transactions (payments...)
- data pipelines operations
- business processes implementation
- etc.

## Getting Started

See the [documentation](https://docs.infinitic.io).
