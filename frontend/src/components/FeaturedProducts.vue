<script setup>
import { onMounted, ref } from 'vue'
import { ArrowUpRight } from 'lucide-vue-next'
import { api, money, deal, discount } from '../api'
import ProductArt from './ProductArt.vue'

// Live products for the landing page. The section disappears entirely if the
// catalog cannot be read, so a failed request never leaves a broken panel.
const products = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await api('/products?page_size=8')
    products.value = (data.items || []).filter(p => p.active !== false).sort((a, b) => Number(deal(b)) - Number(deal(a))).slice(0, 4)
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="lp-featured" v-reveal aria-labelledby="lp-featured-title">
    <header class="lp-section-head">
      <div>
        <p class="eyebrow">FRESH OFF THE SHELF</p>
        <h2 id="lp-featured-title">Good things, right now.</h2>
      </div>
      <router-link class="lp-head-link" to="/shop">See the whole collection <ArrowUpRight :size="17" /></router-link>
    </header>

    <div class="lp-product-grid" v-if="loading || products.length" :class="{ 'is-loading': loading }">
      <template v-if="loading">
        <span v-for="n in 4" :key="n" class="lp-product lp-skeleton" aria-hidden="true">
          <i class="lp-skeleton-art"></i><i class="lp-skeleton-line wide"></i><i class="lp-skeleton-line"></i>
        </span>
      </template>
      <template v-else>
        <router-link
          v-for="(product, index) in products"
          :key="product._id"
          class="lp-product"
          :style="{ '--i': index }"
          :to="'/shop?category=' + product.category"
        >
          <span class="lp-sparks" aria-hidden="true"><i v-for="n in 4" :key="n"></i></span>
          <span class="lp-product-art" :class="product.category">
            <ProductArt :title="product.title" :category="product.category" />
            <span class="lp-product-tag">{{ product.category }}</span>
          </span>
          <span class="lp-product-body">
            <h3>{{ product.title }}</h3>
            <p>{{ product.description }}</p>
            <span class="lp-product-foot">
              <span v-if="deal(product)" class="price-tag"><strong>{{ money(product.price) }}</strong><del>{{ money(product.compare_at_price) }}</del><em>-{{ discount(product) }}%</em></span>
              <strong v-else>{{ money(product.price) }}</strong>
              <span class="lp-product-cta">View in shop <ArrowUpRight :size="14" /></span>
            </span>
          </span>
        </router-link>
      </template>
    </div>
  </section>
</template>

<style scoped>
.lp-featured { margin-top: 30px; }
.lp-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}
.lp-section-head h2 { margin: 0; font-size: clamp(26px, 2.6vw, 34px); }
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
  transition: transform .25s cubic-bezier(.2,.8,.2,1), color .2s;
  flex-shrink: 0;
}
.lp-head-link:hover { color: var(--blue); transform: translateY(-3px); }

