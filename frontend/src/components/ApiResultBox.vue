<template>
  <el-card shadow="never" class="result-card">
    <template #header>
      <div class="card-header">
        <span>接口响应 / 调试输出</span>
        <el-button
          v-if="Object.keys(result).length > 1 || result.message"
          size="small"
          :icon="DocumentCopy"
          @click="copyResult"
        >
          复制
        </el-button>
      </div>
    </template>
    <pre class="result-box">{{ prettyResult }}</pre>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  result: {
    type: Object,
    default: () => ({})
  }
})

const prettyResult = computed(() => {
  if (!props.result || Object.keys(props.result).length === 0) {
    return '暂无数据'
  }
  return JSON.stringify(props.result, null, 2)
})

function copyResult() {
  const text = prettyResult.value
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style scoped>
.result-card {
  margin-top: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.result-box {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #2c3e50;
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.result-box::-webkit-scrollbar {
  width: 6px;
}

.result-box::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.result-box::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>