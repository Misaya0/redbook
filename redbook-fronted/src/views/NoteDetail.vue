<template>
  <div class="note-detail-wrapper">
    <NoteDetailModal 
      :visible="true" 
      :noteId="noteId" 
      @update:visible="handleClose"
    />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import NoteDetailModal from '@/components/NoteDetailModal.vue'

const route = useRoute()
const router = useRouter()
const noteId = route.params.id

const handleClose = (val) => {
  if (!val) {
      router.back()
  }
}
</script>

<style scoped>
/* Empty style */
</style>