<script setup>
import { ref } from 'vue'
import {
  ArrowUpRight, ArrowRight, ShoppingBag, ShieldCheck, Sparkles,
  Headphones, MoveUpRight, Truck, RotateCcw, Star,
} from 'lucide-vue-next'
import ProductArt from '../components/ProductArt.vue'
import HeroScene from '../components/HeroScene.vue'
import MarqueeRibbon from '../components/MarqueeRibbon.vue'
import FeaturedProducts from '../components/FeaturedProducts.vue'

const collections = [
  { title: 'Sound good. Feel good.', label: 'AUDIO', category: 'audio', product: 'Wireless Earbuds', tone: 'peach' },
  { title: 'Make work feel lighter.', label: 'DESK ESSENTIALS', category: 'office', product: 'Desk Lamp LED', tone: 'mint' },
  { title: 'Small upgrades. Big difference.', label: 'PERIPHERALS', category: 'peripherals', product: 'Wireless Keyboard', tone: 'lilac' },
]

const values = [
  { icon: Sparkles, title: 'Curated, not cluttered', copy: 'Everyday pieces actually worth keeping.' },
  { icon: Truck, title: 'Quick dispatch', copy: 'Packed with care and on its way fast.' },
  { icon: RotateCcw, title: 'Easy returns', copy: 'Changed your mind? No fuss at all.' },
  { icon: Headphones, title: 'Human support', copy: 'Real people, quick and kind answers.' },
]

// Pointer parallax for the hero stage: chips and the product art drift at
// different depths. Touch and reduced-motion visitors get the static scene.
const stage = ref(null)
function onMove(event) {
  if (event.pointerType === 'touch' || !stage.value) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const box = stage.value.getBoundingClientRect()
  const px = ((event.clientX - box.left) / box.width - 0.5) * 2
  const py = ((event.clientY - box.top) / box.height - 0.5) * 2
  stage.value.style.setProperty('--px', px.toFixed(3))
  stage.value.style.setProperty('--py', py.toFixed(3))
}
function onLeave() {
  stage.value?.style.setProperty('--px', '0')
  stage.value?.style.setProperty('--py', '0')
}
</script>

