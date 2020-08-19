import { Client } from '@infinitic/client';
import { Worker } from '@infinitic/worker';
import { v4 as uuid } from 'uuid';

import { RefundBooking } from './tasks/refund-booking';

const opts = {
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650',
    },
  },
};

const client = new Client(opts);
console.log('Dispatching task...');
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });
client.dispatchTask('RefundBooking', { bookingId: uuid(), userId: 'john.doe' });

console.log('Launching worker...');
const worker = new Worker(opts);
worker.registerTask(new RefundBooking());
worker.run();
