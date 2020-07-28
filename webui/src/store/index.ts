import Vue from "vue";
import Vuex from "vuex";
import createPersistedState from "vuex-persistedstate";
import { connectionsModule, ConnectionsState } from "./modules/connections";

Vue.use(Vuex);

const dataState = createPersistedState({
  paths: ["connections.currentConnection"]
});

export type State = {
  connections: ConnectionsState;
};

export default new Vuex.Store({
  state: {},
  mutations: {},
  actions: {},
  modules: {
    connections: connectionsModule
  },
  plugins: [dataState]
});
