<template>
  <el-card shadow="never" class="token-card">
    <template #header>
      <span>{{ title }}</span>
    </template>
    <el-descriptions :column="1" border class="token-descriptions">
      <el-descriptions-item label="access_token" label-class-name="token-label-cell">
        <div class="token-box">{{ accessToken || '暂无' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="refresh_token" label-class-name="token-label-cell">
        <div class="token-box">{{ refreshToken || '暂无' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="id_token" label-class-name="token-label-cell">
        <div class="token-box">{{ idToken || '暂无' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="scope" label-class-name="token-label-cell">
        <el-tag
          v-for="s in scopeList"
          :key="s"
          type="warning"
          size="small"
          class="tag-gap"
        >{{ s }}</el-tag>
        <span v-if="!scopeList.length" class="no-data">暂无</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="tokenType" label="token_type" label-class-name="token-label-cell">
        <span>{{ tokenType }}</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="expiresInDisplay" label="expires_in" label-class-name="token-label-cell">
        <span>{{ expiresInDisplay }}</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="showExpiry" label="access_token 剩余时间" label-class-name="token-label-cell">
        <span :class="expiryClass">{{ remainingTimeDisplay }}</span>
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getTokenRemainingSeconds } from '../utils/tokenHelper'
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
  tokenType: {
    type: String,
    default: ''
  },
  expiresIn: {
    type: [String, Number],
    default: ''
  },
  showExpiry: {
    type: Boolean,
    default: false
  }
})

const nowTimestamp = ref(Date.now())
let countdownTimer = null

const maskedAccessToken = computed(() => maskToken(props.accessToken))
const maskedRefreshToken = computed(() => maskToken(props.refreshToken))
const maskedIdToken = computed(() => maskToken(props.idToken))

const scopeList = computed(() => {
  if (!props.scope) return []
  return splitScopeString(props.scope)
})

onMounted(() => {
  if (!props.showExpiry) {
    return
  }
  countdownTimer = window.setInterval(() => {
    nowTimestamp.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
})

const expiresInDisplay = computed(() => {
  if (props.expiresIn === '' || props.expiresIn === null || props.expiresIn === undefined) {
    return ''
  }
  return `${props.expiresIn} 秒`
})

const remainingSeconds = computed(() => {
  nowTimestamp.value
  return getTokenRemainingSeconds()
})

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

.token-descriptions :deep(.token-label-cell.el-descriptions__cell) {
  width: 160px;
  min-width: 160px;
  white-space: nowrap;
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
