<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, ShieldCheck, ShoppingBag } from 'lucide-vue-next'
import { post } from '../api'
import { auth, signIn, safeNext } from '../auth'
const route = useRoute(), router = useRouter()
const admin = computed(() => route.path.startsWith('/admin/'))
const register = computed(() => route.path.endsWith('/register'))
const role = computed(() => admin.value ? 'ADMIN' : 'CUSTOMER')
const base = computed(() => admin.value ? '/admin' : '')
const name = ref(''), email = ref(''), password = ref(''), confirm = ref(''), invitation = ref(''), visible = ref(false), busy = ref(false), error = ref(''), success = ref('')
watch(() => route.path, () => { name.value = ''; email.value = ''; password.value = ''; confirm.value = ''; invitation.value = ''; error.value = ''; success.value = ''; visible.value = false })
async function submit() {
  error.value = ''; success.value = ''
  if (register.value && !name.value.trim()) { error.value = 'Please enter your full name.'; return }
  if (register.value && password.value !== confirm.value) { error.value = 'Your passwords do not match.'; return }
  if (register.value && admin.value && !invitation.value.trim()) { error.value = 'Enter your admin invitation code.'; return }
  busy.value = true
  try {
    if (register.value) {
      await post('/auth/register', { name: name.value.trim(), email: email.value.trim(), password: password.value, role: role.value, ...(admin.value ? { invitationCode: invitation.value.trim() } : {}) })
      password.value = ''; confirm.value = ''; invitation.value = ''
      success.value = 'Your account is ready. Sign in to get started.'
    } else {
      await signIn({ email: email.value.trim(), password: password.value, role: role.value })
      await router.replace(safeNext(route.query.next, auth.user.role))
    }
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
const TEST_ACCOUNTS = {
  customer: { email: 'customer@buymore.test', password: 'Customer-demo-42' },
  admin: { email: 'admin@buymore.test', password: 'Admin-demo-42' }
}
function fillTest(kind) {
  const account = TEST_ACCOUNTS[kind]
  email.value = account.email; password.value = account.password
  visible.value = false; error.value = ''; success.value = ''
}
async function useTest(kind) {
  const wanted = kind === 'admin' ? 'ADMIN' : 'CUSTOMER'
  if (!register.value && role.value === wanted) { fillTest(kind); submit(); return }
  await router.replace(kind === 'admin' ? { path: '/admin/login', query: {} } : { path: '/login', query: route.query })
  await nextTick()
  fillTest(kind)
  submit()
}
</script>
<template>
  <section class="auth-layout">
    <aside class="auth-story"><span class="welcome-icon"><ShieldCheck v-if="admin" :size="27"/><ShoppingBag v-else :size="27"/></span><p class="eyebrow">{{admin ? 'THE BUYMORE TEAM' : 'YOUR EVERYDAY, UPGRADED'}}</p><h1>{{admin ? 'Good work.\nGreat possibilities.' : 'Your next favourite\nstarts here.'}}</h1><p>{{admin ? 'A dedicated space to care for your collection and your customers.' : 'Thoughtful essentials, fresh discoveries, and a little more you.'}}</p><div class="auth-story-note"><span>BUYMORE</span><strong>A little more.<br>A little better.</strong></div></aside>
    <div class="auth-panel"><div class="auth-role-tabs" aria-label="Account type"><router-link :class="{selected: !admin}" :to="{path: register ? '/register' : '/login', query: route.query}">Customer</router-link><router-link :class="{selected: admin}" :to="{path: register ? '/admin/register' : '/admin/login', query: {}}">Admin</router-link></div><p class="eyebrow">{{admin ? 'ADMIN ACCOUNT' : 'CUSTOMER ACCOUNT'}}</p><h2>{{register ? 'Make yourself at home.' : 'Welcome back.'}}</h2><p class="auth-intro">{{register ? 'Create your account to get started.' : 'Sign in to pick up where you left off.'}}</p>
      <form @submit.prevent="submit"><fieldset :disabled="busy"><label v-if="register">Full name<input v-model="name" autocomplete="name" required maxlength="100" placeholder="Your full name"/></label><label>Email address<input v-model="email" type="email" autocomplete="email" required maxlength="254" placeholder="you@example.com"/></label><label>Password<span class="password-field"><input v-model="password" :type="visible ? 'text' : 'password'" :autocomplete="register ? 'new-password' : 'current-password'" :minlength="register ? 8 : undefined" required maxlength="72" :placeholder="register ? 'At least 8 characters' : 'Enter your password'"/><button type="button" :aria-label="visible ? 'Hide password' : 'Show password'" :aria-pressed="visible" @click="visible = !visible"><EyeOff v-if="visible" :size="18"/><Eye v-else :size="18"/></button></span></label><label v-if="register">Confirm password<input v-model="confirm" :type="visible ? 'text' : 'password'" autocomplete="new-password" required placeholder="Repeat your password"/></label><label v-if="register && admin">Admin invitation code<input v-model="invitation" required autocomplete="off" placeholder="Enter your invitation code"/><small>Use the invitation provided by your administrator.</small></label><p v-if="error" class="notice error" role="alert">{{error}}</p><p v-if="success" class="auth-success" role="status">{{success}}</p><button class="primary auth-submit" type="submit">{{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}}<ArrowRight :size="18"/></button></fieldset></form>
      <div v-if="!register" class="auth-test" data-test-accounts><p>OR SKIP A STEP — TEST ACCOUNTS</p><div><button type="button" class="secondary" :disabled="busy" @click="useTest('customer')">Use customer account</button><button type="button" class="secondary" :disabled="busy" @click="useTest('admin')">Use admin account</button></div><small>One click fills the form and signs you in.</small></div>
      <p class="auth-switch">{{register ? 'Already have an account?' : 'New to BuyMore?'}} <router-link :to="{path: base + (register ? '/login' : '/register'), query: route.query}">{{register ? 'Sign in' : 'Create an account'}}</router-link></p><router-link class="auth-back" to="/">← Back to home</router-link>
    </div>
  </section>
</template>
