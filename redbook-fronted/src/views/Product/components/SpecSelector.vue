<template>
  <div class="spec-selector">
    <div v-for="group in groups" :key="group.key" class="spec-group">
      <div class="spec-group__title">
        <span>{{ group.title }}</span>
      </div>

      <div v-if="group.displayType === 'list'" class="spec-list">
        <button
          v-for="opt in group.options"
          :key="opt.value"
          type="button"
          class="spec-row"
          :class="{
            'is-active': selectedSpecs[group.key] === opt.value,
            'is-disabled': isOptionDisabled(group.key, opt.value)
          }"
          @click="selectOption(group.key, opt.value)"
        >
          <img v-if="getOptionImage(group.key, opt.value, opt.image)" class="spec-row__img" :src="getOptionImage(group.key, opt.value, opt.image)" />
          <div class="spec-row__content">
            <div class="spec-row__label">{{ opt.label }}</div>
          </div>
          <span v-if="opt.badgeText" class="spec-row__badge">{{ opt.badgeText }}</span>
        </button>
      </div>

      <div v-else class="spec-chips">
        <button
          v-for="opt in group.options"
          :key="opt.value"
          type="button"
          class="spec-chip"
          :class="{
            'is-active': selectedSpecs[group.key] === opt.value,
            'is-disabled': isOptionDisabled(group.key, opt.value)
          }"
          @click="selectOption(group.key, opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { getImageUrl } from '@/utils/image'

const SKU_FALLBACK_KEY = '__sku'

const props = defineProps({
  skus: { type: Array, default: () => [] },
  specGroups: { type: Array, default: () => [] },
  skuId: { type: [Number, String, null], default: null }
})

const emit = defineEmits(['update:skuId', 'change'])

const selectedSpecs = reactive({})

const normalizeSpecs = (specs) => {
  if (!specs) return {}
  if (typeof specs === 'object') return specs
  try {
    return JSON.parse(specs)
  } catch {
    return {}
  }
}

const normalizedSkus = computed(() => {
  return (props.skus || []).map((sku) => ({
    ...sku,
    _specs: normalizeSpecs(sku.specs),
    _priceNumber: sku.price != null ? Number(sku.price) : Number.POSITIVE_INFINITY,
    _stockNumber: sku.stock != null ? Number(sku.stock) : 0
  }))
})

const inferGroupsFromSkus = (skus) => {
  const valuesByKey = new Map()
  for (const sku of skus) {
    const map = sku._specs || {}
    for (const [k, v] of Object.entries(map)) {
      if (v == null || v === '') continue
      if (!valuesByKey.has(k)) valuesByKey.set(k, new Set())
      valuesByKey.get(k).add(String(v))
    }
  }

  const defaultOrder = ['version', 'color', 'disk', 'ram', 'storage', 'size', 'package']
  const titleMap = {
    version: '版本',
    color: '颜色分类',
    disk: '硬盘容量',
    ram: '内存容量',
    storage: '存储容量',
    size: '尺码',
    package: '套餐类型'
  }
  const keys = [...valuesByKey.keys()].sort((a, b) => {
    const ia = defaultOrder.indexOf(a)
    const ib = defaultOrder.indexOf(b)
    const wa = ia < 0 ? 999 : ia
    const wb = ib < 0 ? 999 : ib
    if (wa !== wb) return wa - wb
    return String(a).localeCompare(String(b))
  })

  if (keys.length === 0) {
    const options = skus
      .filter((s) => s._stockNumber > 0)
      .map((s) => ({ value: String(s.id), label: s.name || String(s.id) }))
    return options.length > 0
      ? [
          {
            key: SKU_FALLBACK_KEY,
            title: '规格',
            displayType: 'chip',
            options
          }
        ]
      : []
  }

  return keys.map((key) => {
    const values = [...valuesByKey.get(key)].sort((a, b) => String(a).localeCompare(String(b)))
    return {
      key,
      title: titleMap[key] || key,
      displayType: String(key).toLowerCase().includes('color') ? 'list' : 'chip',
      options: values.map((v) => ({ value: v, label: v }))
    }
  })
}

const groups = computed(() => {
  if (props.specGroups && props.specGroups.length > 0) return props.specGroups
  return inferGroupsFromSkus(normalizedSkus.value)
})

const matchesSelected = (candidateSelected) => {
  const entries = Object.entries(candidateSelected).filter(([, v]) => v != null && v !== '')
  if (entries.length === 0) {
    return normalizedSkus.value.filter((s) => s._stockNumber > 0)
  }
  if (candidateSelected[SKU_FALLBACK_KEY] != null && candidateSelected[SKU_FALLBACK_KEY] !== '') {
    return normalizedSkus.value.filter(
      (s) => s._stockNumber > 0 && String(s.id) === String(candidateSelected[SKU_FALLBACK_KEY])
    )
  }
  return normalizedSkus.value.filter((s) => {
    if (s._stockNumber <= 0) return false
    for (const [k, v] of entries) {
      if (String(s._specs?.[k] ?? '') !== String(v)) return false
    }
    return true
  })
}

