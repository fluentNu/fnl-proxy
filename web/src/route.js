import Vue from "vue";
import VueRouter from "vue-router";
import Main from "./view/Main.vue"
import Login from './view/Login'
Vue.use(VueRouter);

export default new VueRouter({
        routes: [
            {
                path: '/',
                name: 'Login',
                component: Login,
                meta: {
                    title: 'fnl-proxy'
                },
            },
            {
                path: '/main',
                name: 'Main',
                component: Main,
                meta: {
                    title: 'Main'
                },
            },
        ]
    }
)