<template>
  <div class="lp">
    <!-- 1 · Hero: floating collage, no hard panel behind the product -->
    <section class="lp-hero">
      <div class="lp-copy" v-reveal>
        <span class="lp-kicker"><i></i> THE EVERYDAY COLLECTION</span>
        <h1>A little more<br /><em>you.</em> A lot<br />to love<span class="lp-dot">.</span></h1>
        <p>
          Good design belongs in your everyday.<br />
          Find thoughtful essentials for the way you work,<br class="lp-break" /> unwind, and make a space your own.
        </p>
        <div class="lp-actions">
          <router-link class="primary" to="/shop">Find your next favourite <ArrowUpRight :size="19" /></router-link>
          <router-link class="lp-signup" to="/register">Join BuyMore <ArrowRight :size="16" /></router-link>
        </div>
        <div class="lp-note">
          <span><Sparkles :size="18" /></span>
          <div>Less ordinary. More everyday.<small>Curated with your kind of life in mind.</small></div>
        </div>
      </div>

      <div
        class="lp-stage"
        ref="stage"
        @pointermove="onMove"
        @pointerleave="onLeave"
        @pointercancel="onLeave"
      >
        <div class="lp-rings" aria-hidden="true"><i class="ring-1"></i><i class="ring-2"></i><i class="ring-3"></i></div>
        <span class="lp-stage-label">THE SOUND OF YOUR EVERYDAY</span>
        <span class="lp-sticker">GOOD<br />THINGS<br /><Sparkles :size="18" /></span>

        <HeroScene class="lp-scene" title="Noise Cancelling Headphones" category="audio" />

        <span class="lp-chip chip-rating"><Star :size="14" fill="currentColor" /> 4.9 from everyday shoppers</span>
        <span class="lp-chip chip-shipping"><Truck :size="14" /> Free shipping over $50</span>
        <span class="lp-chip chip-picks"><ShoppingBag :size="14" /> 24 fresh picks in stock</span>

        <div class="lp-caption">
          <div>
            <small>MEET YOUR NEW DAILY COMPANION</small>
            <h2>Turn up your world.</h2>
          </div>
          <router-link class="lp-round" to="/shop?category=audio" aria-label="Explore audio">
            <span class="lp-sparks" aria-hidden="true"><i v-for="n in 4" :key="n"></i></span>
            <ArrowUpRight :size="26" />
          </router-link>
        </div>
      </div>
    </section>

    <!-- 2 · Scrolling tagline ribbon -->
    <MarqueeRibbon />

    <!-- 3 · Live featured products -->
    <FeaturedProducts />

    <!-- 4 · Values strip -->
    <section class="lp-values" v-reveal aria-labelledby="lp-values-title">
      <header class="lp-values-head">
        <p class="eyebrow">WHY BUYMORE</p>
        <h2 id="lp-values-title">The little things, done properly.</h2>
      </header>
      <div class="lp-values-grid">
        <article v-for="(value, index) in values" :key="value.title" class="lp-value" :style="{ '--i': index }">
          <span class="lp-sparks" aria-hidden="true"><i v-for="n in 4" :key="n"></i></span>
          <span class="lp-value-icon"><component :is="value.icon" :size="20" /></span>
          <h3>{{ value.title }}</h3>
          <p>{{ value.copy }}</p>
        </article>
      </div>
    </section>

    <!-- 5 · Collections -->
    <section class="lp-collections" v-reveal aria-labelledby="lp-collections-title">
      <header class="lp-section-head">
        <div>
          <p class="eyebrow">FIND YOUR EVERYDAY</p>
          <h2 id="lp-collections-title">Different moments. Better essentials.</h2>
        </div>
        <router-link class="lp-head-link" to="/shop">Shop the collection <ArrowUpRight :size="17" /></router-link>
      </header>
      <div class="lp-collection-grid">
        <router-link
          v-for="(item, index) in collections"
          :key="item.category"
          :to="'/shop?category=' + item.category"
          class="lp-collection-card"
          :class="item.tone"
          :style="{ '--i': index }"
        >
          <span class="lp-sparks" aria-hidden="true"><i v-for="n in 4" :key="n"></i></span>
          <span class="lp-card-top">{{ item.label }} <MoveUpRight :size="18" /></span>
          <ProductArt :title="item.product" :category="item.category" />
          <h3>{{ item.title }}</h3>
          <span class="lp-card-link">Explore collection <ArrowRight :size="15" /></span>
        </router-link>
      </div>
    </section>

    <!-- 6 · Account panel -->
    <section class="lp-accounts" aria-label="Your BuyMore account">
      <div class="lp-customer" v-reveal>
        <span class="lp-customer-icon"><ShoppingBag :size="26" /></span>
        <div class="lp-customer-copy">
          <p class="eyebrow">YOUR SPACE AT BUYMORE</p>
          <h2>Good to have you here.</h2>
          <p>A new favourite is just around the corner.</p>
          <div class="lp-badges">
            <span class="lp-badge badge-a"><Star :size="13" fill="currentColor" /> Loved daily</span>
            <span class="lp-badge badge-b">No membership fee</span>
          </div>
        </div>
        <div class="lp-account-actions">
          <router-link class="primary" to="/register">Create an account <ArrowUpRight :size="17" /></router-link>
          <router-link to="/login">Already a member? <strong>Log in</strong></router-link>
        </div>
      </div>
      <div class="lp-team" v-reveal>
        <ShieldCheck :size="19" />
        <span>Part of the BuyMore team?</span>
        <router-link to="/admin/login">Admin login <ArrowUpRight :size="14" /></router-link>
        <router-link to="/admin/register">Admin registration <ArrowUpRight :size="14" /></router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.lp { margin-top: 22px; }

/* ---------- shared section furniture ---------- */
.lp-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}
.lp-section-head h2 { margin: 0; font-size: clamp(26px, 2.7vw, 36px); }
.lp-section-head .eyebrow { margin-bottom: 8px; font-size: 9px; }
.lp-head-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  padding: 11px 16px;
  border-radius: 30px;
  background: #ffffffcc;
  border: 1px solid #fff;
  flex-shrink: 0;
  transition: transform .25s cubic-bezier(.2,.8,.2,1), color .2s;
}
.lp-head-link:hover { color: var(--blue); transform: translateY(-3px); }

