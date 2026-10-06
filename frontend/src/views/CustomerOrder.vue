<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowUpRight, Check, Package, RotateCw, ShoppingBag, Truck } from 'lucide-vue-next'
import { api, money, date, statusLabel } from '../api'
import { buyAgain } from '../accountActions'
const route = useRoute(), order = ref(null), loading = ref(true), error = ref(''), buying = ref(false), message = ref('')
const steps = [{ key: 'PENDING', title: 'Order placed', text: 'Your order has been saved.', icon: ShoppingBag }, { key: 'PROCESSING', title: 'Getting ready', text: 'Your order is being prepared.', icon: Package }, { key: 'SHIPPED', title: 'Shipped', text: 'Your order is marked as shipped.', icon: Truck }]
const progress = computed(() => steps.findIndex(s => s.key === order.value?.status))
// Stale orders never show a fresh ETA: the date is suppressed once it passes.
const eta = computed(() => {
  const o = order.value
  if (!o || !['PENDING', 'PROCESSING', 'SHIPPED'].includes(o.status)) return null
  const d = new Date(o.order_date); d.setDate(d.getDate() + 5)
  return d < new Date() ? null : d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
})
let current = 0
async function load() {
  const id = ++current; loading.value = true; error.value = ''
  try { const result = await api(`/orders/${route.params.id}`); if (id === current) order.value = result }
  catch(e) { if (id === current) error.value = e.message }
  finally { if (id === current) loading.value = false }
}
async function reorder() { buying.value = true; try { message.value = await buyAgain(order.value.order_id) } catch(e) { message.value = e.message } finally { buying.value = false } }
onMounted(load); onUnmounted(() => current++)
</script>
<template>
  <div class="hub-page"><router-link class="back-link" to="/account"><ArrowLeft :size="17" />Back to your orders</router-link>
    <div v-if="error" class="notice error" role="alert">{{ error }} <button @click="load">Try again</button></div>
    <div v-else-if="loading && !order" class="hub-panel hub-loading">Loading your order…</div>
    <template v-else-if="order"><div class="hub-section-heading"><div><p class="eyebrow">A LITTLE CLOSER TO YOUR EVERYDAY</p><h1>Order #{{ String(order.order_id).padStart(4, '0') }}<span class="blue-dot">.</span></h1><p class="hub-muted">Placed {{ date(order.order_date) }}<span v-if="eta" class="eta-chip">Arrives by {{ eta }}</span></p></div><button class="secondary" :disabled="loading" @click="load"><RotateCw :size="16" />{{ loading ? 'Checking…' : 'Refresh status' }}</button></div>
      <section class="hub-panel"><div class="hub-section-heading"><h2>Your order’s progress</h2><span class="status" :class="order.status.toLowerCase()">{{ statusLabel(order.status) }}</span></div><ol class="track-timeline"><li v-for="(step,i) in steps" :key="step.key" :class="{done: i < progress, now: i === progress}" :aria-current="i === progress ? 'step' : undefined"><span class="track-marker"><Check v-if="i < progress" :size="21" /><component v-else :is="step.icon" :size="21" /></span><div class="track-copy"><h3>{{ step.title }}</h3><p>{{ i < progress ? step.text : i === progress ? step.text + ' We will update this as it moves.' : 'Not reached yet.' }}</p><small v-if="i === 0">Placed {{ date(order.order_date) }}</small><small v-else-if="i === progress">Last updated {{ date(order.updated_at) }}</small></div></li></ol><p class="hub-muted tracking-note">Status last updated {{ date(order.updated_at) }}. Live carrier tracking isn't connected in this demo; the status updates here as your order moves.</p></section>
      <section class="hub-panel"><div class="hub-section-heading"><div><p class="eyebrow">THE GOOD THINGS YOU PICKED</p><h2>Order details</h2></div><button class="secondary" :disabled="buying" @click="reorder"><RotateCw :size="16" />{{ buying ? 'Adding…' : 'Buy again' }}</button></div><p v-if="message" class="notice" role="status">{{ message }} <router-link class="hub-link" to="/checkout">Review cart <ArrowUpRight :size="15" /></router-link></p><div class="table-wrap"><table><thead><tr><th>Product</th><th>Quantity</th><th>Price paid</th><th>Total</th></tr></thead><tbody><tr v-for="item in order.items" :key="item.product_id"><td><strong>{{ item.title }}</strong></td><td>{{ item.quantity }}</td><td>{{ money(item.unit_price) }}</td><td>{{ money(item.unit_price * item.quantity) }}</td></tr></tbody></table></div><div class="receipt-total"><span>Order total</span><strong>{{ money(order.total_amount) }}</strong></div><p class="hub-muted">These are your original order prices. Buy again adds available items to your cart at today’s prices.</p></section>
    </template>
  </div>
</template>
