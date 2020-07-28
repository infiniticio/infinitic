import { ConnectionsState, Connection } from ".";

export interface SetCurrentConnectionPayload {
  connection: Connection;
}

export function SET_CURRENT_CONNECTION(
  state: ConnectionsState,
  payload: SetCurrentConnectionPayload
) {
  state.currentConnection = payload.connection;
}
