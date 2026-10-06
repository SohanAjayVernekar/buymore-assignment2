<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowUpRight, ArrowRight, Search, SlidersHorizontal, Plus, Check, Grid2X2, Headphones, Cable, Keyboard, LampDesk, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { api, addToCart as apiAddToCart, money, deal, discount, store, notify } from '../api'
import { auth } from '../auth'
import ProductArt from '../components/ProductArt.vue'
import FavouriteButton from '../components/FavouriteButton.vue'
const route = useRoute()
const router = useRouter()
const initialCategory = ['peripherals', 'audio', 'cables', 'office'].includes(route.query.category) ? route.query.category : ''
const products = ref([]), category = ref(initialCategory), query = ref(''), loading = ref(true), error = ref(''), showFilters = ref(false), maxPrice = ref(250)
const page = ref(1), pageSize = ref(24), total = ref(0), totalPages = ref(0), paginationLoading = ref(false), shownFrom = ref(1)
// Checkbox toggle: when off, only the pagination buttons page through results.
const infinite = ref(true)
const categories = [{id: '', label: 'All', icon: Grid2X2}, {id: 'peripherals', label: 'Peripherals', icon: Keyboard}, {id: 'audio', label: 'Audio', icon: Headphones}, {id: 'cables', label: 'Cables', icon: Cable}, {id: 'office', label: 'Office', icon: LampDesk}]
const featured = ref([])
async function loadFeatured() {
  try {
    const [a, b] = await Promise.all([
      api('/products?query=' + encodeURIComponent('Noise Cancelling Headphones') + '&page_size=1'),
      api('/products?query=' + encodeURIComponent('Wireless Mouse Pro') + '&page_size=1')
    ])
    featured.value = [a.items?.[0], b.items?.[0]].filter(Boolean)
  } catch { featured.value = [] }
}
// Add to cart with authentication check: visitors are sent to login with the
// chosen product and quantity remembered, then returned to the same collection.
async function addToCart(product, event) {
  if (!auth.user) {
    const params = new URLSearchParams(Object.fromEntries(Object.entries(route.query).map(([k, v]) => [k, String(v)])))
    params.set('productId', product._id)
    params.set('qty', '1')
    router.push({ path: '/login', query: { next: route.path + '?' + params.toString() } })
    return
  }
  await apiAddToCart(product, event)
}

