# Playground

Enter Docker,
```bash
docker exec -it zenaton_pulsar_1 /bin/bash
```

then you can manually send a message from a file to a topic such as:
```bash
bin/pulsar-client produce -f /zenaton/engine/playground/WorkflowDispatched.json workflows
```

