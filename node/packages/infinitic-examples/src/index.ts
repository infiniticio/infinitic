/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

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
