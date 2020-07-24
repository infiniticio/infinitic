import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import Dashboard from "../views/Dashboard.vue";
import Welcome from "../views/Welcome.vue";
import NotFound from "../views/NotFound.vue";
import store, { State } from "@/store";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
  {
    path: "/",
    redirect: { name: "Dashboard" }
  },
  {
    path: "/welcome",
    name: "Welcome",
    component: Welcome
  },
  {
    path: "/dashboard",
    name: "Dashboard",
    component: Dashboard,
    meta: {
      requiresConnection: true
    }
  },
  {
    path: "/workflows",
    name: "Workflows",
    // route level code-splitting
    // this generates a separate chunk (workflows.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
      import(/* webpackChunkName: "workflows" */ "../views/Workflows.vue"),
    meta: {
      requiresConnection: true
    }
  },
  {
    path: "/tasks",
    name: "Tasks",
    component: () =>
      import(/* webpackChunkName: "tasks" */ "../views/Tasks.vue"),
    meta: {
      requiresConnection: true
    },
    children: [
      {
        path: ":id",
        name: "TaskDetails",
        component: () =>
          import(/* webpackChunkName: "tasks" */ "../views/TaskDetails.vue"),
        props: true
      }
    ]
  },
  {
    path: "*",
    component: NotFound
  }
];

const router = new VueRouter({
  routes
});

router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresConnection)) {
    // this route requires an infinitic connection, check if we have one, or redirect to the welcome screen.
    if (!(store.state as State).connections.currentConnection) {
      next({
        name: "Welcome"
      });
    } else {
      next();
    }
  } else {
    next(); // make sure to always call next()!
  }
});

export default router;