/* ---------- 1 · hero ---------- */
.lp-hero {
  display: grid;
  grid-template-columns: 1.04fr 1fr;
  gap: 26px;
  min-height: 605px;
}
.lp-copy {
  padding: 46px clamp(26px, 4vw, 58px) 40px;
  background: #f6f8ec;
  border: 1px solid #ffffffd9;
  border-radius: 28px;
  box-shadow: 0 20px 50px #355c3312;
  animation: card-in .6s cubic-bezier(.34,1.3,.4,1) both;
}
.lp-kicker {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 28px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2px;
}
.lp-kicker i { width: 7px; height: 7px; border-radius: 50%; background: var(--blue); animation: dot-pulse 2.6s ease-in-out infinite; }
.lp-copy h1 { font-size: clamp(52px, 6vw, 92px); line-height: .99; letter-spacing: -.075em; font-weight: 800; }
.lp-copy h1 em { color: var(--blue); font-style: italic; }
.lp-dot { color: var(--blue); }
.lp-copy > p { font-size: 14px; color: #68705e; line-height: 1.85; margin: 25px 0; }
.lp-actions { display: flex; gap: 22px; align-items: center; flex-wrap: wrap; }
.lp-signup { display: flex; gap: 8px; align-items: center; font-size: 12px; font-weight: 600; }
.lp-signup svg { transition: transform .25s cubic-bezier(.2,.8,.2,1); }
.lp-signup:hover svg { transform: translateX(4px); }
.lp-note { display: flex; gap: 12px; align-items: center; font-size: 11px; margin-top: 34px; }
.lp-note > span {
  display: grid;
  place-items: center;
  padding: 10px;
  border-radius: 50%;
  background: #e5ebd9;
  animation: art-float-b 6.5s ease-in-out infinite;
}
.lp-note small { display: block; font-size: 10px; color: #6e7966; margin-top: 5px; }

/* the open stage: a glow instead of a solid rectangle */
.lp-stage {
  position: relative;
  isolation: isolate;
  min-width: 0;
  min-height: 560px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28px;
  overflow: hidden;
  --px: 0;
  --py: 0;
  background:
    radial-gradient(58% 52% at 52% 44%, #c9d8fb 0%, #d4e0fbee 42%, #d4e0fb00 74%),
    radial-gradient(38% 34% at 78% 82%, #e7f19d66 0%, transparent 70%);
}
.lp-rings { position: absolute; inset: 0; z-index: -1; }
.lp-rings i {
  position: absolute;
  left: 50%;
  top: 46%;
  border-radius: 50%;
  border: 1px solid #ffffff9c;
  translate: -50% -50%;
}
.lp-rings .ring-1 { width: min(78%, 460px); aspect-ratio: 1; animation: lp-ring-spin 46s linear infinite; }
.lp-rings .ring-2 { width: min(58%, 340px); aspect-ratio: 1; border-style: dashed; border-color: #ffffffc4; animation: lp-ring-spin 34s linear infinite reverse; }
.lp-rings .ring-3 { width: min(96%, 570px); aspect-ratio: 1; border-color: #ffffff70; }
@keyframes lp-ring-spin { to { rotate: 360deg; } }

.lp-stage-label {
  position: absolute;
  top: 32px;
  left: 30px;
  font-size: 9px;
  letter-spacing: 2px;
  color: #3a4c72;
}
.lp-scene {
  width: 100%;
  max-width: 560px;
  height: 420px;
  z-index: 1;
  transform: translate3d(calc(var(--px) * 10px), calc(var(--py) * 8px), 0);
  transition: transform .55s cubic-bezier(.2,.8,.2,1);
  animation: art-float-a 7s ease-in-out infinite;
}
.lp-sticker {
  position: absolute;
  right: 26px;
  top: 74px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: #e7f19d;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.1;
  text-align: center;
  box-shadow: 0 10px 24px #7c8a2a33;
  transform: translate3d(calc(var(--px) * 16px), calc(var(--py) * -14px), 0) rotate(13deg);
  transition: transform .55s cubic-bezier(.2,.8,.2,1);
}
.lp-chip {
  position: absolute;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 11px 15px;
  border-radius: 30px;
  background: #fffffff0;
  border: 1px solid #fff;
  box-shadow: 0 14px 30px #28395e22;
  font-size: 11px;
  font-weight: 600;
  color: #33405e;
  white-space: nowrap;
}
.lp-chip svg { color: var(--blue); }
.chip-rating { left: 5%; top: 25%; transform: translate3d(calc(var(--px) * -22px), calc(var(--py) * -16px), 0); transition: transform .55s cubic-bezier(.2,.8,.2,1); }
.chip-shipping { right: 4%; top: 54%; transform: translate3d(calc(var(--px) * 24px), calc(var(--py) * 12px), 0); transition: transform .55s cubic-bezier(.2,.8,.2,1); }
.chip-picks { right: 15%; bottom: 17%; transform: translate3d(calc(var(--px) * -14px), calc(var(--py) * 20px), 0); transition: transform .55s cubic-bezier(.2,.8,.2,1); }
.lp-caption {
  position: absolute;
  left: 26px;
  right: 26px;
  bottom: 24px;
  z-index: 3;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 17px 19px;
  border-radius: 20px;
  background: #fffffff2;
  border: 1px solid #fff;
  box-shadow: 0 18px 38px #28395e22;
  animation: card-in .7s .25s cubic-bezier(.34,1.3,.4,1) both;
}
.lp-caption small { font-size: 8.5px; letter-spacing: 1.5px; color: #79849a; }
.lp-caption h2 { margin: 7px 0 0; font-size: clamp(21px, 2vw, 27px); }
.lp-round {
  position: relative;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--blue);
  color: #fff;
  box-shadow: 0 10px 22px #245cff45;
  transition: transform .3s cubic-bezier(.34,1.56,.64,1);
}
.lp-round:hover { transform: translate(3px, -3px) rotate(8deg); }

/* ---------- 4 · values ---------- */
.lp-values { margin-top: 30px; }
.lp-values-head { margin-bottom: 20px; }
.lp-values-head .eyebrow { font-size: 9px; margin-bottom: 8px; }
.lp-values-head h2 { margin: 0; font-size: clamp(26px, 2.7vw, 36px); }
.lp-values-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.lp-value {
  padding: 24px 22px 25px;
  border-radius: 24px;
  background: #ffffffcc;
  border: 1px solid #fff;
  box-shadow: 0 14px 34px #355c3310;
  transition: transform .35s cubic-bezier(.2,.8,.2,1), box-shadow .35s;
  animation: card-in .55s calc(var(--i, 0) * 80ms) cubic-bezier(.34,1.3,.4,1) both;
}
.lp-value:hover { transform: translateY(-7px); box-shadow: 0 26px 46px #355c331a; }
.lp-value-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 15px;
  background: #e7f19d;
  color: #4c5a1d;
  margin-bottom: 19px;
  transition: transform .35s cubic-bezier(.34,1.56,.64,1);
}
.lp-value:nth-child(2) .lp-value-icon { background: #cddafc; color: #2c4590; }
.lp-value:nth-child(3) .lp-value-icon { background: #ffe0cd; color: #97552b; }
.lp-value:nth-child(4) .lp-value-icon { background: #d9eedd; color: #37654a; }
.lp-value:hover .lp-value-icon { transform: rotate(-9deg) scale(1.07); }
.lp-value h3 { margin: 0 0 9px; font-size: 16px; }
.lp-value p { margin: 0; font-size: 12px; line-height: 1.7; color: #7c8578; }

/* ---------- 5 · collections ---------- */
.lp-collections { margin-top: 34px; }
.lp-collection-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.lp-collection-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 25px;
  border-radius: 26px;
  border: 1px solid #ffffffd9;
  box-shadow: 0 16px 38px #355c3312;
  overflow: hidden;
  transition: transform .38s cubic-bezier(.2,.8,.2,1), box-shadow .38s;
  animation: card-in .6s calc(var(--i, 0) * 100ms) cubic-bezier(.34,1.3,.4,1) both;
}
.lp-collection-card.peach { background: #f7e6db; }
.lp-collection-card.mint { background: #dff0e4; }
.lp-collection-card.lilac { background: #e6e5f7; }
.lp-collection-card:hover { transform: translateY(-9px); box-shadow: 0 30px 54px #355c331e; }
.lp-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.8px;
  color: #5c6555;
}
.lp-card-top svg { transition: transform .3s cubic-bezier(.34,1.56,.64,1); }
.lp-collection-card:hover .lp-card-top svg { transform: translate(3px, -3px); }
.lp-collection-card > .product-art {
  display: block;
  width: 100%;
  height: 190px;
  margin: 6px 0 4px;
  transition: transform .4s cubic-bezier(.2,.8,.2,1);
  animation: art-float-a 8s ease-in-out infinite;
}
.lp-collection-card:hover > .product-art { transform: translateY(-6px) rotate(-4deg); }
.lp-collection-card h3 { margin: auto 0 14px; font-size: clamp(19px, 1.7vw, 23px); line-height: 1.2; }
.lp-card-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  font-weight: 600;
  color: #3d4b3f;
}
.lp-collection-card:hover .lp-card-link { color: var(--blue); }

/* ---------- 6 · accounts ---------- */
.lp-accounts { display: grid; grid-template-columns: 1.55fr 1fr; gap: 18px; margin-top: 34px; }
.lp-customer {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  padding: 34px clamp(24px, 3vw, 40px);
  border-radius: 28px;
  background: #f6f8ec;
  border: 1px solid #ffffffd9;
  box-shadow: 0 16px 38px #355c3312;
  overflow: hidden;
}
.lp-customer-icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 19px;
  background: var(--blue);
  color: #fff;
  box-shadow: 0 12px 26px #245cff40;
  animation: art-float-b 7s ease-in-out infinite;
}
.lp-customer-copy { flex: 1; min-width: 240px; }
.lp-customer-copy .eyebrow { font-size: 9px; margin-bottom: 8px; }
.lp-customer-copy h2 { margin: 0 0 8px; font-size: clamp(24px, 2.4vw, 32px); }
.lp-customer-copy p:last-child { margin: 0; font-size: 13px; color: #78826f; }
.lp-account-actions { display: flex; flex-direction: column; align-items: flex-start; gap: 13px; font-size: 12px; }
.lp-badges { display: flex; flex-wrap: wrap; gap: 11px; margin-top: 15px; }
.lp-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 13px;
  border-radius: 20px;
  background: #fff;
  font-size: 10px;
  font-weight: 700;
  box-shadow: 0 12px 24px #355c331f;
}
.badge-a { color: #a08223; transform: rotate(-3deg); }
.badge-b { color: #3d6b4c; transform: rotate(3deg); }
.lp-team {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 13px;
  padding: 24px 28px;
  border-radius: 28px;
  background: #e7f19d;
  border: 1px solid #f2f7cd;
  box-shadow: 0 16px 38px #7c8a2a24;
  font-size: 12.5px;
}
.lp-team > svg { color: #5f6c1e; animation: art-float-a 6.5s ease-in-out infinite; }
.lp-team > span { font-weight: 700; }
.lp-team a { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; }
.lp-team a:hover { color: var(--blue); }

/* ---------- glitter: shimmer sweeps, shining titles, hover bursts ---------- */
.lp-copy, .lp-value, .lp-customer { position: relative; overflow: hidden; }
.lp-copy::after, .lp-value::after, .lp-collection-card::after, .lp-customer::after {
  content: '';
  position: absolute;
  top: -60%;
  left: -75%;
  width: 46%;
  height: 220%;
  z-index: 0;
  pointer-events: none;
  opacity: 0;
  transform: translateX(0) rotate(9deg);
  background: linear-gradient(100deg, #ffffff00, #ffffffd9 45%, #ffffff00 92%);
  animation: lp-sheen 7.5s ease-in-out infinite;
}
.lp-copy::after { animation-delay: .6s; }
.lp-value:nth-child(2)::after { animation-delay: .85s; }
.lp-value:nth-child(3)::after { animation-delay: 1.7s; }
.lp-value:nth-child(4)::after { animation-delay: 2.55s; }
.lp-collection-card:nth-child(2)::after { animation-delay: 1.15s; }
.lp-collection-card:nth-child(3)::after { animation-delay: 2.3s; }
@keyframes lp-sheen {
  0%, 58% { opacity: 0; transform: translateX(0) rotate(9deg); }
  66% { opacity: .9; }
  92%, 100% { opacity: 0; transform: translateX(440%) rotate(9deg); }
}
/* content paints above the travelling sweep */
.lp-copy > *, .lp-value > *, .lp-customer > *, .lp-collection-card > * { position: relative; z-index: 1; }

.lp-sparks { position: absolute; inset: 0; z-index: 2; pointer-events: none; }
.lp-sparks i {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 7px;
  height: 7px;
  margin: -3.5px;
  border-radius: 2px;
  background: #fff;
  box-shadow: 0 0 9px 3px #ffe9a8cc, 0 0 20px 7px #245cff38;
  opacity: 0;
}
.lp-sparks i:nth-child(1) { --dx: -46px; --dy: -40px; }
.lp-sparks i:nth-child(2) { --dx: 48px; --dy: -36px; animation-delay: .05s; }
.lp-sparks i:nth-child(3) { --dx: -40px; --dy: 46px; animation-delay: .1s; }
.lp-sparks i:nth-child(4) { --dx: 52px; --dy: 42px; animation-delay: .15s; }
.lp-value:hover .lp-sparks i,
.lp-collection-card:hover .lp-sparks i,
.lp-round:hover .lp-sparks i { animation: lp-burst .72s cubic-bezier(.2,.8,.2,1) forwards; }
@keyframes lp-burst {
  0% { opacity: 0; transform: rotate(45deg) scale(.2); }
  30% { opacity: 1; transform: rotate(45deg) translate(calc(var(--dx) * .35), calc(var(--dy) * .35)) scale(1.15); }
  100% { opacity: 0; transform: rotate(45deg) translate(var(--dx), var(--dy)) scale(.35); }
}

/* a moving shine across the display type */
@supports ((-webkit-background-clip: text) or (background-clip: text)) {
  .lp-copy h1, .lp-values-head h2, .lp-section-head h2 {
    background-image: linear-gradient(100deg, #1c2420 38%, #245cff 46%, #9db6ff 51%, #1c2420 60%);
    background-size: 260% 100%;
    background-repeat: no-repeat;
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    animation: lp-shine 7s linear infinite;
  }
}
/* the gradient must always cover the whole headline: keep position inside 0–100% */
@keyframes lp-shine { from { background-position: 100% 0; } to { background-position: 0% 0; } }

/* ---------- responsive ---------- */
@media (min-width: 1500px) {
  .lp-copy { padding-top: 58px; padding-bottom: 48px; }
  .lp-hero { min-height: 670px; }
  .lp-scene { height: 490px; }
}
@media (max-width: 1100px) {
  .lp-values-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .lp-chip { font-size: 10px; padding: 10px 13px; }
}
@media (max-width: 1000px) {
  .lp-copy { padding: 34px 26px; }
  .lp-copy h1 { font-size: clamp(46px, 6.4vw, 64px); }
  .lp-actions { gap: 15px; }
  .lp-stage { min-height: 470px; }
  .lp-scene { height: 330px; }
  .lp-caption { left: 20px; right: 20px; bottom: 18px; }
  .lp-collection-grid { gap: 14px; }
  .lp-collection-card { padding: 21px; }
  .lp-collection-card > .product-art { height: 155px; }
  .lp-collection-card h3 { font-size: 19px; }
  .lp-accounts { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .lp-hero { grid-template-columns: 1fr; gap: 14px; min-height: 0; }
  .lp-stage { min-height: 430px; }
  .lp-scene { height: 300px; }
  .lp-values-grid { gap: 12px; }
  .lp-collection-grid { grid-template-columns: 1fr; }
  .lp-collection-card > .product-art { height: 200px; }
}
@media (max-width: 700px) {
  .lp { margin-top: 14px; }
  .lp-copy { padding: 34px 24px; }
  .lp-copy h1 { font-size: clamp(54px, 12vw, 78px); }
  .lp-break { display: none; }
  .lp-stage-label { top: 24px; left: 24px; font-size: 8px; }
  .lp-sticker { top: 58px; right: 20px; width: 76px; height: 76px; font-size: 11px; }
  .chip-rating { left: 4%; top: 22%; }
  .chip-shipping { right: 4%; top: auto; bottom: 30%; }
  .chip-picks { display: none; }
  .lp-caption { padding: 14px 16px; }
  .lp-round { width: 44px; height: 44px; }
  .lp-customer { padding: 26px 22px; gap: 17px; }
  .lp-customer-icon { width: 48px; height: 48px; }
  .lp-account-actions { width: 100%; align-items: flex-start; }
  .badge-b { display: none; }
  .lp-team { justify-content: flex-start; padding: 21px 24px; }
}
@media (max-width: 480px) {
  .lp-values-grid { grid-template-columns: 1fr; }
  .lp-section-head { flex-direction: column; align-items: flex-start; gap: 14px; }
  .lp-chip { font-size: 9.5px; padding: 9px 12px; }
}
@media (prefers-reduced-motion: reduce) {
  .lp-copy, .lp-caption, .lp-value, .lp-collection-card { animation: none; }
  .lp-scene, .lp-sticker, .lp-chip, .lp-note > span, .lp-customer-icon, .lp-badge, .lp-team > svg { animation: none; }
  .lp-rings i { animation: none; }
  .lp-value:hover, .lp-collection-card:hover, .lp-head-link:hover { transform: none; }
}
</style>