.lp-product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.lp-product {
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
  overflow: hidden;
  padding: 15px;
  border-radius: 24px;
  background: #ffffffd9;
  border: 1px solid #fff;
  box-shadow: 0 14px 34px #355c3310;
  transition: transform .35s cubic-bezier(.2,.8,.2,1), box-shadow .35s;
  animation: card-in .55s calc(var(--i, 0) * 90ms) cubic-bezier(.34,1.3,.4,1) both;
}
.lp-product:hover {
  transform: translateY(-8px);
  box-shadow: 0 26px 48px #355c331a;
}
.lp-product-art {
  position: relative;
  display: block;
  border-radius: 18px;
  background: #f1f4ec;
  overflow: hidden;
}
.lp-product-art.audio { background: #eeedf5; }
.lp-product-art.cables { background: #f3eee9; }
.lp-product-art.office { background: #f4f2e7; }
.lp-product-art > .product-art {
  display: block;
  width: 100%;
  height: 132px;
  padding: 6px;
  animation: art-float-a 7s ease-in-out infinite;
  transition: transform .3s cubic-bezier(.34,1.3,.4,1);
}
.lp-product:hover .product-art { animation: art-sway .55s cubic-bezier(.34,1.3,.4,1) both; }
.lp-product-tag {
  position: absolute;
  left: 11px;
  top: 10px;
  padding: 5px 9px;
  border-radius: 14px;
  background: #ffffffd6;
  font-size: 9px;
  letter-spacing: .6px;
  text-transform: capitalize;
  color: #5f6c5a;
}
.lp-product-body { display: flex; flex-direction: column; flex: 1; padding-top: 13px; }
.lp-product-body h3 {
  margin: 0 0 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  letter-spacing: -.3px;
  line-height: 1.35;
}
.lp-product-body p {
  margin: 0 0 14px;
  font-size: 10.5px;
  line-height: 1.65;
  color: #8d9486;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.lp-product-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: auto; }
.lp-product-foot strong { font-size: 15px; letter-spacing: -.5px; }
.lp-product-cta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 600;
  color: var(--blue);
}
.lp-product:hover .lp-product-cta svg { transform: translate(2px, -2px); }
.lp-product-cta svg { transition: transform .25s cubic-bezier(.2,.8,.2,1); }

.lp-skeleton { pointer-events: none; }
.lp-skeleton i { display: block; border-radius: 9px; background: #eef1ea; animation: lp-pulse 1.4s ease-in-out infinite; }
.lp-skeleton-art { height: 132px; border-radius: 18px !important; }
.lp-skeleton-line { height: 11px; width: 55%; margin-top: 13px; }
.lp-skeleton-line.wide { width: 82%; height: 14px; margin-top: 15px; }
@keyframes lp-pulse { 0%, 100% { opacity: .55; } 50% { opacity: 1; } }

/* glitter: travelling sweep, hover sparkles, shining title */
.lp-product::after {
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
  background: linear-gradient(100deg, #ffffff00, #fff 45%, #ffffff00 92%);
  animation: lp-sheen 7.5s ease-in-out infinite;
  animation-delay: calc(var(--i, 0) * .45s);
}
@keyframes lp-sheen {
  0%, 58% { opacity: 0; transform: translateX(0) rotate(9deg); }
  66% { opacity: .75; }
  92%, 100% { opacity: 0; transform: translateX(440%) rotate(9deg); }
}
.lp-product-art, .lp-product-body { position: relative; z-index: 1; }
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
.lp-sparks i:nth-child(1) { --dx: -42px; --dy: -38px; }
.lp-sparks i:nth-child(2) { --dx: 44px; --dy: -34px; animation-delay: .05s; }
.lp-sparks i:nth-child(3) { --dx: -36px; --dy: 44px; animation-delay: .1s; }
.lp-sparks i:nth-child(4) { --dx: 48px; --dy: 40px; animation-delay: .15s; }
.lp-product:hover .lp-sparks i { animation: lp-burst .72s cubic-bezier(.2,.8,.2,1) forwards; }
@keyframes lp-burst {
  0% { opacity: 0; transform: rotate(45deg) scale(.2); }
  30% { opacity: 1; transform: rotate(45deg) translate(calc(var(--dx) * .35), calc(var(--dy) * .35)) scale(1.15); }
  100% { opacity: 0; transform: rotate(45deg) translate(var(--dx), var(--dy)) scale(.35); }
}
@supports ((-webkit-background-clip: text) or (background-clip: text)) {
  .lp-section-head h2 {
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

@media (max-width: 1100px) { .lp-product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) {
  .lp-featured { margin-top: 22px; }
  .lp-section-head { flex-direction: column; align-items: flex-start; gap: 14px; }
  .lp-product-grid { gap: 12px; }
  .lp-product { padding: 12px; border-radius: 20px; }
  .lp-product-art > .product-art { height: 106px; }
  .lp-product-body p { font-size: 10px; }
}
@media (max-width: 430px) { .lp-product-grid { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) {
  .lp-product, .lp-product-art > .product-art, .lp-skeleton i { animation: none; }
  .lp-product:hover { transform: none; }
}
</style>
