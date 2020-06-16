import { Task } from '@zenaton/worker';

export interface BookingData {
  bookingId: string;
  userId: string;
}

export class RefundBooking implements Task {
  name: string = "RefundBooking";
  async handle(data: BookingData)  {
    console.log(`Refunding booking ${data.bookingId} for user ${data.userId}.`);

    // here you would typically send an http request to your payment system to process the refund.
    // for this example, we will return a fake result considering the booking was correctly refunded.
    return { result: 'ok' };
  }
}
