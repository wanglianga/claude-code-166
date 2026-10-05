<template>
  <el-upload :file-list="fileList" list-type="picture-card" :http-request="upload"
             :on-remove="remove" multiple accept="image/*">
    <el-icon><Plus /></el-icon>
  </el-upload>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  type: { type: String, default: 'visit' }
})
const emit = defineEmits(['update:modelValue'])
const fileList = ref(props.modelValue.map(p => ({ name: p, url: p })))
watch(() => props.modelValue, v => { fileList.value = (v || []).map(p => ({ name: p, url: p })) })

async function upload({ file, onSuccess, onError }) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', props.type)
  try {
    const res = await api.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    onSuccess?.(res)
    emit('update:modelValue', fileList.value.map(f => f.url || f.response?.data?.path).filter(Boolean))
  } catch (e) { onError?.(e); ElMessage.error('照片上传失败') }
}
function remove() {
  emit('update:modelValue', fileList.value.map(f => f.url).filter(Boolean))
}
</script>
