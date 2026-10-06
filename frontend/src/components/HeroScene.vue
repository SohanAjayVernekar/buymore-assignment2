<script setup>
// Real WebGL hero scene: a procedurally built pair of headphones that floats,
// turns toward the cursor and glitters. three.js is imported lazily so the
// hero shell paints first, and the whole scene falls back to the flat SVG art
// when WebGL is unavailable.
import { onBeforeUnmount, onMounted, ref } from 'vue'
import ProductArt from './ProductArt.vue'

const props = defineProps({
  title: { type: String, default: 'Noise Cancelling Headphones' },
  category: { type: String, default: 'audio' },
})

const host = ref(null)
const ok = ref(null) // null = checking, false = fall back to the SVG art

let stop = null

function hasWebGL() {
  try {
    const canvas = document.createElement('canvas')
    return !!(window.WebGLRenderingContext && (canvas.getContext('webgl2') || canvas.getContext('webgl')))
  } catch {
    return false
  }
}

onMounted(async () => {
  if (!hasWebGL()) { ok.value = false; return }
  try {
    stop = await start(host.value)
    ok.value = true
  } catch {
    ok.value = false
  }
})

onBeforeUnmount(() => { stop?.() })

async function start(el) {
  const THREE = await import('three')

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'low-power' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
  renderer.setClearColor(0x000000, 0)
  const canvas = renderer.domElement
  canvas.style.width = '100%'
  canvas.style.height = '100%'
  canvas.style.display = 'block'
  el.appendChild(canvas)

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(34, 1, 0.1, 100)
  camera.position.set(0, 0.1, 7.4)

  /* ---- headphones, built from geometry ---- */
  const graphite = new THREE.MeshStandardMaterial({ color: 0x23292a, roughness: 0.42, metalness: 0.45 })
  const dark = new THREE.MeshStandardMaterial({ color: 0x14181a, roughness: 0.62, metalness: 0.3 })
  const cushion = new THREE.MeshStandardMaterial({ color: 0x2b3134, roughness: 0.95, metalness: 0.05 })
  const steel = new THREE.MeshStandardMaterial({ color: 0x9aa3a6, roughness: 0.28, metalness: 0.9 })
  const accent = new THREE.MeshStandardMaterial({ color: 0x245cff, roughness: 0.3, metalness: 0.5 })

  const parts = []
  const add = (geometry, material, position, rotation) => {
    const mesh = new THREE.Mesh(geometry, material)
    if (position) mesh.position.set(...position)
    if (rotation) mesh.rotation.set(...rotation)
    parts.push(mesh)
    return mesh
  }

  const model = new THREE.Group()

  // headband: an arc across the top, plus a slim inner rail
  add(new THREE.TorusGeometry(1.18, 0.085, 18, 90, Math.PI), graphite, [0, 0.02, 0])
  add(new THREE.TorusGeometry(1.03, 0.045, 14, 90, Math.PI), steel, [0, 0.02, 0])
  // headband pad at the crown
  add(new THREE.CapsuleGeometry(0.13, 0.5, 6, 16), cushion, [0, 1.16, 0], [0, 0, Math.PI / 2])

  for (const side of [-1, 1]) {
    // yoke linking band to cup
    add(new THREE.BoxGeometry(0.07, 0.42, 0.14), steel, [side * 1.16, -0.14, 0])
    // ear cup shell (cylinder axis turned to face sideways)
    add(new THREE.CylinderGeometry(0.47, 0.44, 0.26, 46), graphite, [side * 1.18, -0.44, 0], [0, 0, Math.PI / 2])
    // face plate
    add(new THREE.CylinderGeometry(0.3, 0.3, 0.05, 40), dark, [side * 1.33, -0.44, 0], [0, 0, Math.PI / 2])
    // brand ring on the face
    add(new THREE.TorusGeometry(0.34, 0.028, 12, 44), accent, [side * 1.3, -0.44, 0], [0, Math.PI / 2, 0])
    // cushion ring facing the ear
    add(new THREE.TorusGeometry(0.36, 0.13, 16, 40), cushion, [side * 1.03, -0.44, 0], [0, Math.PI / 2, 0])
  }
  model.add(...parts)
  model.rotation.set(-0.12, -0.45, 0.06)
  model.position.y = 0.12
  scene.add(model)

  /* ---- glitter: twinkling additive points ---- */
  const COUNT = 110
  const positions = new Float32Array(COUNT * 3)
  const phases = new Float32Array(COUNT)
  const scales = new Float32Array(COUNT)
  const colors = new Float32Array(COUNT * 3)
  const palette = [new THREE.Color(0xffffff), new THREE.Color(0xffe9a6), new THREE.Color(0xa9c4ff), new THREE.Color(0xd9f0a8)]
  for (let i = 0; i < COUNT; i++) {
    const angle = Math.random() * Math.PI * 2
    const radius = 1.7 + Math.random() * 2.1
    positions[i * 3] = Math.cos(angle) * radius
    positions[i * 3 + 1] = (Math.random() - 0.35) * 3.4
    positions[i * 3 + 2] = Math.sin(angle) * radius * 0.75
    phases[i] = Math.random() * Math.PI * 2
    scales[i] = 0.55 + Math.random() * 1.15
    const c = palette[(Math.random() * palette.length) | 0]
    colors[i * 3] = c.r; colors[i * 3 + 1] = c.g; colors[i * 3 + 2] = c.b
  }
  const sparkleGeometry = new THREE.BufferGeometry()
  sparkleGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  sparkleGeometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  sparkleGeometry.setAttribute('aScale', new THREE.BufferAttribute(scales, 1))
  sparkleGeometry.setAttribute('aColor', new THREE.BufferAttribute(colors, 3))

  const sparkleMaterial = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: { uTime: { value: 0 }, uPixelRatio: { value: renderer.getPixelRatio() } },
    vertexShader: `
      attribute float aPhase; attribute float aScale; attribute vec3 aColor;
      uniform float uTime; uniform float uPixelRatio;
      varying float vAlpha; varying vec3 vColor;
      void main() {
        vColor = aColor;
        float tw = sin(uTime * 1.7 + aPhase) * 0.5 + 0.5;
        vAlpha = pow(tw, 3.0);
        vec4 mv = modelViewMatrix * vec4(position, 1.0);
        gl_PointSize = (3.5 + 9.0 * tw) * aScale * uPixelRatio * (300.0 / max(0.001, -mv.z)) / 100.0;
        gl_Position = projectionMatrix * mv;
      }`,
    fragmentShader: `
      varying float vAlpha; varying vec3 vColor;
      void main() {
        float d = length(gl_PointCoord - vec2(0.5));
        float core = smoothstep(0.5, 0.0, d);
        float star = smoothstep(0.5, 0.42, d) * 0.0;
        if (core <= 0.001) discard;
        gl_FragColor = vec4(vColor, core * core * vAlpha);
      }`,
  })
  const sparkles = new THREE.Points(sparkleGeometry, sparkleMaterial)
  scene.add(sparkles)

  /* ---- lighting ---- */
  scene.add(new THREE.HemisphereLight(0xe6efff, 0x9db58a, 0.75))
  const key = new THREE.DirectionalLight(0xffffff, 2.1)
  key.position.set(3.2, 4, 5)
  scene.add(key)
  const rim = new THREE.DirectionalLight(0x9fc0ff, 1.5)
  rim.position.set(-4.5, 1.5, -3)
  scene.add(rim)
  const bounce = new THREE.PointLight(0x245cff, 14, 14)
  bounce.position.set(0, -2.2, 3)
  scene.add(bounce)

  /* ---- sizing ---- */
  const resize = () => {
    const w = el.clientWidth || 1
    const h = el.clientHeight || 1
    renderer.setSize(w, h, false)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    sparkleMaterial.uniforms.uPixelRatio.value = renderer.getPixelRatio()
  }
  resize()
  const ro = 'ResizeObserver' in window ? new ResizeObserver(resize) : null
  ro?.observe(el)

  /* ---- pointer tilt across the hero stage ---- */
  const target = { x: 0, y: 0 }
  const eased = { x: 0, y: 0 }
  const zone = el.closest('.lp-stage') || el
  const move = event => {
    if (event.pointerType === 'touch' || reduce) return
    const box = zone.getBoundingClientRect()
    target.x = ((event.clientX - box.left) / box.width - 0.5) * 2
    target.y = ((event.clientY - box.top) / box.height - 0.5) * 2
  }
  const reset = () => { target.x = 0; target.y = 0 }
  zone.addEventListener('pointermove', move)
  zone.addEventListener('pointerleave', reset)
  zone.addEventListener('pointercancel', reset)

  /* ---- loop, paused when off-screen or hidden ---- */
  const started = performance.now()
  let frame = 0
  let running = true
  let visible = true
  const draw = () => {
    const t = (performance.now() - started) / 1000
    eased.x += (target.x - eased.x) * 0.055
    eased.y += (target.y - eased.y) * 0.055
    model.rotation.y = -0.45 + eased.x * 0.55 + t * 0.16
    model.rotation.x = -0.12 + eased.y * 0.24
    model.position.y = 0.12 + Math.sin(t * 0.9) * 0.07
    sparkleMaterial.uniforms.uTime.value = t
    sparkles.rotation.y = t * 0.05
    renderer.render(scene, camera)
  }
  const tick = () => {
    if (!running) return
    if (visible && !document.hidden) draw()
    frame = requestAnimationFrame(tick)
  }
  if (reduce) draw()
  else tick()

  const io = 'IntersectionObserver' in window
    ? new IntersectionObserver(entries => { visible = entries[0]?.isIntersecting ?? true }, { threshold: 0 })
    : null
  io?.observe(el)
  const onVisibility = () => { if (!document.hidden && running && !reduce) draw() }
  document.addEventListener('visibilitychange', onVisibility)

  return () => {
    running = false
    cancelAnimationFrame(frame)
    io?.disconnect()
    ro?.disconnect()
    document.removeEventListener('visibilitychange', onVisibility)
    zone.removeEventListener('pointermove', move)
    zone.removeEventListener('pointerleave', reset)
    zone.removeEventListener('pointercancel', reset)
    sparkleGeometry.dispose()
    sparkleMaterial.dispose()
    parts.forEach(mesh => mesh.geometry.dispose())
    ;[graphite, dark, cushion, steel, accent].forEach(material => material.dispose())
    renderer.dispose()
    canvas.remove()
  }
}
</script>

<template>
  <div class="hero-scene">
    <div v-show="ok !== false" ref="host" class="hero-scene-stage"></div>
    <ProductArt v-if="ok === false" class="lp-art" :title="props.title" :category="props.category" />
  </div>
</template>

<style scoped>
.hero-scene { position: relative; display: block; min-width: 0; }
.hero-scene-stage { position: absolute; inset: 0; }
.hero-scene-stage :deep(canvas) { touch-action: pan-y; }
/* flat fallback keeps the pose the 3D model would have had */
.hero-scene :deep(.product-art) {
  display: block;
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 26px 16px #28395e2b);
  transform: rotate(-11deg);
}
</style>
