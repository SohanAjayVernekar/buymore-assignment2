<script setup>
import { ref, watch } from 'vue'
import { ArrowRight, Check, Mail } from 'lucide-vue-next'
import { post } from '../api'

const name = ref(''), email = ref(''), subject = ref(''), message = ref(''), busy = ref(false), error = ref(''), success = ref('')

// Dismiss a stale error while the visitor edits; the confirmation banner stays
// visible until they submit again (clearing the fields after a save must not
// erase the "saved" message the watcher would otherwise wipe).
watch([name, email, subject, message], () => { error.value = '' })

async function submit() {
  error.value = ''; success.value = ''
  if (!name.value.trim()) { error.value = 'Please enter your name.'; return }
  if (!email.value.trim()) { error.value = 'Please enter your email address.'; return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) { error.value = 'Please enter a valid email address.'; return }
  if (!subject.value.trim()) { error.value = 'Please enter a subject.'; return }
  if (!message.value.trim()) { error.value = 'Please enter a message.'; return }
  if (message.value.trim().length > 4000) { error.value = 'Your message is too long (4000 characters maximum).' ; return }
  busy.value = true
  try {
    await post('/contact', { name: name.value.trim(), email: email.value.trim(), subject: subject.value.trim(), message: message.value.trim() })
    name.value = ''; email.value = ''; subject.value = ''; message.value = ''
    success.value = 'Your message has been saved. We will get back to you.'
  } catch (e) { error.value = e.message } finally { busy.value = false }
}
</script>
<template>
  <section class="auth-layout contact-layout">
    <aside class="auth-story"><span class="welcome-icon"><Mail :size="27"/></span><p class="eyebrow">WE WOULD LOVE TO HEAR FROM YOU</p><h1>Say hello<span class="blue-dot">.</span></h1><p>Questions, feedback, or just a quick note — send it our way and we will read every message.</p><div class="auth-story-note"><span>BUYMORE</span><strong>A little more.<br>A little better.</strong></div></aside>
    <div class="auth-panel">
      <p class="eyebrow">CONTACT US</p><h2>Get in touch.</h2><p class="auth-intro">Fill in the form below and your message will be saved securely.</p>
      <form @submit.prevent="submit"><fieldset :disabled="busy">
        <div class="form-grid">
          <label class="input-label">Your name<input v-model="name" autocomplete="name" required maxlength="100" placeholder="Your full name"/></label>
          <label class="input-label">Email address<input v-model="email" type="email" autocomplete="email" required maxlength="254" placeholder="you@example.com"/></label>
          <label class="input-label wide">Subject<input v-model="subject" required maxlength="200" placeholder="What is this about?"/></label>
          <label class="input-label wide">Message<textarea v-model="message" required rows="6" maxlength="4000" placeholder="Write your message…"></textarea></label>
        </div>
        <p v-if="error" class="notice error" role="alert">{{error}}</p>
        <p v-if="success" class="auth-success" role="status">{{success}}</p>
        <button class="primary auth-submit" type="submit">{{busy ? 'Sending…' : 'Send message'}}<ArrowRight :size="17"/></button>
      </fieldset></form>
      <router-link class="auth-back" to="/">← Back to home</router-link>
    </div>
  </section>
</template>
<style scoped>
.contact-layout textarea{resize:vertical;min-height:120px;font-family:inherit;line-height:1.6}
.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 16px}
.form-grid .wide{grid-column:span 2}
@media(max-width:650px){.form-grid{grid-template-columns:1fr}.form-grid .wide{grid-column:span 1}}
</style>
