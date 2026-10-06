<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Compass, ShoppingBag, Search, Boxes, ArrowUpRight, LayoutGrid, UserRound, Check, ShoppingCart, BarChart3 } from 'lucide-vue-next'
import { store } from './api'
import { auth, signOut } from './auth'
import SiteFooter from './components/SiteFooter.vue'
const route = useRoute(), router = useRouter()
const count = computed(() => store.cart.reduce((s, i) => s + i.quantity, 0))
const shopper = computed(() => auth.user)
const error = ref('')
async function logout() { try { await signOut(); await router.push('/'); error.value = '' } catch (e) { error.value = e.message } }
</script>
<template>
  <div class="ambient" aria-hidden="true"><i class="orb orb-a"></i><i class="orb orb-b"></i></div>
  <div v-if="route.meta.publicLayout" class="public-shell" :class="{'account-shell': route.path !== '/'}"><header class="public-header"><router-link class="brand" to="/"><span class="brand-mark"><ShoppingBag :size="18"/></span>Buy<span>More</span></router-link><nav v-if="route.path === '/'" aria-label="Main navigation"><router-link to="/shop">Collection</router-link><router-link to="/login">Log in</router-link><router-link class="primary" to="/register">Get started <ArrowUpRight :size="16"/></router-link></nav><router-link v-else class="account-home" to="/">← Back to home</router-link></header><main><router-view :key="route.path"/></main><SiteFooter /></div>
  <div v-else class="app-shell">
    <div class="shell-body">
    <aside class="sidebar">
      <router-link to="/" class="brand"><span class="brand-mark" aria-hidden="true"><ShoppingBag :size="16"/></span>Buy<span>More</span><i></i></router-link>
      <p class="nav-label">YOUR EVERYDAY, UPGRADED</p>
      <nav>
        <router-link to="/shop" :class="{active: route.path === '/shop'}"><Compass :size="18"/> Explore new</router-link>
        <router-link to="/checkout" :class="{active: route.path === '/checkout'}"><ShoppingBag :size="18"/> My cart <small v-if="count">{{count}}</small></router-link>
        <router-link v-if="auth.user" to="/account" :class="{active: route.path.startsWith('/account')}"><UserRound :size="18"/> My space</router-link>
        <template v-if="auth.user?.role === 'ADMIN'"><div class="nav-divider"></div><p class="nav-label">MANAGE</p>
        <router-link to="/admin/dashboard" :class="{active: route.path === '/admin/dashboard'}"><BarChart3 :size="18"/> Overview</router-link>
        <router-link to="/admin/search" :class="{active: route.path.startsWith('/admin/orders') || route.path === '/admin/search'}"><Search :size="18"/> Order search</router-link>
        <router-link to="/admin/catalog" :class="{active: route.path === '/admin/catalog'}"><Boxes :size="18"/> Product catalog</router-link></template>
      </nav>
      <div class="side-note"><div class="note-icon"><LayoutGrid :size="22"/></div><h3>A little more.<br>A little better.</h3><p>Thoughtful essentials for<br>your everyday setup.</p><router-link to="/shop">Explore the collection <ArrowUpRight :size="16"/></router-link></div>
      <div class="sidebar-bottom"><span class="live-dot"></span> The everyday collection <span>2026</span></div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div class="edition"><strong>Good things,<br>great finds.</strong><span>CURATED FOR YOUR EVERYDAY</span></div>
        <div class="switcher"><router-link to="/shop" :class="{selected: !route.path.startsWith('/admin') && !route.path.startsWith('/account') && route.path !== '/contact'}">Storefront</router-link><router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/dashboard" :class="{selected: route.path.startsWith('/admin/dashboard')}">Dashboard</router-link><router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/search" :class="{selected: route.path.startsWith('/admin/orders') || route.path === '/admin/search'}">Orders</router-link><router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/catalog" :class="{selected: route.path === '/admin/catalog'}">Catalog</router-link><router-link v-if="auth.user" to="/account" :class="{selected: route.path.startsWith('/account')}">My space</router-link><router-link to="/contact" :class="{selected: route.path === '/contact'}">Contact</router-link></div>
        <div class="account"><router-link class="cart-link" to="/checkout"><ShoppingCart :size="17"/><span>Cart</span><b :key="store.cartBump" :class="{'cart-count-pop': store.cartBump}">{{count}}</b></router-link><div class="avatar">{{shopper?.name?.split(' ').map(n => n[0]).join('') || '?'}}</div><template v-if="auth.user"><router-link to="/account?tab=profile">{{shopper.name}}</router-link><button class="text-button" @click="logout">Log out</button></template><router-link v-else to="/login">Log in</router-link></div>
      </header>
      <div v-if="error" class="notice error">{{error}} </div>
      <main><router-view v-slot="{ Component }"><Transition name="page" mode="out-in"><div :key="route.path" class="page-content"><component :is="Component"/></div></Transition></router-view></main>
    </div>
    </div>
    <SiteFooter />
  </div>
  <div v-for="flight in store.flights" :key="flight.id" class="cart-flight" aria-hidden="true" :style="{left:flight.x+'px', top:flight.y+'px', '--flight-x':flight.dx+'px', '--flight-y':flight.dy+'px'}"><ShoppingBag :size="18"/><small>+1</small></div>
  <Transition name="toast"><div v-if="store.toast" class="toast" role="status"><Check :size="18"/>{{store.toast}}</div></Transition>
</template>
