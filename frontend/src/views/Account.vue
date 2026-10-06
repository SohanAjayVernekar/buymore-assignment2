<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowUpRight, Package, Heart, UserRound, ShoppingBag, RotateCw, ChevronLeft, ChevronRight, Plus, Check, Truck, Wallet, ShoppingCart, LifeBuoy, Gift } from 'lucide-vue-next'
import { api, post, money, date, statusLabel, addToCart, notify, deal, discount, store } from '../api'
import { auth, setUser } from '../auth'
import { favourites, loadFavourites } from '../favourites'
import { buyAgain } from '../accountActions'
import ProductArt from '../components/ProductArt.vue'
import FavouriteButton from '../components/FavouriteButton.vue'
const route = useRoute()
const tab = computed(() => ['orders','favourites','profile'].includes(route.query.tab) ? route.query.tab : 'orders')
const result = ref({ items: [], summary: { total: 0, active: 0, spent: 0 }, total_pages: 0 })
const loading = ref(true), error = ref(''), page = ref(1), buying = ref(null), message = ref('')
// Status chips filter server-side, so paging keeps working while filtered.
const statusOptions = [{ value: '', label: 'All' }, { value: 'PENDING', label: 'Pending' }, { value: 'PROCESSING', label: 'Processing' }, { value: 'SHIPPED', label: 'Shipped' }]
const statusFilter = ref(statusOptions.some(o => o.value === route.query.status) ? route.query.status : '')
function filterStatus(value) { statusFilter.value = value; if (page.value !== 1) page.value = 1; else load() }
const name = ref(auth.user?.name || ''), saving = ref(false), profileError = ref('')
let request = 0
async function load() {
  const id = ++request; loading.value = true; error.value = ''
  try { const response = await api(`/account/orders?page=${page.value}${statusFilter.value ? `&status=${statusFilter.value}` : ''}`); if (id === request) result.value = response }
  catch(e) { if (id === request) error.value = e.message }
  finally { if (id === request) loading.value = false }
}
async function reorder(id) {
  buying.value = id; message.value = ''
  try { message.value = await buyAgain(id) } catch(e) { message.value = e.message }
  finally { buying.value = null }
}
async function saveProfile() {
  saving.value = true; profileError.value = ''
  try { setUser(await post('/account/profile', { name: name.value.trim() }, 'PATCH')); name.value = auth.user.name; notify('Your profile has been updated') }
  catch(e) { profileError.value = e.message } finally { saving.value = false }
}
const deals = ref([])
const activeOrder = computed(() => result.value.items.find(o => o.status === 'PENDING' || o.status === 'PROCESSING') || null)
const trackSteps = [
  { key: 'PENDING', title: 'Order placed', icon: ShoppingBag },
  { key: 'PROCESSING', title: 'Getting ready', icon: Package },
  { key: 'SHIPPED', title: 'Shipped', icon: Truck }
]
const trackProgress = computed(() => activeOrder.value ? trackSteps.findIndex(s => s.key === activeOrder.value.status) : -1)
const FREE_SHIPPING_AT = 50
const spent = computed(() => Number(result.value.summary.spent || 0))
const rewards = computed(() => {
  const pct = Math.min(100, Math.round((spent.value / FREE_SHIPPING_AT) * 100))
  return { pct, unlocked: spent.value >= FREE_SHIPPING_AT, left: Math.max(0, FREE_SHIPPING_AT - spent.value) }
})
const cartCount = computed(() => store.cart.reduce((sum, i) => sum + i.quantity, 0))
const cartTotal = computed(() => store.cart.reduce((sum, i) => sum + i.unit_price * i.quantity, 0))
function arrivalDate(o) {
  const d = new Date(o.order_date); d.setDate(d.getDate() + 5)
  return d < new Date() ? null : d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
}
const eta = computed(() => activeOrder.value ? arrivalDate(activeOrder.value) : null)
async function loadDeals() {
  try { const res = await api('/products?page_size=50'); deals.value = (res.items || []).filter(deal).slice(0, 6) }
  catch { deals.value = [] }
}
watch(page, load)
onMounted(() => { load(); loadDeals() })
onUnmounted(() => request++)
</script>

