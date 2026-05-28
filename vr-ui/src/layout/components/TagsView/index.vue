<template>
  <div class="tags-view" v-if="views.length > 0">
    <el-tag
      v-for="view in views" :key="view.path"
      :closable="!view.affix"
      :type="isActive(view) ? '' : 'info'"
      :effect="isActive(view) ? 'dark' : 'plain'"
      @click="navigate(view)"
      @close="close(view)"
      size="small"
      class="tag-item"
    >
      {{ view.meta?.title || view.name }}
    </el-tag>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTagsViewStore } from '@/store/modules/tagsView';

const route = useRoute();
const router = useRouter();
const store = useTagsViewStore();

const views = computed(() => store.visitedViews);

watch(() => route.path, () => {
  store.addView({ ...route, meta: route.meta });
}, { immediate: true });

function isActive(view) { return route.path === view.path; }
function navigate(view) { router.push(view.path); }
function close(view) { store.delView(view); }
</script>

<style scoped>
.tags-view {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.tag-item { cursor: pointer; }
</style>
