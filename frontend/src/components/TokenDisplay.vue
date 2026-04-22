<template>
  <el-card shadow="never" class="token-card">
    <template #header>
      <span>{{ title }}</span>
    </template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="access_token">
        <div class="token-box">{{ maskedAccessToken || '暂无' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="refresh_token">
        <div class="token-box">{{ maskedRefreshToken || '暂无' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="id_token">
        <div class="token-box">{{ maskedIdToken || '暂无' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="scope">
        <el-tag
          v-for="s in scopeList"
          :key="s"
          type="warning"
          size="small"
          class="tag-gap"
        >{{ s }}</el-tag>
        <span v-if="!scopeList.length" class="no-data">暂无</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="showExpiry" label="access_token 剩余时间">
        <span :class="expiryClass">{{ remainingTimeDisplay }}</span>
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { maskToken, getTokenRemainingSeconds } from '../utils/tokenHelper'
import { splitScopeString } from '../utils/oauth2Helper'

const props = defineProps({
  title: {
    type: String,
    default: '当前 Token'
  },
  accessToken: {
    type: String,
    default: ''
  },
  refreshToken: {
    type: String,
    default: ''
  },
  idToken: {
    type: String,
    default: ''
  },
  scope: {
    type: String,
    default: ''
  },
  showExpiry: {
    type: Boolean,
    default: false
  }
})

const maskedAccessToken = computed(() => maskToken(props.accessToken))
const maskedRefreshToken = computed(() => maskToken(props.refreshToken))
const maskedIdToken = computed(() => maskToken(props.idToken))

const scopeList = computed(() => {
  if (!props.scope) return []
  return splitScopeString(props.scope)
})

const remainingSeconds = computed(() => getTokenRemainingSeconds())

const remainingTimeDisplay = computed(() => {
  if (remainingSeconds.value === null) {
    return '未知'
  }
  if (remainingSeconds.value < 0) {
    return `${Math.abs(remainingSeconds.value)} 秒前已过期`
  }
  return `${remainingSeconds.value} 秒`
})

const expiryClass = computed(() => {
  if (remainingSeconds.value === null) return 'text-info'
  if (remainingSeconds.value < 0) return 'text-danger'
  if (remainingSeconds.value < 60) return 'text-warning'
  return 'text-success'
})
</script>

<style scoped>
.token-card {
  border-radius: 12px;
}

.token-box {
  word-break: break-all;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  max-height: 100px;
  overflow-y: auto;
}

.tag-gap {
  margin: 2px;
}

.no-data {
  color: #909399;
  font-size: 13px;
}

.text-success {
  color: #67c23a;
  font-weight: 500;
}

.text-warning {
  color: #e6a23c;
  font-weight: 500;
}

.text-danger {
  color: #f56c6c;
  font-weight: 500;
}

.text-info {
  color: #909399;
}
</style>