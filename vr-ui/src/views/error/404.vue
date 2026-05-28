<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.error-page h1 { font-size: 72px; color: #409EFF; margin: 0; }
.error-page p { font-size: 18px; color: #909399; margin: 16px 0 24px; }
</style>
