import { reactive } from 'vue'
import { api, post, store } from './api'

export const auth = reactive({ user: null })
let restoration
export function setUser(user) {
  if (!user || !['ADMIN', 'CUSTOMER'].includes(user.role) || !user.id) throw new Error('The account response is incomplete. Please contact support.')
  auth.user = user
  store.userId = user.id
  store.users = [user]
}
export function restoreSession() {
  return restoration ||= api('/auth/me').then(setUser).catch(() => {}).finally(() => { restoration = null })
}
export async function signIn(credentials) {
  const result = await post('/auth/login', credentials)
  setUser(result.user)
}
export async function signOut() {
  await post('/auth/logout', {})
  auth.user = null
  store.userId = null
  store.users = []
  store.cart = []
}
export function destination(role) { return role === 'ADMIN' ? '/admin/dashboard' : '/account' }
export function safeNext(value, role) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return destination(role)
  const path = value.split(/[?#]/)[0]
  if (path === '/checkout' || path === '/shop' || path === '/account' || /^\/account\/orders\/\d+$/.test(path) || (role === 'ADMIN' && /^\/admin\/(dashboard|search|catalog|orders\/[^/?#]+)$/.test(path))) return value
  return destination(role)
}
