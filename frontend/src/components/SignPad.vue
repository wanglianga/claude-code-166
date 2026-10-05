<template>
  <div class="sign-wrap">
    <canvas ref="canvasRef" :width="420" :height="160"
            class="sign-canvas" @pointerdown="start" @pointermove="move" @pointerup="end"
            @pointerleave="end"></canvas>
    <div class="sign-actions">
      <el-button size="small" @click="clear">清空重签</el-button>
      <el-button size="small" type="primary" :loading="uploading" @click="confirm">提交签字</el-button>
    </div>
    <div v-if="path" class="sign-ok">
      <el-icon color="#67c23a"><CircleCheckFilled /></el-icon>
      已采集签字：{{ signName || '家属' }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({ modelValue: { type: String, default: '' }, signName: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])
const canvasRef = ref()
const drawing = ref(false)
const path = ref(props.modelValue)
const uploading = ref(false)
let ctx, last

onMounted(() => {
  const c = canvasRef.value
  ctx = c.getContext('2d')
  ctx.lineWidth = 2.4
  ctx.lineCap = 'round'
  ctx.strokeStyle = '#1f2d3d'
  ctx.fillStyle = '#fafafa'
  ctx.fillRect(0, 0, c.width, c.height)
})

function pos(e) {
  const r = canvasRef.value.getBoundingClientRect()
  return { x: (e.clientX - r.left) * (420 / r.width), y: (e.clientY - r.top) * (160 / r.height) }
}
function start(e) {
  drawing.value = true
  canvasRef.value.setPointerCapture(e.pointerId)
  last = pos(e)
}
function move(e) {
  if (!drawing.value) return
  const p = pos(e)
  ctx.beginPath(); ctx.moveTo(last.x, last.y); ctx.lineTo(p.x, p.y); ctx.stroke()
  last = p
}
function end() { drawing.value = false }
function clear() {
  ctx.fillStyle = '#fafafa'
  ctx.fillRect(0, 0, 420, 160)
}

async function confirm() {
  // 是否空白（检查像素）
  const data = ctx.getImageData(0, 0, 420, 160).data
  let drawn = false
  for (let i = 0; i < data.length; i += 4) {
    if (data[i] < 200) { drawn = true; break }
  }
  if (!drawn) return ElMessage.warning('请家属手写签名后再提交')
  uploading.value = true
  try {
    const blob = await new Promise(r => canvasRef.value.toBlob(r, 'image/png'))
    const fd = new FormData()
    fd.append('file', new File([blob], 'sign.png', { type: 'image/png' }))
    fd.append('type', 'sign')
    const res = await api.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    path.value = res.data.path
    emit('update:modelValue', res.data.path)
    ElMessage.success('家属签字已采集')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.sign-canvas { border: 1px dashed #c0c4cc; border-radius: 6px; background: #fafafa; touch-action: none; }
.sign-actions { margin-top: 8px; display: flex; gap: 8px; }
.sign-ok { margin-top: 6px; color: #67c23a; font-size: 13px; display: flex; align-items: center; gap: 4px; }
</style>
