#!/bin/bash

# 批量替换HTML文件中的CDN链接为本地文件路径

echo "开始替换CDN链接为本地文件路径..."

# 替换Element UI CSS链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/element-ui/lib/theme-chalk/index.css|/lib/element-ui.css|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/element-ui/2.15.13/theme-chalk/index.css|/lib/element-ui.css|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/element-ui/lib/theme-chalk/index.css|/lib/element-ui.css|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/element-ui/2.15.13/theme-chalk/index.css|/lib/element-ui.css|g' {} \;

# 替换Vue.js链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/vue/2.6.14/vue.min.js|/lib/vue.min.js|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/vue@2.6.14/dist/vue.js|/lib/vue.min.js|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/vue@2.6.14/dist/vue.min.js|/lib/vue.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/vue/2.6.14/vue.min.js|/lib/vue.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/vue@2.6.14/dist/vue.js|/lib/vue.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/vue@2.6.14/dist/vue.min.js|/lib/vue.min.js|g' {} \;

# 替换Element UI JS链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/element-ui/2.15.13/index.min.js|/lib/element-ui.min.js|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/element-ui/lib/index.js|/lib/element-ui.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/element-ui/2.15.13/index.min.js|/lib/element-ui.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/element-ui/lib/index.js|/lib/element-ui.min.js|g' {} \;

# 替换Axios链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/axios/1.3.6/axios.min.js|/lib/axios.min.js|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/axios/dist/axios.min.js|/lib/axios.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/axios/1.3.6/axios.min.js|/lib/axios.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://unpkg.com/axios/dist/axios.min.js|/lib/axios.min.js|g' {} \;

# 替换Crypto-js链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/crypto-js/4.1.1/crypto-js.min.js|/lib/crypto-js.min.js|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.1.1/crypto-js.min.js|/lib/crypto-js.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/crypto-js/4.1.1/crypto-js.min.js|/lib/crypto-js.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.1.1/crypto-js.min.js|/lib/crypto-js.min.js|g' {} \;

# 替换ECharts链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/echarts/5.4.2/echarts.min.js|/lib/echarts.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.bootcdn.net/ajax/libs/echarts/5.4.2/echarts.min.js|/lib/echarts.min.js|g' {} \;

# 替换jQuery链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/jquery@3.6.0/dist/jquery.min.js|/lib/jquery.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/jquery@3.6.0/dist/jquery.min.js|/lib/jquery.min.js|g' {} \;

# 替换Bootstrap CSS链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css|/lib/bootstrap.min.css|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css|/lib/bootstrap.min.css|g' {} \;

# 替换Bootstrap JS链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js|/lib/bootstrap.bundle.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js|/lib/bootstrap.bundle.min.js|g' {} \;

# 替换Animate.css链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/animate.css@4.1.1/animate.min.css|/lib/animate.min.css|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/animate.css@4.1.1/animate.min.css|/lib/animate.min.css|g' {} \;

# 替换Vis.js链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/vis-network@9.1.2/dist/vis-network.min.js|/lib/vis-network.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdn.jsdelivr.net/npm/vis-network@9.1.2/dist/vis-network.min.js|/lib/vis-network.min.js|g' {} \;

# 替换Highlight.js链接
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/atom-one-dark.min.css|/lib/atom-one-dark.min.css|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/highlight.min.js|/lib/highlight.min.js|g' {} \;
find src/main/resources/static -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/languages/javascript.min.js|/lib/javascript.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/atom-one-dark.min.css|/lib/atom-one-dark.min.css|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/highlight.min.js|/lib/highlight.min.js|g' {} \;
find src/main/resources/templates -name "*.html" -type f -exec sed -i '' 's|https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/languages/javascript.min.js|/lib/javascript.min.js|g' {} \;

echo "CDN链接替换完成！" 