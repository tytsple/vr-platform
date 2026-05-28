import { defineStore } from 'pinia';

export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [],
  }),
  actions: {
    addView(view) {
      if (this.visitedViews.some(v => v.path === view.path)) return;
      this.visitedViews.push(view);
    },
    delView(view) {
      this.visitedViews = this.visitedViews.filter(v => v.path !== view.path);
    },
    delOthersViews(view) {
      this.visitedViews = this.visitedViews.filter(v => v.affix || v.path === view.path);
    },
    closeAllViews() {
      this.visitedViews = this.visitedViews.filter(v => v.affix);
    },
  },
});
