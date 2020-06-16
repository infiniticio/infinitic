import { Task } from '@zenaton/worker';
export interface BookingData {
    bookingId: string;
    userId: string;
}
export declare class RefundBooking implements Task {
    name: string;
    handle(data: BookingData): Promise<{
        result: string;
    }>;
}