// After login or registration the visitor returns here with the remembered
// product; add it exactly once, then clean the URL.
let restored = false
watch(
  () => [route.query.productId, auth.user],
  async ([productId, user]) => {
    if (restored || !productId || !user) return
    restored = true
    const quantity = Math.min(100, Math.max(1, parseInt(route.query.qty, 10) || 1))
    let product
    try { product = await api('/products/' + encodeURIComponent(productId)) }
    catch { notify('This product is no longer available.') }
    if (product) for (let i = 1; i <= quantity; i++) await apiAddToCart(product, null)
    const query = { ...route.query }
    delete query.productId
    delete query.qty
    router.replace({ path: route.path, query })
  },
  { immediate: true }
)
const browsing = computed(() => category.value || query.value || showFilters.value)
// Loads the catalog. mode = 'reset' (fresh grid + skeleton), 'page' (swap in
// place) or 'append' (infinite scroll: keep what is already on screen).
// Each call takes a sequence token; replies from superseded requests are
// dropped so an older, slower response can never overwrite a newer search.
let loadSeq = 0
async function load(mode = 'reset') {
  const seq = ++loadSeq
  if (mode === 'reset') loading.value = true
  else paginationLoading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, page_size: pageSize.value })
    if (category.value) params.append('category', category.value)
    if (query.value) params.append('query', query.value)
    if (showFilters.value) params.append('max_price', maxPrice.value)
    const res = await api(`/products?${params.toString()}`)
    if (seq !== loadSeq) return
    products.value = mode === 'append' ? [...products.value, ...res.items] : res.items
    if (mode !== 'append') shownFrom.value = products.value.length ? (page.value - 1) * pageSize.value + 1 : 0
    total.value = res.total
    totalPages.value = res.total_pages
    error.value = ''
  } catch (e) {
    if (seq !== loadSeq) return
    error.value = e.message
    appendNext = false
  } finally {
    if (seq === loadSeq) {
      loading.value = false
      paginationLoading.value = false
      if (mode === 'append') nextTick(maybeMore)
    }
  }
}
function explore(id) { category.value = id; page.value = 1; load() }
watch(() => category.value, () => { page.value = 1; load() })
// Debounced so a burst of keystrokes fires one request instead of one per
// letter (each reset used to blow away the grid — and the focused input).
let queryTimer = null
watch(() => query.value, () => {
  clearTimeout(queryTimer)
  queryTimer = setTimeout(() => { page.value = 1; load() }, 300)
})
watch(() => showFilters.value, () => { page.value = 1; load() })
watch(() => maxPrice.value, () => { page.value = 1; load() })
// Pagination and infinite scroll share one source of truth: the page number.
// Scrolling near the bottom bumps `page` with appendNext set (grid grows);
// the page controls swap the grid and take you back to the top of it.
let appendNext = false
watch(() => page.value, () => {
  if (appendNext) { appendNext = false; load('append'); return }
  load('page'); scrollToGrid()
})
function scrollToGrid() { document.querySelector('.collection-header')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
const sentinel = ref(null)
function loadMore() {
  if (!infinite.value || loading.value || paginationLoading.value || page.value >= totalPages.value) return
  appendNext = true
  page.value++
}
// Ticking the box mid-scroll picks up where the sentinel left off.
watch(infinite, on => { if (on) nextTick(maybeMore) })
function maybeMore() {
  const el = sentinel.value
  if (!el || loading.value || paginationLoading.value || page.value >= totalPages.value) return
  if (el.getBoundingClientRect().top < window.innerHeight + 400) loadMore()
}
let observer = null
watch(sentinel, el => {
  if (observer) { observer.disconnect(); observer = null }
  if (!el) return
  observer = new IntersectionObserver(entries => { if (entries.some(e => e.isIntersecting)) loadMore() }, { rootMargin: '400px 0px' })
  observer.observe(el)
})
// Range line stays correct on every page and after infinite-scroll appends
const rangeText = computed(() => products.value.length
  ? `Showing ${shownFrom.value}\u2013${shownFrom.value + products.value.length - 1} of ${total.value} products`
  : `Showing 0 of ${total.value} products`)
// Page number strip: 1 … 4 5 6 … 105 (with elisions trimmed in)
const pageNumbers = computed(() => {
  const tp = totalPages.value, cur = page.value, out = []
  if (tp <= 7) { for (let i = 1; i <= tp; i++) out.push(i); return out }
  out.push(1)
  if (cur > 3) out.push('…')
  let from = Math.max(2, cur - 1), to = Math.min(tp - 1, cur + 1)
  if (cur === 1) { from = 2; to = Math.min(tp - 1, 3) }
  if (cur >= tp - 2) { from = Math.max(2, tp - 3); to = tp - 1 }
  for (let i = from; i <= to; i++) out.push(i)
  if (to < tp - 1) out.push('…')
  out.push(tp)
  return out
})
const tabsEl = ref(null), tabStyle = ref({ transform: 'translateX(0px)', width: '0px' })
function placeTab() {
  const btn = tabsEl.value?.querySelector('button.selected')
  if (btn) tabStyle.value = { transform: `translateX(${btn.offsetLeft}px)`, width: `${btn.offsetWidth}px` }
}
watch(category, () => nextTick(placeTab))
onMounted(() => { load(); loadFeatured(); placeTab(); window.addEventListener('resize', placeTab); document.fonts?.ready.then(() => placeTab()) })
onUnmounted(() => { window.removeEventListener('resize', placeTab); observer?.disconnect() })
</script>
<template>
  <section class="page-heading"><div><p class="eyebrow">FIND YOUR NEXT FAVOURITE</p><h1>Explore<span class="blue-dot">.</span></h1></div><div class="category-tabs" ref="tabsEl"><button v-for="c in categories" :key="c.id" :class="{selected: category === c.id}" @click="explore(c.id)"><component :is="c.icon" :size="15"/>{{c.label}}</button><i class="tab-slide" :style="tabStyle"></i></div><button class="filter-button" :class="{chosen: showFilters}" @click="showFilters = !showFilters"><SlidersHorizontal :size="15"/>Filters</button></section>
  <div v-if="error" class="notice error" role="alert">{{error}} <button @click="load()">Try again</button></div>
  <div v-else-if="loading && !products.length" class="loading">Finding your next favourite…</div>
  <template v-else>
    <div v-if="!browsing" class="editorial-grid">
      <div class="editorial-stack">
        <button class="banner mint-banner" @click="explore('peripherals')"><span class="banner-copy"><small>SMALL UPGRADES. BIG DIFFERENCE.</small><strong>Make room for<br>better everyday.</strong><span class="pill">Explore essentials <ArrowUpRight :size="14"/></span></span><ProductArt title="Wireless Keyboard"/></button>
        <button class="banner yellow-banner" @click="explore('audio')"><span class="banner-copy"><small>TUNE INTO YOUR WORLD</small><strong>Your day.<br>Your soundtrack.</strong><span>Find your perfect sound <ArrowRight :size="15"/></span></span><ProductArt title="Wireless Earbuds"/><span class="round-arrow"><ArrowUpRight :size="18"/></span></button>
      </div>
      <article v-for="(p, i) in featured" :key="p._id" class="featured-product" :class="i ? 'sand-card' : 'lavender-card'">
        <div class="product-top"><span class="swatches"><i></i><i></i></span><span class="mini-label">{{ i ? 'Everyday essential' : 'Our pick' }}</span></div><ProductArt :title="p.title" :category="p.category"/>
        <div class="featured-bottom"><div><small>{{p.category}}</small><h3>{{p.title}}</h3></div><button class="price-button" :class="{added: store.recentlyAdded === p._id}" @click="addToCart(p, $event)" :aria-label="`Add ${p.title} to cart`"><span v-if="deal(p)" class="price-tag"><strong>{{money(p.price)}}</strong><del>{{money(p.compare_at_price)}}</del></span><strong v-else>{{money(p.price)}}</strong> <Check v-if="store.recentlyAdded === p._id" :size="13"/><Plus v-else :size="13"/></button></div>
      </article>
      <button class="desk-banner" @click="explore('office')"><div><small>A SPACE THAT WORKS FOR YOU</small><h2>Good design.<br>Better workdays.</h2><span>Discover office essentials <ArrowUpRight :size="16"/></span></div><ProductArt title="Desk Lamp LED"/></button>
      <button class="collection-banner" @click="explore('cables')"><div><small>STAY CONNECTED</small><h2>Less clutter.<br>More possibility.</h2><span>Shop cables & hubs <ArrowUpRight :size="16"/></span></div><ProductArt title="USB-C Hub 7-in-1"/></button>
    </div>
    <section class="collection-header" v-reveal><div><p class="eyebrow">THE EVERYDAY COLLECTION</p><h2>{{ category ? categories.find(c => c.id === category).label : "A few things you'll love" }} <span class="count">{{total}}</span></h2></div><div class="collection-tools"><label class="infinite-toggle"><input type="checkbox" v-model="infinite"/>Infinite scroll</label><label class="search-input"><Search :size="17"/><input v-model="query" placeholder="Find something good…" aria-label="Search products"/></label></div></section>
    <div v-if="showFilters" class="filter-strip"><label>Maximum price <strong>{{money(maxPrice)}}</strong><input v-model.number="maxPrice" type="range" min="0" max="250" step="5"/></label><button class="text-button" @click="maxPrice = 250; category = ''; query = ''">Reset filters</button></div>
    <div v-if="!products.length" class="empty"><Search :size="32"/><h3>No products found</h3><p>Try another search or a different category.</p><button class="secondary" @click="category = ''; query = ''; maxPrice = 250">Clear filters</button></div>
    <div class="product-grid"><article v-reveal v-for="p in products" :key="p._id" class="product-card"><div class="product-image" :class="p.category"><FavouriteButton :product="p"/><span class="mini-label">{{p.attributes.color}}</span><ProductArt :title="p.title" :category="p.category"/></div><div class="product-info"><small>{{p.category}} <span>· {{p.attributes.brand}}</span></small><h3>{{p.title}}</h3><p>{{p.description}}</p><div class="product-bottom"><span v-if="deal(p)" class="price-tag"><strong>{{money(p.price)}}</strong><del>{{money(p.compare_at_price)}}</del><em>-{{discount(p)}}%</em></span><strong v-else>{{money(p.price)}}</strong><button class="add-button" :class="{added: store.recentlyAdded === p._id}" @click="addToCart(p, $event)" :aria-label="`Add ${p.title} to cart`"><Check v-if="store.recentlyAdded === p._id" :size="16"/><Plus v-else :size="16"/> {{store.recentlyAdded === p._id ? 'Added' : 'Add'}}</button></div></div></article></div>
    <div ref="sentinel" class="scroll-sentinel" aria-hidden="true"><span v-if="paginationLoading">Loading more…</span></div>
    <div class="pagination pagination-pages" v-reveal><span class="page-info">{{ rangeText }}</span><div class="page-buttons"><button class="icon-button" :disabled="page <= 1 || paginationLoading" @click="page = 1" aria-label="First page">&laquo;</button><button class="icon-button" :disabled="page <= 1 || paginationLoading" @click="page--" aria-label="Previous page"><ChevronLeft :size="16"/></button><template v-for="(n, i) in pageNumbers" :key="i"><span v-if="n === '…'" class="page-ellipsis">…</span><button v-else class="page-number" :class="{current: n === page}" :disabled="paginationLoading" @click="page = n">{{n}}</button></template><button class="icon-button" :disabled="page >= totalPages || paginationLoading" @click="page++" aria-label="Next page"><ChevronRight :size="16"/></button><button class="icon-button" :disabled="page >= totalPages || paginationLoading" @click="page = totalPages" aria-label="Last page">&raquo;</button></div></div>
  </template>
</template>
