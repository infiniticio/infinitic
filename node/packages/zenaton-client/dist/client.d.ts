import { ClientOpts as PulsarClientOpts } from 'pulsar-client';
import { ForEngineMessage } from '@zenaton/messages';
export interface ClientOpts {
    pulsar: {
        client: PulsarClientOpts;
    };
}
export declare class Client {
    private opts;
    private pulsarClient?;
    private pulsarProducer?;
    constructor(opts?: Partial<ClientOpts>);
    dispatchTask(name: string, input: any): Promise<void>;
    dispatchForEngineMessage(message: ForEngineMessage): Promise<void>;
}