<template>
  <div class="hub-page">
    <section class="hub-hero customer-hero">
      <div><p class="eyebrow">YOUR OWN LITTLE CORNER</p><h1>Hello, {{ auth.user?.name?.split(' ')[0] }}<span>.</span></h1><p>Your favourites, your orders, your everyday. All right here.</p><router-link class="hub-link" to="/shop">Find your next favourite <ArrowUpRight :size="17" /></router-link></div>
      <div class="hero-orbit" aria-hidden="true"><span><ShoppingBag :size="58" :stroke-width="1.3" /></span><i><Heart :size="22" /></i><b><Package :size="24" /></b></div>
    </section>
    <div class="hub-stats four-stats">
      <article class="hub-stat"><span class="stat-icon lavender"><Package :size="21" /></span><div><small>Orders placed</small><strong>{{ loading || error ? '—' : result.summary.total }}</strong></div></article>
      <article class="hub-stat"><span class="stat-icon peach"><ShoppingBag :size="21" /></span><div><small>In progress</small><strong>{{ loading || error ? '—' : result.summary.active }}</strong></div></article>
      <article class="hub-stat"><span class="stat-icon mint"><Heart :size="21" /></span><div><small>Saved favourites</small><strong>{{ favourites.loading || favourites.error ? '—' : favourites.items.length }}</strong></div></article>
      <article class="hub-stat"><span class="stat-icon sky"><Wallet :size="21" /></span><div><small>Total spent</small><strong>{{ loading || error ? '—' : money(result.summary.spent) }}</strong></div></article>
    </div>
    <section class="quick-actions" aria-label="Quick actions">
      <router-link v-if="activeOrder" class="quick-card" :to="`/account/orders/${activeOrder.order_id}`"><span class="stat-icon peach"><Truck :size="18" /></span><div><strong>Track order</strong><small>#{{ String(activeOrder.order_id).padStart(4, '0') }} is on the way</small></div><ArrowUpRight :size="15" /></router-link>
      <router-link class="quick-card" to="/shop"><span class="stat-icon mint"><Gift :size="18" /></span><div><strong>Today's deals</strong><small>Fresh offers daily</small></div><ArrowUpRight :size="15" /></router-link>
      <router-link class="quick-card" to="/account?tab=favourites"><span class="stat-icon lavender"><Heart :size="18" /></span><div><strong>Saved items</strong><small>{{ favourites.items.length }} in your list</small></div><ArrowUpRight :size="15" /></router-link>
      <router-link class="quick-card" to="/contact"><span class="stat-icon sky"><LifeBuoy :size="18" /></span><div><strong>Get help</strong><small>We reply within a day</small></div><ArrowUpRight :size="15" /></router-link>
    </section>
    <section class="hub-duo">
      <article class="hub-panel rewards-panel">
        <div class="rewards-copy"><span class="stat-icon sky"><Wallet :size="20" /></span><div><p class="eyebrow">REWARDS</p><h3>{{ rewards.unlocked ? 'Free shipping unlocked' : `Spend ${money(rewards.left)} more for free shipping` }}</h3><p class="hub-muted">{{ money(spent) }} of your {{ money(FREE_SHIPPING_AT) }} free-shipping goal</p></div></div>
        <div class="rewards-bar" role="progressbar" :aria-valuenow="rewards.pct" aria-valuemin="0" aria-valuemax="100" aria-label="Free shipping progress"><i :class="{ full: rewards.unlocked }" :style="{ width: rewards.pct + '%' }"></i></div>
      </article>
      <article class="hub-panel cart-snapshot">
        <div class="rewards-copy"><span class="stat-icon peach"><ShoppingCart :size="20" /></span><div><p class="eyebrow">YOUR CART</p><h3>{{ cartCount ? `${cartCount} ${cartCount === 1 ? 'item' : 'items'} waiting` : 'Your cart is waiting' }}</h3><p class="hub-muted">{{ cartCount ? money(cartTotal) + ' subtotal' : 'Add something you love' }}</p></div></div>
        <router-link class="hub-link" :to="cartCount ? '/checkout' : '/shop'">{{ cartCount ? 'Go to checkout' : 'Start shopping' }} <ArrowUpRight :size="15" /></router-link>
      </article>
    </section>

    <section v-if="deals.length" class="hub-panel deals-panel">
      <div class="hub-section-heading"><div><p class="eyebrow">TODAY'S OFFERS</p><h2>Deals worth a look</h2></div><router-link class="hub-link" to="/shop">Shop all deals <ArrowUpRight :size="15" /></router-link></div>
      <div class="deal-grid">
        <article v-for="p in deals" :key="p._id" class="deal-card">
          <div class="deal-art" :class="p.category"><ProductArt :title="p.title" :category="p.category" /><span class="deal-flag">-{{ discount(p) }}%</span></div>
          <small>{{ p.category }}</small><h3>{{ p.title }}</h3>
          <div class="deal-bottom"><span class="price-tag"><strong>{{ money(p.price) }}</strong><del>{{ money(p.compare_at_price) }}</del></span><button class="secondary" :disabled="!p.active" @click="addToCart(p, $event)"><Plus :size="14" />Add</button></div>
        </article>
      </div>
    </section>

    <section v-if="activeOrder" class="hub-panel active-order">
      <div class="hub-section-heading"><div><p class="eyebrow">ON THE WAY TO YOU</p><h2>Order #{{ String(activeOrder.order_id).padStart(4, '0') }} is moving</h2></div><span class="status" :class="activeOrder.status.toLowerCase()">{{ statusLabel(activeOrder.status) }}</span></div>
      <ol class="track-mini">
        <li v-for="(step, i) in trackSteps" :key="step.key" :class="{ done: i < trackProgress, now: i === trackProgress }"><span>{{ step.title }}</span></li>
      </ol>
      <div class="active-order-foot"><p class="hub-muted">{{ date(activeOrder.order_date) }} · {{ activeOrder.item_count }} {{ Number(activeOrder.item_count) === 1 ? 'item' : 'items' }} · {{ money(activeOrder.total_amount) }}</p><span v-if="eta" class="eta-chip">Arrives by {{ eta }}</span><router-link class="hub-link" :to="`/account/orders/${activeOrder.order_id}`">Track this order <ArrowUpRight :size="15" /></router-link></div>
    </section>

    <nav class="hub-tabs" aria-label="Your account sections"><router-link :class="{selected: tab === 'orders'}" to="/account?tab=orders"><Package :size="17" />My orders</router-link><router-link :class="{selected: tab === 'favourites'}" to="/account?tab=favourites"><Heart :size="17" />Favourites</router-link><router-link :class="{selected: tab === 'profile'}" to="/account?tab=profile"><UserRound :size="17" />My profile</router-link></nav>

    <section v-if="tab === 'orders'" class="hub-panel">
      <div class="hub-section-heading"><div><p class="eyebrow">FROM OUR COLLECTION TO YOUR DAY</p><h2>Your order history</h2></div><button class="hub-icon" :disabled="loading" @click="load" aria-label="Refresh orders"><RotateCw :size="18" /></button></div>
      <div class="status-chips" role="group" aria-label="Filter orders by status">
        <button v-for="opt in statusOptions" :key="opt.value || 'all'" :class="{ selected: statusFilter === opt.value }" :aria-pressed="statusFilter === opt.value" @click="filterStatus(opt.value)">{{ opt.label }}</button>
      </div>
      <p v-if="message" class="notice" role="status">{{ message }} <router-link class="hub-link" to="/checkout">Review cart <ArrowUpRight :size="15" /></router-link></p>
      <div v-if="error" class="notice error" role="alert">{{ error }} <button @click="load">Try again</button></div>
      <div v-else-if="loading" class="hub-loading" role="status">Finding your orders…</div>
      <div v-else-if="!result.items.length && statusFilter" class="hub-empty"><Package :size="36" /><h3>No {{ statusLabel(statusFilter).toLowerCase() }} orders.</h3><p>Nothing here under this filter — try another one.</p><button class="secondary" @click="filterStatus('')">Show all orders</button></div>
      <div v-else-if="!result.items.length" class="hub-empty"><Package :size="36" /><h3>Your story starts here.</h3><p>Once you place an order, you can follow its progress here.</p><router-link class="primary" to="/shop">Explore the collection <ArrowUpRight :size="16" /></router-link></div>
      <div v-else class="order-list"><article v-for="order in result.items" :key="order.order_id" class="account-order"><div class="order-emblem"><Package :size="23" /></div><div class="order-copy"><router-link :to="`/account/orders/${order.order_id}`">Order #{{ String(order.order_id).padStart(4, '0') }}</router-link><p>{{ date(order.order_date) }} · {{ order.item_count }} {{ Number(order.item_count) === 1 ? 'item' : 'items' }}</p></div><span class="status" :class="order.status.toLowerCase()">{{ statusLabel(order.status) }}</span><strong>{{ money(order.total_amount) }}</strong><div class="order-actions"><router-link class="hub-link" :to="`/account/orders/${order.order_id}`">Track order <ArrowUpRight :size="15" /></router-link><button class="text-button" :disabled="buying !== null" @click="reorder(order.order_id)">{{ buying === order.order_id ? 'Adding…' : 'Buy again' }}</button></div></article></div>
      <div v-if="!error && result.total_pages > 1" class="hub-pagination"><button class="hub-icon" :disabled="loading || page <= 1" @click="page--" aria-label="Previous orders page"><ChevronLeft :size="18" /></button><span>Page {{ page }} of {{ result.total_pages }}</span><button class="hub-icon" :disabled="loading || page >= result.total_pages" @click="page++" aria-label="Next orders page"><ChevronRight :size="18" /></button></div>
    </section>

    <section v-else-if="tab === 'favourites'" class="hub-panel">
      <div class="hub-section-heading"><div><p class="eyebrow">KEEP THE GOOD ONES CLOSE</p><h2>Saved for another day</h2></div><button class="hub-icon" :disabled="favourites.loading" @click="loadFavourites" aria-label="Refresh favourites"><RotateCw :size="18" /></button></div>
      <div v-if="favourites.error" class="notice error" role="alert">{{ favourites.error }} <button @click="loadFavourites">Try again</button></div>
      <div v-else-if="favourites.loading" class="hub-loading">Loading your favourites…</div>
      <div v-else-if="!favourites.items.length" class="hub-empty"><Heart :size="36" /><h3>A place for your favourites.</h3><p>Tap a heart in the collection to save something you love.</p><router-link class="primary" to="/shop">Find something good <ArrowUpRight :size="16" /></router-link></div>
      <div v-else class="saved-grid"><article v-for="product in favourites.items" :key="product._id" class="saved-card"><div class="saved-art"><ProductArt :title="product.title" :category="product.category" /><FavouriteButton :product="product" /></div><small>{{ product.category || 'Unavailable' }}</small><h3>{{ product.title }}</h3><div class="saved-bottom"><span v-if="product.active && deal(product)" class="price-tag"><strong>{{ money(product.price) }}</strong><del>{{ money(product.compare_at_price) }}</del><em>-{{ discount(product) }}%</em></span><strong v-else>{{ product.active ? money(product.price) : 'Unavailable' }}</strong><button class="secondary" :disabled="!product.active" @click="addToCart(product, $event)"><Plus :size="16" />Add to cart</button></div></article></div>
    </section>

    <section v-else class="hub-panel profile-layout"><div><span class="profile-avatar">{{ auth.user?.name?.slice(0,1).toUpperCase() }}</span><p class="eyebrow">MAKE YOURSELF AT HOME</p><h2>Your profile</h2><p class="hub-muted">Keep your display name up to date.<br>Your orders stay connected to this account.</p></div><form class="profile-form" @submit.prevent="saveProfile"><label class="input-label">Full name<input v-model="name" required maxlength="120" autocomplete="name" :disabled="saving" /></label><label class="input-label">Email address<input :value="auth.user?.email" type="email" readonly /><small>Your sign-in email cannot be changed here.</small></label><p v-if="profileError" class="notice error" role="alert">{{ profileError }}</p><button class="primary" :disabled="saving || !name.trim() || name.trim() === auth.user?.name">{{ saving ? 'Saving…' : 'Save changes' }}<ArrowUpRight :size="17" /></button></form></section>
  </div>
</template>
