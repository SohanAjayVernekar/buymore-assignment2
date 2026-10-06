<script setup>
import { ShieldCheck } from 'lucide-vue-next'
</script>
<template>
  <section class="policy-page">
    <header class="policy-head">
      <span class="welcome-icon"><ShieldCheck :size="27"/></span>
      <p class="eyebrow">HOW WE HANDLE YOUR INFORMATION</p>
      <h1>Privacy Policy<span class="blue-dot">.</span></h1>
      <p class="policy-intro">This page describes exactly what data this BuyMore application collects, why it is collected, and what is stored where. It covers this implementation only — it is not legal advice.</p>
    </header>
    <article class="policy-card">
      <section>
        <h2>1. Account information</h2>
        <p>When you create an account we collect your name, email address, and a password. Your password is never stored in plain text: it is hashed with BCrypt on the server before saving, and the hash is never sent to the browser. Your name and email are stored in our PostgreSQL database so we can identify your account and your orders.</p>
      </section>
      <section>
        <h2>2. Session cookies</h2>
        <p>After you sign in, the server creates a session and stores a session identifier in your browser as an <strong>HttpOnly cookie</strong>. This cookie:</p>
        <ul>
          <li>Cannot be read by JavaScript in the browser,</li>
          <li>Is sent with your requests so the server knows who you are,</li>
          <li>Expires when you log out or the server-side session expires,</li>
          <li>Is rotated on login to prevent session fixation.</li>
        </ul>
        <p>We also use a session-based CSRF token to protect forms and requests from cross-site request forgery. This token is stored in the same server session, not in persistent browser storage.</p>
      </section>
      <section>
        <h2>3. Shopping cart and favourites</h2>
        <p>Saved favourites are linked to your account and stored on the server, so they are available when you sign in on another device. You can remove them from your favourites page. Your account dashboard also lets you view your orders and update your display name.</p>
        <p>Your cart contents (product identifier, title, price, category, and quantity) are stored in your browser's local storage on this device so the cart survives page reloads. The cart is cleared when you log out. Cart contents are only sent to the server when you place an order.</p>
      </section>
      <section>
        <h2>4. Orders</h2>
        <p>When you place an order we store the order lines, quantities, prices, total, status, and a link to your account in our PostgreSQL database. Order history is visible to you and to administrators of this store. Order data may be copied into an Elasticsearch index used for the admin order-search feature.</p>
      </section>
      <section>
        <h2>5. Contact submissions</h2>
        <p>Administrators can read submitted messages in their inbox and mark them resolved or reopen them. The resolved status is stored with the message.</p>
        <p>When you submit the Contact Us form we store your name, email address, subject, and message durably in our PostgreSQL database so your message is not lost. <strong>No email is sent</strong>: this deployment has no configured email service, so submissions are saved for the store owner to read. Please do not include sensitive information such as payment card numbers in your message.</p>
      </section>
      <section>
        <h2>6. What we do not collect</h2>
        <p>This application does not use advertising trackers, analytics pixels, or third-party profiling cookies. It does not process payments or shipping details. Passwords are never stored or logged in readable form, and no credentials are stored in your browser.</p>
      </section>
      <section>
        <h2>7. Retention and owner information</h2>
        <p><strong>Still needed from the store owner:</strong> this implementation does not configure automatic deletion schedules, and no specific retention periods, business address, contact email, or legal/jurisdiction details have been supplied for this deployment. The store owner should add those details before any public release.</p>
      </section>
      <section>
        <h2>8. Changes to this policy</h2>
        <p>This page reflects the data this application actually collects today. If the application changes, this page should be updated at the same time.</p>
      </section>
      <div class="policy-note"><strong>Note:</strong> this Privacy Policy covers your personal data. The software itself is distributed under a separate <router-link to="/license">MIT License</router-link>.</div>
    </article>
  </section>
</template>
<style scoped>
.policy-page{max-width:820px;margin:34px auto 10px}.policy-head{text-align:center;display:flex;flex-direction:column;align-items:center;gap:6px;margin-bottom:26px}.policy-head .welcome-icon{display:inline-flex;background:#eaf0e2;border-radius:15px;padding:15px;color:#4c6444}.policy-head h1{font-size:clamp(34px,4vw,52px)}.policy-intro{color:#687163;font-size:14px;line-height:1.8;max-width:620px;margin-top:12px}.policy-card{background:#fff;border:1px solid var(--line);border-radius:26px;padding:clamp(26px,4vw,48px);box-shadow:0 14px 45px #355c3308}.policy-card section{margin-bottom:30px}.policy-card h2{font-size:19px;margin-bottom:12px}.policy-card p,.policy-card li{font-size:13.5px;line-height:1.85;color:#5d6657}.policy-card p{margin-bottom:12px}.policy-card ul{margin:0 0 14px;padding-left:20px}.policy-card li{margin-bottom:7px}.policy-card strong{color:#2c3a26}.policy-card a{color:var(--blue);font-weight:600}.policy-note{border-top:1px solid var(--line);padding-top:22px;font-size:12.5px;line-height:1.7;color:#707967}
@media(max-width:650px){.policy-page{margin-top:22px}.policy-card h2{font-size:17px}.policy-card p,.policy-card li{font-size:13px}}
</style>
