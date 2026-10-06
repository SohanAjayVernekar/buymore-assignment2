<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Heart } from 'lucide-vue-next'
import { auth } from '../auth'
import { notify } from '../api'
import { favourites, toggleFavourite } from '../favourites'
const props = defineProps({ product: { type: Object, required: true } })
const router = useRouter()
const saved = computed(() => favourites.items.some(p => p._id === props.product._id))
async function toggle() {
  if (!auth.user) { router.push({ path: '/login', query: { next: '/shop' } }); return }
  try { await toggleFavourite(props.product); notify(saved.value ? 'Saved to your favourites' : 'Removed from favourites') }
  catch(e) { notify(e.message) }
}
</script>
<template><button class="favourite-button" :class="{saved}" :aria-pressed="saved" :aria-label="`${saved ? 'Remove' : 'Save'} ${product.title} ${saved ? 'from' : 'to'} favourites`" :disabled="favourites.loading || favourites.busy.includes(product._id)" @click="toggle"><Heart :size="18" :fill="saved ? 'currentColor' : 'none'" /></button></template>
<style scoped>
.favourite-button{display:grid;place-items:center;width:38px;height:38px;border-radius:50%;background:#fffffff0;color:#596b59;box-shadow:0 4px 12px #283c2310;transition:transform .2s,color .2s}
.favourite-button:hover{transform:translateY(-2px);color:#ad4165}.favourite-button.saved{background:#fceaf0;color:#ad4165}
@media(prefers-reduced-motion:reduce){.favourite-button{transition:none}.favourite-button:hover{transform:none}}
</style>
