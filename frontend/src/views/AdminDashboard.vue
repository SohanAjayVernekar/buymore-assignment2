<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowUpRight, BarChart3, Package, Users, Mail, RefreshCw, RotateCw, ChevronLeft, ChevronRight, Search, Check, ShoppingBag, TrendingUp, Inbox, CircleCheck, AlertCircle } from 'lucide-vue-next'
import { api, post, money, date, statusLabel, notify } from '../api'
import { auth } from '../auth'
const route = useRoute()
const tabs = [{ id:'overview', label:'Overview', icon:BarChart3 },{ id:'fulfilment', label:'Fulfilment', icon:Package },{ id:'customers', label:'Customers', icon:Users },{ id:'messages', label:'Inbox', icon:Mail },{ id:'sync', label:'Search sync', icon:RefreshCw }]
const tab = computed(() => tabs.some(t => t.id === route.query.tab) ? route.query.tab : 'overview')
const overview = ref(null), data = ref({items:[],total:0,total_pages:0}), loading = ref(true), error = ref(''), actionError = ref(''), acting = ref(null)
const page = ref(1), status = ref('PENDING'), query = ref(''), resolved = ref(false)
const totals = computed(() => overview.value?.totals || {})
const summaryMoney = value => Number(value) >= 1000000 ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 }).format(value) : money(value)
const maxRevenue = computed(() => Math.max(1, ...(overview.value?.trend || []).map(d => Number(d.revenue))))
const synced = computed(() => data.value.items.filter(o => o.sync_status === 'in_sync').length)
let request=0, timer
function endpoint() {
  if(tab.value === 'overview') return '/admin/overview'
  if(tab.value === 'fulfilment') return `/admin/orders?status=${status.value}&page=${page.value}`
  if(tab.value === 'customers') return `/admin/customers?query=${encodeURIComponent(query.value.trim())}&page=${page.value}`
  if(tab.value === 'messages') return `/admin/messages?resolved=${resolved.value}&page=${page.value}`
  return '/admin/sync'
}
async function load() {
  const id = ++request, section = tab.value
  loading.value=true; error.value=''
  try { const result = await api(endpoint()); if(id===request) { if(section==='overview') overview.value=result; else data.value=result } }
  catch(e) { if(id===request) error.value=e.message }
  finally { if(id===request) loading.value=false }
}
function reload() { clearTimeout(timer); load() }
function resetAndLoad() { clearTimeout(timer); if(page.value!==1) page.value=1; else load() }
async function changeStatus(order) {
  acting.value=order.order_id; actionError.value=''
  try { const result=await post(`/orders/${order.order_id}/status`,{status:order.status==='PENDING'?'PROCESSING':'SHIPPED'},'PATCH'); notify(result.sync_status==='queue_unavailable'?'Status saved. Search sync needs attention.':'Order status updated'); if(data.value.items.length===1 && page.value>1) page.value--; else await load() }
  catch(e) { actionError.value=e.message } finally { acting.value=null }
}
async function resolve(message) {
  acting.value=message.id; actionError.value=''
  try { await post(`/admin/messages/${message.id}`,{resolved:!message.resolved},'PATCH'); notify(message.resolved?'Message reopened':'Message marked resolved'); if(data.value.items.length===1 && page.value>1) page.value--; else await load() }
  catch(e) { actionError.value=e.message } finally { acting.value=null }
}
async function retry(order) {
  acting.value=order.order_id; actionError.value=''
  try { const result=await post(`/admin/sync/${order.order_id}`,{}); if(!result.queued) throw new Error('The queue is unavailable. The order is saved; try again shortly.'); notify('Update queued. Refresh in a moment to check progress.'); await load() }
  catch(e) { actionError.value=e.message } finally { acting.value=null }
}
watch(tab, () => { data.value={items:[],total:0,total_pages:0}; actionError.value=''; resetAndLoad() })
watch([status,resolved], resetAndLoad)
watch(query, () => { clearTimeout(timer); request++; loading.value=true; timer=setTimeout(resetAndLoad,250) })
watch(page, load)
onMounted(load)
onUnmounted(() => { clearTimeout(timer); request++ })
const shortDay = value => new Intl.DateTimeFormat('en-US',{weekday:'short',timeZone:'UTC'}).format(new Date(value))
</script>

