import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Shop from './views/Shop.vue'
import Checkout from './views/Checkout.vue'
import Search from './views/Search.vue'
import OrderDetails from './views/OrderDetails.vue'
import Catalog from './views/Catalog.vue'
import Landing from './views/Landing.vue'
import Auth from './views/Auth.vue'
import Contact from './views/Contact.vue'
import PrivacyPolicy from './views/PrivacyPolicy.vue'
import License from './views/License.vue'
import { auth, restoreSession, destination } from './auth'
import './style.css'
import './welcome.css'
import './dashboard.css'
import Account from './views/Account.vue'
import CustomerOrder from './views/CustomerOrder.vue'
import AdminDashboard from './views/AdminDashboard.vue'
const router = createRouter({history: createWebHistory(), scrollBehavior(to, from, saved) { return saved || (to.path !== from.path ? { top: 0 } : false) }, routes: [
  {path: '/', component: Landing, meta: {publicLayout: true}},
  {path: '/contact', component: Contact, meta: {publicLayout: true}},
  {path: '/privacy-policy', component: PrivacyPolicy, meta: {publicLayout: true}},
  {path: '/license', component: License, meta: {publicLayout: true}},
  ...['/login', '/register', '/admin/login', '/admin/register'].map(path => ({path, component: Auth, meta: {publicLayout: true}})),
  {path: '/account', component: Account, meta: {requiresAuth: true}},
  {path: '/account/orders/:id', component: CustomerOrder, meta: {requiresAuth: true}},
  {path: '/admin/dashboard', component: AdminDashboard, meta: {admin: true}},
  {path: '/shop', component: Shop}, {path: '/checkout', component: Checkout, meta: {requiresAuth: true}},
  {path: '/admin/search', component: Search, meta: {admin: true}}, {path: '/admin/orders/:id', component: OrderDetails, meta: {admin: true}},
  {path: '/admin/catalog', component: Catalog, meta: {admin: true}}, {path: '/:pathMatch(.*)*', redirect: '/'},
]})
router.beforeEach(async to => {
  if (!auth.user && (to.path === '/shop' || to.meta.requiresAuth || to.meta.admin)) await restoreSession()
  if (!to.meta.requiresAuth && !to.meta.admin) return
  if (!auth.user) return {path: to.meta.admin ? '/admin/login' : '/login', query: {next: to.fullPath}}
  if (to.meta.admin && auth.user.role !== 'ADMIN') return destination(auth.user.role)
})
createApp(App).use(router).directive('reveal', {
  mounted(el) {
    if (!('IntersectionObserver' in window)) return
    el.classList.add('reveal')
    const io = new IntersectionObserver(entries => {
      for (const e of entries) if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target) }
    }, { threshold: 0, rootMargin: '0px 0px -8% 0px' })
    io.observe(el)
    el.__revealIO = io
  },
  unmounted(el) { el.__revealIO?.disconnect() }
}).mount('#app')
