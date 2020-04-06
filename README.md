# Zenaton

## Run Standalone Pulsar

In same directory than `docker-compose.yaml`:
```bash
docker-compose up
```

> note: make sure you've compiled the project at least once to already have a directory `./engine/build/libs`

To enter the docker container:

```bash
docker exec -it zenaton_pulsar_1 /bin/bash
```