const chooseSkuId = (skus, preferredSkuId) => {
  if (!skus || skus.length === 0) return null
  if (preferredSkuId != null) {
    const found = skus.find((s) => String(s.id) === String(preferredSkuId))
    if (found) return found.id
  }
  const sorted = [...skus].sort((a, b) => a._priceNumber - b._priceNumber)
  return sorted[0].id
}

const setSelectedFromSku = (sku) => {
  const map = sku?._specs || {}
  if (!map || Object.keys(map).length === 0) {
    selectedSpecs[SKU_FALLBACK_KEY] = String(sku?.id ?? '')
  } else if (selectedSpecs[SKU_FALLBACK_KEY] != null) {
    delete selectedSpecs[SKU_FALLBACK_KEY]
  }
  for (const group of groups.value) {
    if (group.key === SKU_FALLBACK_KEY) {
      continue
    }
    if (map[group.key] != null) {
      selectedSpecs[group.key] = String(map[group.key])
    } else if (selectedSpecs[group.key] != null) {
      delete selectedSpecs[group.key]
    }
  }
}

const initDefaultSelection = () => {
  const skus = normalizedSkus.value.filter((s) => s._stockNumber > 0)
  if (skus.length === 0) return
  const preferred = props.skuId != null ? props.skuId : null
  const nextSkuId = chooseSkuId(skus, preferred)
  const sku = skus.find((s) => String(s.id) === String(nextSkuId))
  if (!sku) return
  setSelectedFromSku(sku)
  emit('update:skuId', sku.id)
  emit('change', { skuId: sku.id, selectedSpecs: { ...selectedSpecs } })
}

watch(
  () => [normalizedSkus.value, groups.value],
  () => {
    initDefaultSelection()
  },
  { immediate: true, deep: true }
)

watch(
  () => props.skuId,
  (id) => {
    if (id == null) return
    const sku = normalizedSkus.value.find((s) => String(s.id) === String(id))
    if (!sku) return
    setSelectedFromSku(sku)
  },
  { immediate: true }
)

const isOptionDisabled = (groupKey, optionValue) => {
  if (groupKey === SKU_FALLBACK_KEY) {
    const sku = normalizedSkus.value.find((s) => String(s.id) === String(optionValue))
    return !sku || sku._stockNumber <= 0
  }
  const candidate = { ...selectedSpecs, [groupKey]: optionValue }
  return matchesSelected(candidate).length === 0
}

const selectOption = (groupKey, optionValue) => {
  if (isOptionDisabled(groupKey, optionValue)) return
  if (groupKey === SKU_FALLBACK_KEY) {
    selectedSpecs[SKU_FALLBACK_KEY] = String(optionValue)
    emit('update:skuId', optionValue)
    emit('change', { skuId: optionValue, selectedSpecs: { ...selectedSpecs } })
    return
  }
  const candidate = { ...selectedSpecs, [groupKey]: optionValue }
  const matches = matchesSelected(candidate)
  if (matches.length === 0) return
  selectedSpecs[groupKey] = optionValue
  const nextSkuId = chooseSkuId(matches, props.skuId)
  emit('update:skuId', nextSkuId)
  emit('change', { skuId: nextSkuId, selectedSpecs: { ...selectedSpecs } })
}

const getOptionImage = (groupKey, optionValue, optionImage) => {
  if (optionImage) return getImageUrl(optionImage)
  const sku = normalizedSkus.value.find((s) => String(s._specs?.[groupKey] ?? '') === String(optionValue) && !!s.image)
  return sku?.image ? getImageUrl(sku.image) : ''
}
</script>

<style scoped>
.spec-group__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
  margin: 16px 0 10px;
}

.spec-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-chip {
  border: 1px solid #e9e9e9;
  border-radius: 10px;
  padding: 8px 12px;
  background: #fff;
  color: #111;
  line-height: 1;
  cursor: pointer;
}

.spec-chip.is-active {
  border-color: #ff2442;
  color: #ff2442;
}

.spec-chip.is-disabled {
  background: #f5f5f5;
  color: #bdbdbd;
  cursor: not-allowed;
}

.spec-list {
  display: grid;
  gap: 10px;
}

.spec-row {
  width: 100%;
  border: 1px solid #e9e9e9;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.spec-row.is-active {
  border-color: #ff2442;
  color: #ff2442;
}

.spec-row.is-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.spec-row__img {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  object-fit: cover;
  flex: 0 0 auto;
}

.spec-row__content {
  flex: 1;
  min-width: 0;
}

.spec-row__label {
  font-size: 13px;
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spec-row__badge {
  font-size: 11px;
  background: #ff6a00;
  color: #fff;
  padding: 2px 6px;
  border-radius: 8px;
  flex: 0 0 auto;
}
</style>
