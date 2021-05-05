# Infinitic

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

To install Infinitic:
```bash
gradle install
```

To remove Infinitic
```bash
gradle delete
```

To update Infinitic
```bash
gradle update
```

## Pulsar Manager
Once Docker run, you can access it at `http://localhost:9527`

Create an admin user with:

```bash
CSRF_TOKEN=$(curl http://localhost:7750/pulsar-manager/csrf-token)
curl \
    -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
    -H "Cookie: XSRF-TOKEN=$CSRF_TOKEN;" \
    -H 'Content-Type: application/json' \
    -X PUT http://localhost:7750/pulsar-manager/users/superuser \
    -d '{"name": "admin", "password": "apachepulsar", "description": "test", "email": "username@test.org"}'
```

Connect with user = `admin` and password = `apachepulsar`

To obtain the service url of Pulsar *from the Pulsar Manager container*, do:

```bash
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' pulsar_pulsar_1
```
you should obtain something like `172.20.0.3`.
Then the service url to use for adding an environment is `http://172.20.0.3:8080`
