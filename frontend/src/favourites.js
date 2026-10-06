import { reactive, watch } from 'vue'
import { auth } from './auth'
import { api, post } from './api'

export const favourites = reactive({ items: [], loading: false, error: '', busy: [] })
let generation = 0
export async function loadFavourites() {
  const request = ++generation
  favourites.error = ''
  if (!auth.user) { favourites.items = []; favourites.loading = false; return }
  favourites.loading = true
  try {
    const result = await api('/account/favourites')
    if (request === generation) favourites.items = result.items
  } catch (e) { if (request === generation) favourites.error = e.message }
  finally { if (request === generation) favourites.loading = false }
}
watch(() => auth.user?.id, () => { favourites.items = []; loadFavourites() }, { immediate: true })
export async function toggleFavourite(product) {
  if (favourites.busy.includes(product._id)) return
  const owner = auth.user?.id
  favourites.busy.push(product._id)
  try {
    const saved = favourites.items.some(p => p._id === product._id)
    await post('/account/favourites/' + encodeURIComponent(product._id), {}, saved ? 'DELETE' : 'PUT')
    if (owner !== auth.user?.id) return
    favourites.items = saved ? favourites.items.filter(p => p._id !== product._id) : [product, ...favourites.items]
  } finally { favourites.busy = favourites.busy.filter(id => id !== product._id) }
}
