# Zenaton Worker

This is a NodeJS worker to run Zenaton tasks.

## Usage

### Javascript

This section describes how to use the worker using Javascript only.
If you want to use Typescript instead, please refer to the [Typescript](#typescript) section.

Before trying to use this package, we need to make sure it is linkable by yarn in other projects.
Run the following command in the root directory of this package:

```bash
yarn build
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

worker.registerTask(require('./src/tasks/refund-booking'));

worker.run();
```

Run this file using node:

```bash
node index.js
```

Your worker should start and be waiting for incoming messages asking for task processing.

### Typescript

This section describes how to use the worker using Typescript.
If you want to use Javascript instead, please refer to the [Javascript](#javascript) section.

Before trying to use this package, we need to make sure it is linkable by yarn in other projects.
Run the following command in the root directory of this package:

```bash
yarn build
yarn link
```

This allows yarn to symlink this package in other projects while this package is not yet published on the npm registry.

To create our project, we will use [TSDX](https://github.com/jaredpalmer/tsdx). It's a zero-config tool to help
develop typescript projects.
You are free to create your project manually or using a different tool but keep in mind you might have to adapt some
of the following steps in order to make everything work properly.

Run the following command to bootstrap your project:

```bash
npx tsdx create my-zenaton-project
```

After answer a few questions, your project will be created.
You can go to the newly created directory and link the worker package.

```bash
cd my-zenaton-project
yarn link @zenaton/worker
```

The `yarn start` command is provided by TSDX and is used to run the project in development/watch mode.
Your project will be rebuilt upon changes.

The first thing we will do is to write a task. Tasks are objects conforming to the Task interface defined
in the `@zenaton/worker` package.

```typescript
// src/tasks/refund-booking.ts
import { Task } from '@zenaton/worker';

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
import { Worker } from '@zenaton/worker';
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

If you ran the `yarn start` as instructed in the previous steps, TSDX should have rebuild your project,
which is now ready to be run.
By default, your project is build in the `dist/` directory. You can run it with the following command:

```bash
node dist/index.js
```

The worker should start and wait for incoming RefundBooking tasks to process.
