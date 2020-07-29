# Infinitic Client

This NodeJS package can be used to dispatch tasks to an infinitic cluster.

> ⚠️ This package is mostly a work in progress and serves as an example infinitic client implementation.
> It is _not_ recommended to use it in production.

Currently, the client only supports dispatching tasks, but more features might be added in the future.

## Installation

To install the latest version:

```sh
npm install --save @zenaton/client
```

Or using yarn:

```sh
yarn add @zenaton/client
```

## Usage

### Javascript

> This section describes how to use the worker using Javascript only.
> If you want to use Typescript instead, please refer to the [Typescript](#typescript) section.

First, you will need to create a Client object. The client will need to connect to your infinitic cluster,
this is the only required option you need to provide:

```javascript
const { Client } = require('@zenaton/client');

const client = new Client({
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
});
```

Then, you can dispatch a task using the `client.dispatchTask(name, input)` method to dispatch a task:

```javascript
await client.dispatchTask('RefundBooking', 123456);
```

The `input` parameter can be any JSON encodable value. If you need to pass multiple values,
you can wrap them in an object:

```javascript
await client.dispatchTask('Refund Booking', {
  userId: 123456,
  bookingId: 654321,
  force: true,
});
```

### Typescript

> This section describes how to use the worker using Typescript.
> If you want to use Javascript instead, please refer to the [Javascript](#javascript) section.

First, you will need to create a Client object. The client will need to connect to your infinitic cluster,
this is the only required option you need to provide:

```typescript
import { Client } from '@zenaton/client';

const client = new Client({
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
});
```

Then, you can dispatch a task using the `client.dispatchTask(name: string, input: any): Promise<void>` method to dispatch a task:

```typescript
await client.dispatchTask('RefundBooking', 123456);
```

The `input` parameter can be any JSON encodable value. If you need to pass multiple values,
you can wrap them in an object:

```typescript
await client.dispatchTask('Refund Booking', {
  userId: 123456,
  bookingId: 654321,
  force: true,
});
```

## API

### `Class: Client`

#### `new Client(options)`

- `options` `<Object>`
  - `pulsar` `<Object>`
    - `client` `<Object>`
      - `serviceUrl` `<string>` The infinic cluster connection url.

#### `client.dispatchTask(name[, input])`

- `name` `<string>`
- `input` `<string>` | `<number>` | `<boolean>` | `<null>` | `<Object>` | `<Array>`
- Returns: `Promise<void>`

Dispatch a task to the infinitic instance. The task will receive the value given as the `input` parameter.
