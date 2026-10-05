<template>
  <div class="sig-wrap">
    <canvas ref="canvas" class="sig-canvas" @pointerdown="down" @pointermove="move"
            @pointerup="up" @pointerleave="up"></canvas>
    <div class="sig-bar">
      <span class="muted">家属在此手写签名</span>
      <el-button size="small" link type="danger" @click="clear">清除重签</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])
const canvas = ref(null)
let ctx, drawing = false

onMounted(() => {
  const c = canvas.value
  c.width = c.clientWidth
  c.height = c.clientHeight
  ctx = c.getContext('2d')
  ctx.lineWidth = 2.2
  ctx.lineCap = 'round'
  ctx.strokeStyle = '#1e4e7e'
})

function pos(e) {
  const r = canvas.value.getBoundingClientRect()
  return { x: e.clientX - r.left, y: e.clientY - r.top }
}
function down(e) {
  drawing = true
  const p = pos(e)
  ctx.beginPath(); ctx.moveTo(p.x, p.y)
  canvas.value.setPointerCapture(e.pointerId)
}
function move(e) {
  if (!drawing) return
  const p = pos(e)
  ctx.lineTo(p.x, p.y); ctx.stroke()
}
function up() {
  if (!drawing) return
  drawing = false
  emit('update:modelValue', canvas.value.toDataURL('image/png'))
}
function clear() {
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  emit('update:modelValue', '')
}
</script>

<style scoped>
.sig-wrap { border: 1px dashed #b3c8dd; border-radius: 8px; background: #fbfdff; }
.sig-canvas { width: 100%; height: 130px; display: block; touch-action: none; cursor: crosshair; }
.sig-bar { display:flex; justify-content:space-between; padding: 2px 8px; }
</style>
