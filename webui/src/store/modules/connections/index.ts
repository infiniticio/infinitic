import { Module } from "vuex";
import { SET_CURRENT_CONNECTION } from "./mutations";

export interface Connection {
  url: string;
}

export interface ConnectionsState {
  currentConnection: Connection | undefined;
}

export const connectionsModule: Module<ConnectionsState, unknown> = {
  state: {
    currentConnection: undefined
  },
  mutations: {
    SET_CURRENT_CONNECTION
  }
};
