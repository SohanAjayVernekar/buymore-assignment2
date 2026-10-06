<script setup>
import { ArrowUpRight, ShoppingBag } from 'lucide-vue-next'
const year = new Date().getFullYear()
</script>

<template>
  <footer class="site-footer">
    <div class="footer-main">
      <div class="footer-story">
        <router-link class="footer-brand" to="/" aria-label="BuyMore home"><span><ShoppingBag :size="21" /></span>BuyMore<span class="brand-dot">.</span></router-link>
        <h2>Little things.<br>A better everyday.</h2>
        <p>Thoughtful essentials for your desk, your day, and everything in between.</p>
      </div>
      <nav class="footer-links" aria-label="Explore BuyMore">
        <h3>Explore</h3>
        <router-link to="/">Home</router-link>
        <router-link to="/shop">The collection <ArrowUpRight :size="14" /></router-link>
        <router-link to="/checkout">Your cart</router-link>
      </nav>
      <div class="footer-help">
        <span class="footer-kicker">A LITTLE HELP?</span>
        <h3>Let’s talk.</h3>
        <p>A question or an idea?<br>We’d love to hear from you.</p>
        <router-link to="/contact">Contact us <span><ArrowUpRight :size="19" /></span></router-link>
      </div>
    </div>
    <div class="footer-bottom">
      <span>© {{ year }} BuyMore</span>
      <span class="footer-signoff">Curated for your everyday.</span>
      <nav class="footer-legal" aria-label="Legal information"><router-link to="/privacy-policy">Privacy policy</router-link><router-link to="/license">License</router-link></nav>
    </div>
  </footer>
</template>

<style scoped>
.site-footer{display:block;padding:0;margin-top:20px;border:1px solid #ffffffb8;border-radius:28px;background:#f7faefee;color:#263c32;overflow:hidden;box-shadow:0 12px 35px #355c3308;font-size:13px}
.footer-main{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(120px,.7fr) minmax(210px,1fr);gap:40px;padding:38px}
.footer-brand{display:inline-flex;align-items:center;gap:9px;font-size:22px;font-weight:750;letter-spacing:-1px;width:fit-content}
.footer-brand>span:first-child{display:grid;place-items:center;width:36px;height:36px;background:#dce8d3;border-radius:12px}
.footer-brand .brand-dot{color:var(--blue);margin-left:-8px}
.footer-story h2{font-size:clamp(25px,2.6vw,35px);line-height:1.2;margin:23px 0 12px;letter-spacing:-1.3px}
.footer-story p,.footer-help p{font-size:13px;line-height:1.8;color:#607264;margin:0;max-width:290px}
.site-footer .footer-links{display:flex;flex-direction:column;align-items:flex-start;gap:14px;padding-top:10px}
.footer-links h3{font-family:inherit;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;margin:0 0 10px;color:#637465}
.footer-links a{display:flex;align-items:center;gap:7px;padding:3px 0;font-size:13px;transition:color .2s,transform .2s}
.footer-links a:hover{color:var(--blue);transform:translateX(3px)}
.footer-help{align-self:start;background:#e7eccf;border-radius:20px;padding:25px}
.footer-kicker{font-size:9px;font-weight:700;letter-spacing:1.8px;color:#52664a}
.footer-help h3{font-size:28px;letter-spacing:-1px;margin:12px 0 9px}
.footer-help>a{display:flex;align-items:center;justify-content:space-between;gap:16px;font-size:13px;font-weight:600;margin-top:21px}
.footer-help>a span{display:grid;place-items:center;width:35px;height:35px;border-radius:50%;background:#fff;transition:transform .2s,background .2s}
.footer-help>a:hover span{transform:translate(2px,-2px);background:#f5f8ec}
.footer-bottom{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:16px;border-top:1px solid #dce4d5;padding:19px 38px;font-size:11px;color:#627360}
.site-footer .footer-legal{display:flex;flex-wrap:wrap;align-items:center;gap:22px}
.footer-legal a{padding:4px 0;transition:color .2s}
.footer-legal a:hover{color:var(--blue)}
@media(max-width:1100px){.footer-main{gap:24px;padding:28px;grid-template-columns:minmax(0,1.2fr) minmax(110px,.6fr) minmax(190px,1fr)}.footer-help{padding:21px}.footer-bottom{padding:17px 28px}.footer-signoff{display:none}}
@media(max-width:800px){.footer-main{grid-template-columns:minmax(0,1fr) minmax(110px,.65fr)}.footer-help{grid-column:1/-1;display:grid;grid-template-columns:1fr 1fr;gap:0 20px}.footer-help .footer-kicker,.footer-help h3{grid-column:1}.footer-help p{grid-column:2;grid-row:1/3;align-self:center}.footer-help>a{grid-column:1/-1;margin-top:10px}}
@media(max-width:480px){.site-footer{border-radius:23px}.footer-main{padding:24px 21px;gap:28px 18px}.footer-story h2{font-size:26px}.footer-story p{font-size:12px}.footer-links a{font-size:12px}.footer-help{display:block}.footer-help p{font-size:12px}.footer-bottom{padding:17px 21px;align-items:flex-start;gap:10px}.site-footer .footer-legal{gap:15px}.footer-brand{font-size:20px}}
@media(prefers-reduced-motion:reduce){.site-footer a,.footer-help>a span{transition:none}.footer-links a:hover,.footer-help>a:hover span{transform:none}}
</style>