<template>
  <div class="hub-page admin-hub">
    <section class="hub-hero admin-hero"><div><p class="eyebrow">THE BUYMORE WORKSPACE</p><h1>Good things, in motion<span>.</span></h1><p>Welcome back, {{ auth.user?.name?.split(' ')[0] }}. A clear view of your store starts here.</p><router-link class="hub-link" to="/admin/catalog">Manage your collection <ArrowUpRight :size="17" /></router-link></div><div class="admin-hero-mark" aria-hidden="true"><BarChart3 :size="64" :stroke-width="1.2" /><span>MAKE EVERY DAY COUNT</span></div></section>
    <nav class="hub-tabs admin-tabs" aria-label="Admin dashboard sections"><router-link v-for="section in tabs" :key="section.id" :to="`/admin/dashboard?tab=${section.id}`" :class="{selected:tab===section.id}"><component :is="section.icon" :size="17" />{{ section.label }}</router-link></nav>
    <div v-if="error" class="notice error" role="alert">{{ error }} <button @click="reload">Try again</button></div>
    <div v-if="actionError" class="notice error" role="alert">{{ actionError }}</div>
    <div v-if="loading && tab !== 'customers'" class="hub-loading hub-panel" role="status">Bringing your store into view…</div>
    <template v-if="!error && (!loading || tab === 'customers')">
      <template v-if="tab==='overview' && overview">
        <div class="hub-stats four-stats"><article class="hub-stat"><span class="stat-icon lavender"><TrendingUp :size="21" /></span><div><small>All-time order value</small><strong :title="money(totals.revenue)" :aria-label="money(totals.revenue)">{{ summaryMoney(totals.revenue) }}</strong></div></article><article class="hub-stat"><span class="stat-icon mint"><ShoppingBag :size="21" /></span><div><small>Orders placed</small><strong>{{ totals.orders }}</strong></div></article><article class="hub-stat"><span class="stat-icon peach"><Package :size="21" /></span><div><small>Awaiting preparation</small><strong>{{ totals.pending }}</strong></div></article><article class="hub-stat"><span class="stat-icon sky"><Users :size="21" /></span><div><small>Customer accounts</small><strong>{{ totals.customers }}</strong></div></article></div>
        <div class="overview-grid"><section class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">THE LAST SEVEN DAYS</p><h2>A little momentum</h2></div><button class="hub-icon" @click="reload" aria-label="Refresh overview"><RotateCw :size="18" /></button></div><p class="hub-muted">Daily order value · USD · store time</p><div class="revenue-chart" role="img" :aria-label="overview.trend.map(d => `${date(d.day)}: ${money(d.revenue)}, ${d.orders} orders`).join('; ')"><div v-for="day in overview.trend" :key="day.day" class="revenue-day"><span>{{ money(day.revenue) }}</span><div class="bar-track"><i :style="{height: `${Number(day.revenue) ? Math.max(3, Number(day.revenue)/maxRevenue*100) : 0}%`}"></i></div><small>{{ shortDay(day.day) }}</small></div></div></section><aside class="hub-panel attention-panel"><span class="stat-icon mint"><CircleCheck :size="24" /></span><p class="eyebrow">NEXT UP</p><h2>A little care.<br> A happier customer.</h2><router-link to="/admin/dashboard?tab=fulfilment"><span><strong>{{ totals.pending }}</strong> orders to prepare</span><ArrowUpRight :size="18" /></router-link><router-link to="/admin/dashboard?tab=messages"><span><strong>{{ totals.messages }}</strong> open messages</span><ArrowUpRight :size="18" /></router-link><p>{{ totals.processing }} processing · {{ totals.shipped }} shipped</p></aside></div>
        <section class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">FRESH FROM THE STOREFRONT</p><h2>Recent orders</h2></div><router-link class="hub-link" to="/admin/search">Search all orders <ArrowUpRight :size="16" /></router-link></div><div v-if="!overview.recent.length" class="hub-empty"><ShoppingBag :size="32" /><h3>Ready for the first order.</h3><p>New orders will appear here as customers check out.</p></div><div v-else class="table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Date</th><th>Status</th><th>Value</th></tr></thead><tbody><tr v-for="order in overview.recent" :key="order.order_id"><td><router-link class="hub-link" :to="`/admin/orders/${order.order_id}`">#{{ order.order_id }} <ArrowUpRight :size="13" /></router-link></td><td>{{ order.name }}</td><td>{{ date(order.order_date) }}</td><td><span class="status" :class="order.status.toLowerCase()">{{ statusLabel(order.status) }}</span></td><td>{{ money(order.total_amount) }}</td></tr></tbody></table></div></section>
      </template>

      <section v-else-if="tab==='fulfilment'" class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">ONE GOOD ORDER AT A TIME</p><h2>Fulfilment queue</h2><p class="hub-muted">Oldest orders first, so nobody gets left behind.</p></div><button class="hub-icon" @click="reload" aria-label="Refresh fulfilment"><RotateCw :size="18" /></button></div><div class="hub-segments" aria-label="Filter orders by status"><button v-for="s in ['PENDING','PROCESSING','SHIPPED']" :key="s" :class="{selected:status===s}" :aria-pressed="status===s" @click="status=s">{{ statusLabel(s) }}</button></div><div v-if="!data.items.length" class="hub-empty"><Check :size="36" /><h3>All clear here.</h3><p>No {{ status.toLowerCase() }} orders right now.</p></div><div v-else class="table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Placed</th><th>Value</th><th>Next step</th></tr></thead><tbody><tr v-for="order in data.items" :key="order.order_id"><td><router-link class="hub-link" :to="`/admin/orders/${order.order_id}`">#{{ order.order_id }} <ArrowUpRight :size="13" /></router-link></td><td><strong>{{ order.name }}</strong><small class="table-subline">{{ order.email }}</small></td><td>{{ date(order.order_date) }}</td><td>{{ money(order.total_amount) }}</td><td><button v-if="status!=='SHIPPED'" class="secondary compact-action" :disabled="acting!==null" @click="changeStatus(order)">{{ acting===order.order_id ? 'Saving…' : status==='PENDING' ? 'Start preparing' : 'Mark shipped' }}</button><span v-else class="status shipped">Shipped</span></td></tr></tbody></table></div></section>

      <section v-else-if="tab==='customers'" class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">THE PEOPLE BEHIND THE ORDERS</p><h2>Your customers</h2></div><button class="hub-icon" @click="reload" aria-label="Refresh customers"><RotateCw :size="18" /></button></div><label class="search-input directory-search"><Search :size="18" /><input v-model="query" maxlength="200" placeholder="Find a name or email…" aria-label="Search customers" /></label><p class="hub-muted">{{ data.total }} customer{{ data.total===1?'':'s' }}</p><div v-if="!data.items.length" class="hub-empty"><Users :size="36" /><h3>{{ query ? 'No matching customers.' : 'A community in the making.' }}</h3><p>{{ query ? 'Try a different name or email.' : 'Customer accounts will appear here after registration.' }}</p></div><div v-else class="table-wrap"><table><thead><tr><th>Customer</th><th>Email</th><th>Orders</th><th>Total order value</th><th>Last order</th></tr></thead><tbody><tr v-for="customer in data.items" :key="customer.id"><td><strong>{{ customer.name }}</strong></td><td>{{ customer.email }}</td><td>{{ customer.orders }}</td><td>{{ money(customer.spent) }}</td><td>{{ customer.last_order ? date(customer.last_order) : 'No orders yet' }}</td></tr></tbody></table></div></section>

      <section v-else-if="tab==='messages'" class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">GOOD CONVERSATIONS START HERE</p><h2>Customer inbox</h2><p class="hub-muted">Messages submitted through Contact Us. Resolving a message does not send a reply.</p></div><button class="hub-icon" @click="reload" aria-label="Refresh messages"><RotateCw :size="18" /></button></div><div class="hub-segments" aria-label="Filter messages"><button :class="{selected:!resolved}" :aria-pressed="!resolved" @click="resolved=false">Open</button><button :class="{selected:resolved}" :aria-pressed="resolved" @click="resolved=true">Resolved</button></div><div v-if="!data.items.length" class="hub-empty"><Inbox :size="36" /><h3>{{ resolved ? 'No resolved messages yet.' : 'You’re all caught up.' }}</h3><p>{{ resolved ? 'Resolved conversations will live here.' : 'New customer messages will appear here.' }}</p></div><div v-else class="message-list"><article v-for="message in data.items" :key="message.id" class="message-card"><div class="message-heading"><span class="message-avatar">{{ message.name.slice(0,1).toUpperCase() }}</span><div><h3>{{ message.subject }}</h3><p>{{ message.name }} · {{ message.email }}</p></div><time>{{ date(message.created_at) }}</time></div><p class="message-body">{{ message.message }}</p><button class="secondary compact-action" :disabled="acting!==null" @click="resolve(message)"><Check :size="15" />{{ acting===message.id ? 'Saving…' : message.resolved ? 'Reopen message' : 'Mark resolved' }}</button></article></div></section>

      <section v-else-if="tab==='sync'" class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">KEEP THE DETAILS UP TO DATE</p><h2>Order search health</h2><p class="hub-muted">Checking the latest {{ data.checked }} updated orders out of {{ data.total }} saved orders.</p></div><button class="secondary" @click="reload"><RotateCw :size="16" />Check again</button></div><div class="sync-summary" :class="{warning:data.state==='unavailable'}"><component :is="data.state==='available' ? CircleCheck : AlertCircle" :size="27" /><div><strong>{{ data.state==='available' ? `${synced} of ${data.checked} checked orders are up to date` : 'Search is currently unavailable' }}</strong><p>Orders remain saved. Retry an update to send the latest saved version to search.</p></div></div><div v-if="!data.items.length" class="hub-empty"><RefreshCw :size="34" /><h3>No orders to check yet.</h3><p>Search synchronization begins with your first order.</p></div><div v-else class="table-wrap"><table><thead><tr><th>Order</th><th>Last updated</th><th>Search status</th><th>Action</th></tr></thead><tbody><tr v-for="order in data.items" :key="order.order_id"><td><router-link class="hub-link" :to="`/admin/orders/${order.order_id}`">#{{ order.order_id }} <ArrowUpRight :size="13" /></router-link></td><td>{{ date(order.updated_at) }}</td><td><span class="status" :class="order.sync_status==='in_sync'?'shipped':'pending'">{{ {in_sync:'Up to date',pending:'Update needed',unavailable:'Unavailable'}[order.sync_status] }}</span></td><td><button v-if="order.sync_status!=='in_sync'" class="secondary compact-action" :disabled="acting!==null" @click="retry(order)">{{ acting===order.order_id ? 'Queuing…' : 'Retry update' }}</button><Check v-else :size="18" class="sync-check" /></td></tr></tbody></table></div></section>
      <div v-if="!['overview','sync'].includes(tab) && data.total_pages>1" class="hub-pagination"><button class="hub-icon" :disabled="page<=1" @click="page--" aria-label="Previous dashboard page"><ChevronLeft :size="18" /></button><span>Page {{ page }} of {{ data.total_pages }}</span><button class="hub-icon" :disabled="page>=data.total_pages" @click="page++" aria-label="Next dashboard page"><ChevronRight :size="18" /></button></div>
    </template>
  </div>
</template>
