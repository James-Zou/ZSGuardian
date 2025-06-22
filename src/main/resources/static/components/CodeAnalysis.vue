<template>
  <div class="code-analysis">
    <div class="code-input-container">
      <!-- 拖拽上传区域 -->
      <el-upload
        class="code-upload-drag"
        drag
        action=""
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        :multiple="false">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <div class="el-upload__tip" slot="tip">
          支持.java/.txt等文本文件
        </div>
      </el-upload>

      <!-- 代码展示区域 -->
      <div class="code-editor-wrapper">
        <div class="code-editor-header">
          <span class="file-info" v-if="fileName">
            当前文件: {{ fileName }}
          </span>
          <div class="editor-controls">
            <el-button 
              size="mini" 
              type="text" 
              @click="toggleFullScreen"
              :icon="isFullScreen ? 'el-icon-close' : 'el-icon-full-screen'">
              {{ isFullScreen ? '退出全屏' : '全屏' }}
            </el-button>
            <el-button 
              size="mini" 
              type="text" 
              @click="clearCode"
              v-if="code.length > 0">
              清空
            </el-button>
          </div>
        </div>
        
        <div :class="['code-editor-container', { 'full-screen': isFullScreen }]">
          <el-input
            ref="codeEditor"
            type="textarea"
            v-model="code"
            :rows="isFullScreen ? 40 : 20"
            :autosize="{ minRows: 10, maxRows: isFullScreen ? 100 : 30 }"
            class="code-editor"
            placeholder="请输入或拖入要分析的代码..."
            @input="handleCodeChange"
          />
          
          <div class="editor-footer">
            <span class="char-count">
              字符数: {{ code.length }} | 
              行数: {{ getLineCount() }}
            </span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button 
          type="primary" 
          @click="analyzeCode" 
          :loading="loading"
          :disabled="!code.trim()">
          开始分析
        </el-button>
      </div>
    </div>

    <!-- 分析结果展示 -->
    <div v-if="analysisResult" class="analysis-result">
      <!-- ... 保持原有的分析结果展示部分不变 ... -->
    </div>
  </div>
</template>

<script>
export default {
  name: 'CodeAnalysis',
  data() {
    return {
      code: '',
      fileName: '',
      loading: false,
      analysisResult: null,
      isFullScreen: false,
      maxFileSize: 10 * 1024 * 1024 // 10MB
    }
  },
  methods: {
    handleFileChange(file) {
      if (file.size > this.maxFileSize) {
        this.$message.error('文件大小不能超过10MB');
        return;
      }

      this.fileName = file.name;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.code = e.target.result;
        this.$nextTick(() => {
          this.adjustEditorHeight();
        });
      };
      reader.onerror = () => {
        this.$message.error('文件读取失败');
      };
      reader.readAsText(file.raw);
    },

    handleCodeChange() {
      this.$nextTick(() => {
        this.adjustEditorHeight();
      });
    },

    adjustEditorHeight() {
      const textarea = this.$refs.codeEditor.$el.querySelector('textarea');
      if (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = `${textarea.scrollHeight}px`;
      }
    },

    getLineCount() {
      return this.code.split('\n').length;
    },

    toggleFullScreen() {
      this.isFullScreen = !this.isFullScreen;
      this.$nextTick(() => {
        this.adjustEditorHeight();
      });
    },

    clearCode() {
      this.code = '';
      this.fileName = '';
      this.analysisResult = null;
    },

    async analyzeCode() {
      if (!this.code.trim()) {
        this.$message.warning('请输入要分析的代码');
        return;
      }

      this.loading = true;
      try {
        const response = await fetch('/api/analyze/code', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ code: this.code })
        });

        if (!response.ok) {
          throw new Error('分析请求失败');
        }

        this.analysisResult = await response.json();
        this.$message.success('分析完成');
      } catch (error) {
        this.$message.error('分析失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    }
  },
  
  mounted() {
    // 添加拖拽事件监听
    document.addEventListener('dragover', (e) => {
      e.preventDefault();
    });
    
    // 监听窗口大小变化
    window.addEventListener('resize', this.adjustEditorHeight);
  },
  
  beforeDestroy() {
    window.removeEventListener('resize', this.adjustEditorHeight);
  }
}
</script>

<style scoped>
.code-analysis {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.code-input-container {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.code-upload-drag {
  margin-bottom: 20px;
}

.code-editor-wrapper {
  position: relative;
  margin: 20px 0;
}

.code-editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #f8f9fa;
  border: 1px solid #dcdfe6;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
}

.file-info {
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.file-info i {
  margin-right: 5px;
}

.editor-controls {
  display: flex;
  gap: 10px;
}

.code-editor-container {
  position: relative;
  transition: all 0.3s;
  max-height: 600px;
  overflow: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.code-editor-container.full-screen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  max-height: none;
  height: 100vh;
  margin: 0;
  padding: 20px;
  background: #fff;
  border: none;
}

.code-editor {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
  font-size: 14px;
  width: 100%;
  height: 100%;
}

.code-editor /deep/ .el-textarea__inner {
  font-family: inherit;
  padding: 10px;
  line-height: 1.6;
  background-color: #f8f9fa;
  border: none;
  white-space: pre;
  overflow-wrap: normal;
  overflow-x: auto;
  min-height: 300px;
  height: auto !important;
  resize: none;
}

/* 容器滚动条样式 */
.code-editor-container::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.code-editor-container::-webkit-scrollbar-thumb {
  background: #909399;
  border-radius: 6px;
  border: 3px solid #f8f9fa;
}

.code-editor-container::-webkit-scrollbar-track {
  background: #f8f9fa;
  border-radius: 6px;
}

/* 水平滚动条样式 */
.code-editor-container::-webkit-scrollbar-thumb:horizontal {
  background: #909399;
  border-radius: 6px;
}

/* 全屏模式样式调整 */
.full-screen .code-editor /deep/ .el-textarea__inner {
  height: calc(100vh - 120px) !important;
  max-height: none !important;
  border: 1px solid #dcdfe6;
}

/* 底部信息栏样式 */
.editor-footer {
  position: sticky;
  bottom: 0;
  background: #f8f9fa;
  padding: 8px 16px;
  border-top: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  color: #909399;
  font-size: 12px;
}

/* 长行处理 */
.code-editor /deep/ .el-textarea__inner {
  overflow-x: auto;
  white-space: pre;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .code-editor-container {
    max-height: 400px;
  }
  
  .code-editor /deep/ .el-textarea__inner {
    min-height: 200px;
  }
}

.action-buttons {
  text-align: right;
  margin-top: 15px;
}

/* 分析结果样式保持不变 */
.analysis-result {
  margin-top: 20px;
}

.result-card {
  margin-bottom: 20px;
}

.test-suggestions {
  margin-bottom: 15px;
}

.test-suggestions h3 {
  margin-bottom: 10px;
  color: #409EFF;
}

.test-suggestions ul {
  padding-left: 20px;
}

.test-suggestions li {
  margin-bottom: 5px;
}

.el-alert {
  margin-bottom: 10px;
}
</style> 