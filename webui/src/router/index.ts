import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import Dashboard from "../views/Dashboard.vue";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
  {
    path: "/",
    name: "Dashboard",
    component: Dashboard
  },
  {
    path: "/workflows",
    name: "Workflows",
    // route level code-splitting
    // this generates a separate chunk (workflows.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
      import(/* webpackChunkName: "workflows" */ "../views/Workflows.vue")
  },
  {
    path: "/tasks",
    name: "Tasks",
    component: () =>
      import(/* webpackChunkName: "tasks" */ "../views/Tasks.vue")
  }
];

const router = new VueRouter({
  routes
});

export default router;
