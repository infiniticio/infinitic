# Infinitic Worker

This package is a NodeJS implementation of an infinitic worker.

> ⚠️ This package is mostly a work in progress and serves as an example infinitic worker implementation.
> It is _not_ recommended to use it in production.

## Installation

To install the latest version:

```sh
npm install --save @infinitic/worker
```

Or using yarn:

```sh
yarn add @infinitic/worker
```

## Usage

### Javascript

> This section describes how to use the worker using Javascript only.
> If you want to use Typescript instead, please refer to the [Typescript](#typescript) section.

In order to use the worker, you first need to define a `task` that the worker will be able to run.
Create a file at `src/tasks/refund-booking.js` with the following content:

```javascript
// src/tasks/refund-booking.js

module.exports = {
  name: 'RefundBooking',
  async handle({ bookingId, userId }): {
    console.log(`Refunding booking ${bookingId} for user ${userId}.`);

    // here you would typically send an http request to your payment system to process the refund.
    // for this example, we will return a fake result considering the booking was correctly refunded.

    return { result: 'ok' };
  }
};
```

Now, a `Worker` object needs to be created to register and run the task.
Create a file at `src/index.js` with the following content:

```javascript
// src/index.js

const { Worker } = require('@infinitic/worker');
const RefundBooking = require('./tasks/refund-booking');

const worker = new Worker({
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
});

worker.registerTask(RefundBooking);

worker.run();
```

You can now run this file using `node src/index.js`.
The worker should start and wait for incoming RefundBooking tasks to process.
You are free to register as many tasks as you want on the worker object.

### Typescript

> This section describes how to use the worker using Typescript.
> If you want to use Javascript instead, please refer to the [Javascript](#javascript) section.

The first thing we will do is to write a task. Tasks are objects conforming to the Task interface defined
in the `@infinitic/worker` package.

```javascript
// src/tasks/refund-booking.ts
import { Task } from '@infinitic/worker';

interface RefundBookingInput {
  bookingId: string;
  userId: string;
}

type RefundBookingOutput = 'ok' | 'error';

export const RefundBooking: Task<RefundBookingInput, RefundBookingOutput> = {
  name: 'RefundBooking',
  async handle({ userId, bookingId }: RefundBookingInput): RefundBookingOutput {
    console.log(`Refunding booking ${bookingId} for user ${userId}.`);

    // here you would typically send an http request to your payment system to process the refund.
    // for this example, we will return a fake result considering the booking was correctly refunded.

    return { result: 'ok' };
  },
};
```

Now that we have a task, we can write our index.ts file which will create a Zenaton worker object,
register the task we wrote in the previous step, and run the worker:

```typescript
// src/index.ts
import { Worker } from '@infinitic/worker';
import { RefundBooking } from './tasks/refund-booking';

const worker = new Worker({
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
});

worker.registerTask(RefundBooking);

worker.run();
```

Build and run your project using the typescript compiler or the module bundler of your choice.
The worker should start and wait for incoming RefundBooking tasks to process.
You are free to register as many tasks as you want on the worker object.

## API

### `Class: Worker`

#### `new Worker(options)`

- `options` `<Object>`
  - `pulsar` `<Object>`
    - `client` `<Object>`
      - `serviceUrl` `<string>` The infinic cluster connection url.

#### `worker.registerTask(task)`

- `task` `<Object>`

Register a task on the worker.

#### `worker.run()`

Starts the worker. It will connect to the Pulsar cluster and consume topics based on tasks that were
registered.

## Contributing

If you want change code in the worker and observe on a real infinitic cluster how it behaves, make sure
this package is linkable by yarn in other projects.
Run the following command in the root directory of this package:

```bash
yarn build
yarn link
```

This allows yarn to symlink this package in other projects. You will be able to use a locally modified
worker.

Start a new project somewhere:

```bash
mkdir my-infinitic-project && cd my-infinitic-project
```

Initialize a NodeJS project:

```bash
yarn init -y
```

Link the worker package in your project:

```bash
yarn link @infinitic/worker
```

This will make your current working directory of the worker package to be used in your project.
Please refer to the [Usage](#usage) section for examples of usages.
