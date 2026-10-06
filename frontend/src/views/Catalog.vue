<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { Plus, Pencil, X, Search, Check, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { api, post, money, notify } from '../api'
import ProductArt from '../components/ProductArt.vue'
import SelectMenu from '../components/SelectMenu.vue'
const products = ref([]), query = ref(''), loading = ref(true), error = ref(''), editError = ref(''), editing = ref(false), saving = ref(false), editId = ref(null), form = ref({})
const page = ref(1), pageSize = ref(24), total = ref(0), totalPages = ref(0), paginationLoading = ref(false)
const catalogCounts = ref({active: 0, inactive: 0})
let requestId = 0, searchTimer
async function load(isPagination = false) {
  const id = ++requestId
  if (!isPagination) loading.value = true
  else paginationLoading.value = true
  try {
    const params = new URLSearchParams({include_inactive: 'true', page: page.value, page_size: pageSize.value, query: query.value.trim()})
    const res = await api(`/products?${params}`)
    if (id !== requestId) return
    products.value = res.items
    total.value = res.total
    totalPages.value = res.total_pages
    catalogCounts.value = res.catalog_counts
    error.value = ''
  } catch(e) { if (id === requestId) error.value = e.message }
  finally { if (id === requestId) { loading.value = false; paginationLoading.value = false } }
}
watch(query, () => {
  clearTimeout(searchTimer)
  requestId++
  searchTimer = setTimeout(() => {
    if (page.value !== 1) page.value = 1
    else load()
  }, 250)
})
watch(page, () => { clearTimeout(searchTimer); load(true) })
// Range line: correct on every page (catalog always swaps, never appends)
const rangeText = computed(() => products.value.length
  ? `Showing ${(page.value - 1) * pageSize.value + 1}\u2013${(page.value - 1) * pageSize.value + products.value.length} of ${total.value} products`
  : `Showing 0 of ${total.value} products`)
// Page strip: 1 … 4 5 6 … 105 (elisions trimmed in)
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
onUnmounted(() => { clearTimeout(searchTimer); requestId++ })
function edit(product) {editId.value = product?._id || null; editError.value = ''; form.value = product ? {...product, tags: product.tags.join(', '), attributes: JSON.stringify(product.attributes, null, 2), variants: JSON.stringify(product.variants, null, 2)} : {sku: '', title: '', description: '', price: '', category: 'peripherals', tags: '', attributes: '{\n  "color": "graphite",\n  "brand": "BuyMore"\n}', variants: '[\n  {"sku": "", "color": "graphite", "stock": 0}\n]', active: true}; editing.value = true}
async function save() {saving.value = true; editError.value = ''; try {const data = {...form.value, price: Number(form.value.price), tags: form.value.tags.split(',').map(t => t.trim()).filter(Boolean), attributes: JSON.parse(form.value.attributes), variants: JSON.parse(form.value.variants)}; delete data._id; delete data.updated_at; await post(editId.value ? `/products/${editId.value}` : '/products', data, editId.value ? 'PUT' : 'POST'); editing.value = false; notify(editId.value ? 'Product updated' : 'Product created'); await load()} catch(e) {editError.value = e instanceof SyntaxError ? 'Attributes and variants must contain valid JSON.' : e.message} finally {saving.value = false}}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><p class="eyebrow">A COLLECTION WORTH CURATING</p><h1>Product catalog<span class="blue-dot">.</span></h1></div><button class="primary" @click="edit(null)"><Plus :size="17"/>New product</button></div>
  <div class="catalog-toolbar" v-reveal><p>{{catalogCounts.active}} active products <span class="subtle">· {{catalogCounts.inactive}} inactive</span></p><label class="search-input"><Search :size="17"/><input v-model="query" placeholder="Search title or SKU…" aria-label="Search catalog"/></label></div>
  <div v-if="error" class="notice error" role="alert">{{error}} <button @click="load()">Retry</button></div>
  <div v-else-if="loading" class="loading">Loading the collection…</div>
  <div v-else>
    <div class="catalog-grid" v-reveal><article v-for="p in products" :key="p._id" class="catalog-card"><div class="catalog-card-art"><ProductArt :title="p.title" :category="p.category"/><span class="status" :class="p.active ? 'shipped' : 'pending'">{{p.active ? 'Active' : 'Inactive'}}</span></div><div class="catalog-card-info"><small>{{p.sku}}</small><h3>{{p.title}}</h3><p class="capitalize">{{p.category}} &middot; {{p.variants.length}} {{p.variants.length === 1 ? 'option' : 'options'}}</p><div class="catalog-card-bottom"><strong>{{money(p.price)}}</strong><button class="icon-button" @click="edit(p)" :aria-label="`Edit ${p.title}`"><Pencil :size="16"/></button></div></div></article><div v-if="!products.length" class="empty compact">No products match your search.</div></div>
    <div class="pagination pagination-pages" v-reveal><span class="page-info">{{ rangeText }}</span><div class="page-buttons"><button class="icon-button" :disabled="page <= 1 || paginationLoading" @click="page = 1" aria-label="First page">&laquo;</button><button class="icon-button" :disabled="page <= 1 || paginationLoading" @click="page--" aria-label="Previous page"><ChevronLeft :size="16"/></button><template v-for="(n, i) in pageNumbers" :key="i"><span v-if="n === '…'" class="page-ellipsis">…</span><button v-else class="page-number" :class="{current: n === page}" :disabled="paginationLoading" @click="page = n">{{n}}</button></template><button class="icon-button" :disabled="page >= totalPages || paginationLoading" @click="page++" aria-label="Next page"><ChevronRight :size="16"/></button><button class="icon-button" :disabled="page >= totalPages || paginationLoading" @click="page = totalPages" aria-label="Last page">&raquo;</button></div></div>
  </div>
  <Teleport to="body"><div v-if="editing" class="modal-overlay" @click.self="!saving && (editing = false)" @keydown.esc="!saving && (editing = false)"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="product-dialog-title"><div class="modal-heading"><div><p class="eyebrow">THE EVERYDAY COLLECTION</p><h2 id="product-dialog-title">{{editId ? 'Edit product' : 'A new addition'}}</h2></div><button class="icon-button" :disabled="saving" @click="editing = false" aria-label="Close editor"><X :size="22"/></button></div><form @submit.prevent="save"><div class="modal-body"><p class="form-section">BASICS</p><div class="form-grid"><label class="input-label wide">Product title<input v-model="form.title" required maxlength="200"/></label><label class="input-label">SKU<input v-model="form.sku" required/></label><label class="input-label">Price ($)<input v-model="form.price" type="number" min="0" max="1000000" step="0.01" required/></label><label class="input-label wide">Description<textarea v-model="form.description" required rows="2"></textarea></label><div class="input-label">Category<SelectMenu v-model="form.category" label="Category" :options="[...new Set(['peripherals','audio','cables','office',form.category])].map(c => ({value:c,label:c.charAt(0).toUpperCase()+c.slice(1)}))"/></div><label class="input-label">Tags, separated by commas<input v-model="form.tags" placeholder="wireless, bluetooth"/></label></div><p class="form-section">STORE DATA</p><div class="form-grid"><label class="input-label wide"><span class="label-line">Attributes <i class="code-chip">{ }</i></span><textarea v-model="form.attributes" class="code-input" rows="4" required spellcheck="false"></textarea></label><label class="input-label wide"><span class="label-line">Variants <i class="code-chip">[ ]</i></span><textarea v-model="form.variants" class="code-input" rows="4" required spellcheck="false"></textarea></label><label class="checkbox-row wide"><input v-model="form.active" type="checkbox"/>Visible in storefront</label></div><p class="subtle">Changes apply to the catalog. Previously placed orders keep their original names and prices.</p><div v-if="editError" class="notice error" role="alert">{{editError}}</div></div><div class="modal-actions"><button class="secondary" type="button" @click="editing = false">Cancel</button><button class="primary" :disabled="saving" type="submit">{{saving ? 'Saving…' : (editId ? 'Update' : 'Create')}}</button></div></form></section></div></Teleport>
</template>
