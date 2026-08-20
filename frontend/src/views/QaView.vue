<template>
  <div class="page qa-page">
    <PageHeader title="经营追问" subtitle="在选定范围内直接向经营数据提问" />

    <div class="panel scope-panel">
      <div class="scope-title">分析范围</div>
      <div class="scope-row">
        <el-select v-model="region" clearable placeholder="地区部" style="width: 200px">
          <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
        </el-select>
        <el-select v-model="period" clearable placeholder="期间" style="width: 180px">
          <el-option v-for="p in periodOptions" :key="p" :label="p" :value="p" />
        </el-select>
        <el-select v-model="themeId" clearable placeholder="主题" style="width: 180px">
          <el-option v-for="t in themes" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>
    </div>

    <div class="panel chat-panel">
      <div class="chat-list" ref="chatListRef">
        <div v-for="(msg, i) in messages" :key="i" class="chat-item" :class="msg.role">
          <div class="chat-role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="chat-content">{{ msg.content }}</div>
          <div v-if="msg.evidence?.length" class="chat-evidence">
            <div v-for="e in msg.evidence" :key="e" class="evidence-line">{{ e }}</div>
          </div>
        </div>
        <div v-if="!messages.length" class="muted empty-chat">输入经营问题，例如“哪些代表处收入增长超过 20%，但利润增长没有超过 10%？”</div>
      </div>
      <div class="chat-input">
        <el-input v-model="question" placeholder="输入经营问题..." @keyup.enter="ask" />
        <el-button type="primary" :loading="asking" @click="ask">追问</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import { getModelData, listMaterials, listThemes } from '../api'
import type { Material, ModelData, Theme } from '../types'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  evidence?: string[]
}

const materials = ref<Material[]>([])
const themes = ref<Theme[]>([])
const region = ref('')
const period = ref('')
const themeId = ref<number>()
const question = ref('')
const asking = ref(false)
const messages = ref<ChatMessage[]>([])
const chatListRef = ref<HTMLElement>()

const regionOptions = computed(() => [...new Set(materials.value.map((m) => m.region || '默认地区部'))])
const periodOptions = computed(() => [...new Set(materials.value.map((m) => m.reportPeriod).filter(Boolean))] as string[])
const scopedMaterials = computed(() =>
  materials.value.filter(
    (m) =>
      m.status === 'COMPLETED' &&
      (!region.value || (m.region || '默认地区部') === region.value) &&
      (!period.value || m.reportPeriod === period.value) &&
      (!themeId.value || m.themeId === themeId.value)
  )
)

const ask = async () => {
  const q = question.value.trim()
  if (!q) return
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  asking.value = true
  try {
    const dataByMaterial = await Promise.all(
      scopedMaterials.value.map(async (m) => {
        const data = await getModelData(m.id)
        return { material: m, data }
      })
    )
    const answer = buildAnswer(q, dataByMaterial)
    messages.value.push(answer)
    await nextTick()
    if (chatListRef.value) chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  } finally {
    asking.value = false
  }
}

function valueOf(data: ModelData[], code: string) {
  return data.find((d) => d.fieldCode === code)?.fieldValue
}

function buildAnswer(
  q: string,
  rows: Array<{ material: Material; data: ModelData[] }>
): ChatMessage {
  const lower = q.toLowerCase()
  const evidence: string[] = []
  if (lower.includes('收入') && lower.includes('利润')) {
    const matched = rows.filter((r) => {
      const g = Number(valueOf(r.data, 'revenue_growth') ?? NaN)
      const p = Number(valueOf(r.data, 'profit_growth') ?? NaN)
      return !Number.isNaN(g) && !Number.isNaN(p) && g > 20 && p < 10
    })
    matched.forEach((r) => {
      evidence.push(
        `${r.material.organization}：收入 ${Number(valueOf(r.data, 'revenue') ?? 0).toLocaleString()}，收入同比 ${valueOf(r.data, 'revenue_growth')}%，利润同比 ${valueOf(r.data, 'profit_growth')}%`
      )
    })
    return {
      role: 'assistant',
      content: matched.length
        ? `共有 ${matched.length} 个代表处符合“收入增长超过 20%、利润增长低于 10%”。`
        : '当前范围内没有代表处同时满足该条件。',
      evidence
    }
  }
  if (lower.includes('dso') || lower.includes('回款')) {
    rows.forEach((r) => {
      const dso = valueOf(r.data, 'dso')
      const collection = valueOf(r.data, 'collection')
      evidence.push(`${r.material.organization}：DSO ${dso ?? '-'} 天，回款 ${collection ?? '-'}`)
    })
    return {
      role: 'assistant',
      content: `当前范围共 ${rows.length} 个代表处，回款与 DSO 情况如下。`,
      evidence
    }
  }
  rows.forEach((r) => {
    evidence.push(
      `${r.material.organization}：收入 ${valueOf(r.data, 'revenue') ?? '-'}，收入同比 ${valueOf(r.data, 'revenue_growth') ?? '-'}%，利润同比 ${valueOf(r.data, 'profit_growth') ?? '-'}%`
    )
  })
  return {
    role: 'assistant',
    content: `当前范围内已完成 ${rows.length} 份材料分析，关键经营指标如下。`,
    evidence
  }
}

onMounted(async () => {
  materials.value = await listMaterials()
  themes.value = await listThemes()
})
</script>

<style scoped>
.qa-page {
  height: calc(100vh - 110px);
}
.scope-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.scope-title {
  font-weight: 600;
}
.scope-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.chat-panel {
  flex: 1;
  min-height: 480px;
  display: flex;
  flex-direction: column;
}
.chat-list {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px;
  margin-bottom: 12px;
}
.chat-item {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 8px;
}
.chat-item.user {
  align-self: flex-end;
  background: var(--brand);
  color: #fff;
}
.chat-item.assistant {
  align-self: flex-start;
  background: #f8fafc;
  border: 1px solid var(--line);
}
.chat-role {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}
.chat-content {
  line-height: 1.7;
  white-space: pre-wrap;
}
.chat-evidence {
  margin-top: 8px;
  border-top: 1px solid var(--line);
  padding-top: 8px;
  font-size: 12px;
  color: var(--muted);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.chat-input {
  display: flex;
  gap: 10px;
}
.empty-chat {
  text-align: center;
  margin: auto;
}
</style>
