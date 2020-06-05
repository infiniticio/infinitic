# Zenaton Worker

This is a NodeJS worker to run Zenaton tasks.

## Usage

Before trying to use this package, we need to make sure it is linkable by yarn in other projects.
Run the following command in the root directory of this package:

```bash
yarn link
```

This allows yarn to symlink this package in other projects while this package is not yet published on the npm registry.

Start a new project somewhere:

```bash
mkdir my-zenaton-project && cd my-zenaton-project
```

Initialize a NodeJS project:

```bash
yarn init -y
```

Link the worker package in your project:

```bash
yarn link @zenaton/worker
```

Create a file at `src/tasks/my-task.js` with the following content:

```javascript
module.exports = {
  name: 'MyTask',
  async handle(): {
    console.log('Hooray! My task is running!');
  }
};
```

Create a file at `index.js` with the following content:

```javascript
const { Worker } = require('@zenaton/worker');

const worker = new Worker({
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
});
worker.registerTask(require('./src/tasks/my-task'));

worker.run();
```

Run this file using node:

```bash
node index.js
```

Your worker should start and be waiting for incoming messages asking for task processing.
