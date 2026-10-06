<script setup>
import { Scale } from 'lucide-vue-next'
</script>
<template>
  <section class="policy-page">
    <header class="policy-head">
      <span class="welcome-icon"><Scale :size="27"/></span>
      <p class="eyebrow">OPEN SOURCE LICENSE</p>
      <h1>License<span class="blue-dot">.</span></h1>
      <p class="policy-intro">The BuyMore software in this repository is released under the MIT License, reproduced below exactly as it appears in the project's <code>LICENSE</code> file.</p>
    </header>
    <article class="policy-card license-card">
      <h2>MIT License</h2>
      <p><strong>Copyright (c) 2026 Sohan Ajay Vernekar</strong></p>
      <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:</p>
      <p>The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.</p>
      <p class="disclaimer">THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.</p>
      <div class="policy-note"><strong>Different from the Privacy Policy:</strong> this license governs how the <em>software</em> may be used, copied, and distributed. It says nothing about your personal data — for that, see the <router-link to="/privacy-policy">Privacy Policy</router-link>.</div>
    </article>
  </section>
</template>
<style scoped>
.policy-page{max-width:820px;margin:34px auto 10px}.policy-head{text-align:center;display:flex;flex-direction:column;align-items:center;gap:6px;margin-bottom:26px}.policy-head .welcome-icon{display:inline-flex;background:#eaf0e2;border-radius:15px;padding:15px;color:#4c6444}.policy-head h1{font-size:clamp(34px,4vw,52px)}.policy-intro{color:#687163;font-size:14px;line-height:1.8;max-width:620px;margin-top:12px}.policy-intro code{background:#f2f5ee;border-radius:6px;padding:2px 7px;font-size:12.5px}.policy-card{background:#fff;border:1px solid var(--line);border-radius:26px;padding:clamp(26px,4vw,48px);box-shadow:0 14px 45px #355c3308}.policy-card h2{font-size:19px;margin-bottom:14px}.policy-card p{font-size:13.5px;line-height:1.85;color:#5d6657;margin-bottom:15px}.policy-card strong{color:#2c3a26}.policy-card a{color:var(--blue);font-weight:600}.disclaimer{font-size:12.5px !important;background:#fafbf7;border:1px solid var(--line);border-radius:14px;padding:16px 18px;color:#6d7566 !important}.policy-note{border-top:1px solid var(--line);padding-top:22px;font-size:12.5px;line-height:1.7;color:#707967}
@media(max-width:650px){.policy-page{margin-top:22px}.policy-card h2{font-size:17px}.policy-card p{font-size:13px}}
</style>
