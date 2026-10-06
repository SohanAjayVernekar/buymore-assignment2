<script setup>
import { Sparkles } from 'lucide-vue-next'
const phrases = ['WORK A LITTLE BETTER', 'PLAY A LITTLE LOUDER', 'MAKE IT YOURS', 'EVERYDAY, UPGRADED']
</script>

<template>
  <div class="lp-ribbon" aria-label="Collection themes">
    <div class="lp-ribbon-track">
      <span v-for="copy in 2" :key="copy" class="lp-ribbon-group" :aria-hidden="copy === 2 ? 'true' : undefined">
        <template v-for="phrase in phrases" :key="phrase">
          <span>{{ phrase }}</span>
          <Sparkles :size="15" aria-hidden="true" />
        </template>
      </span>
    </div>
  </div>
</template>

<style scoped>
.lp-ribbon {
  margin-top: 24px;
  padding: 17px 0;
  overflow: hidden;
  border-block: 1px solid #ffffffcc;
  background: #ffffff7a;
  border-radius: 22px;
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 7%, #000 93%, transparent);
  mask-image: linear-gradient(90deg, transparent, #000 7%, #000 93%, transparent);
}
.lp-ribbon-track {
  display: flex;
  width: max-content;
  animation: lp-marquee 34s linear infinite;
}
.lp-ribbon:hover .lp-ribbon-track { animation-play-state: paused; }
.lp-ribbon-group {
  display: flex;
  align-items: center;
  gap: 30px;
  padding-right: 30px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2.6px;
  color: #4f6153;
  white-space: nowrap;
}
.lp-ribbon-group svg { color: var(--blue); animation: art-float-b 5s ease-in-out infinite; }
@keyframes lp-marquee { from { transform: translateX(0); } to { transform: translateX(-50%); } }
@media (max-width: 700px) {
  .lp-ribbon { margin-top: 14px; padding: 14px 0; border-radius: 17px; }
  .lp-ribbon-group { gap: 20px; padding-right: 20px; font-size: 9px; letter-spacing: 2px; }
}
@media (prefers-reduced-motion: reduce) {
  .lp-ribbon-track, .lp-ribbon-group svg { animation: none; }
}
</style>
