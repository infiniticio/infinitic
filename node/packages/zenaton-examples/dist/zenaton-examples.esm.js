import { Client } from '@zenaton/client';
import { Worker } from '@zenaton/worker';
import { v4 } from 'uuid';

class RefundBooking {
  constructor() {
    this.name = "RefundBooking";
  }

  handle(data) {
    try {
      console.log(`Refunding booking ${data.bookingId} for user ${data.userId}.`); // here you would typically send an http request to your payment system to process the refund.
      // for this example, we will return a fake result considering the booking was correctly refunded.

      return Promise.resolve({
        result: 'ok'
      });
    } catch (e) {
      return Promise.reject(e);
    }
  }

}

const opts = {
  pulsar: {
    client: {
      serviceUrl: 'pulsar://localhost:6650'
    }
  }
};
const client = /*#__PURE__*/new Client(opts);
client.dispatchTask("RefundBooking", {
  bookingId: v4(),
  userId: "john.doe"
});
const worker = /*#__PURE__*/new Worker(opts);
worker.registerTask(new RefundBooking());
worker.run();
//# sourceMappingURL=zenaton-examples.esm.js.map
