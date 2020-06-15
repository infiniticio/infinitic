# Zenaton

## Run Standalone Pulsar

> Create those directories, if you don't have them yet:
/engine/build/libs
/engine/build/schemas

In the same directory than `docker-compose.yaml`, do:
```bash
docker-compose up
```

To enter the docker container:

```bash
docker exec -it pulsar_pulsar_1 /bin/bash
```

To clean everything
```bash
docker-compose down --volumes
```

To install Zenaton:
```bash
gradle install
```

To remove Zenaton
```bash
gradle delete
```

To update Zenaton
```bash
gradle update
```

## Pulsar Manager
Once Docker run, you can access it at `http://127.0.0.1:9527`

Connect with user = `pulsar` and password = `pulsar`

To obtain the service url of Pulsar *from the Pulsar Manager container*, do:
```sh
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' zenaton_pulsar_1
```
you should obtain something like `172.18.0.2`. Then the service url to use for adding an environment is `http://172.18.0.2:8080`
