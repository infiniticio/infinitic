'use strict';

var client$1 = require('@zenaton/client');
var worker$1 = require('@zenaton/worker');
var uuid = require('uuid');

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
const client = /*#__PURE__*/new client$1.Client(opts);
client.dispatchTask("RefundBooking", {
  bookingId: uuid.v4(),
  userId: "john.doe"
});
const worker = /*#__PURE__*/new worker$1.Worker(opts);
worker.registerTask(new RefundBooking());
worker.run();
//# sourceMappingURL=zenaton-examples.cjs.development.js.map
