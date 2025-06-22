// 设置全局默认值，防止模板渲染错误
window.currentMenu = 'dashboard';

// 确保在DOM加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, creating Vue instance...');
    
    // 创建正式的Vue实例
    const app = new Vue({
        el: '#app',
        data: {
            uploading: false,
            uploadProgress: 0,
            progressText: '',
            parseResult: null,
            aiCorrecting: false,
            aiCorrection: null,
            username: '',
            loggingOut: false,
            activeNames: ['1'],
            defaultProps: {
                children: 'children',
                label: 'label'
            },
            improvedXMindFile: null,
            currentText: '',
            isTyping: false,
            fullText: 'AI 赋能，新一代研发管理守护系统',
            uploadedFileName: '',
            loading: false,
            typingSpeed: 150,  // 打字速度
            pauseTime: 2000,   // 完成后暂停时间
            
            // 菜单和iframe管理 - 确保有默认值
            currentMenu: 'dashboard',
            iframeLoading: false,
            currentIframeSrc: '/dashboard.html', // 直接设置默认src
            sidebarCollapsed: false, // 侧边栏折叠状态
            
            // 菜单配置
            menuConfig: {
                dashboard: {
                    title: '系统概览',
                    src: '/dashboard.html'
                },
                code: {
                    title: '代码守护',
                    src: '/code-guardian.html'
                },
                test: {
                    title: '测试守护',
                    src: '/test-guardian.html'
                },
                review: {
                    title: '评审守护',
                    src: '/change-review.html'
                },
                ops: {
                    title: '运维守护',
                    src: '/ops-guardian.html'
                },
                testcase: {
                    title: '测试用例管理',
                    src: '/test-case-management.html'
                },
                data: {
                    title: '数据大屏',
                    src: '/data-screen.html'
                },
                user: {
                    title: '用户管理',
                    src: '/user-manage.html'
                },
                settings: {
                    title: '系统设置',
                    src: '/settings.html'
                }
            },
            
            // 四大模块统计数据
            codeStats: {
                issues: 0,
                passed: 0
            },
            testStats: {
                running: 0,
                completed: 0
            },
            reviewStats: {
                pending: 0,
                approved: 0
            },
            opsStats: {
                alerts: 0,
                healthy: 0
            },
            
            // 核心图表数据
            codeQualityScore: 85,
            testCoverage: 78,
            reviewEfficiency: 2.5,
            systemUptime: 99.8
        },
        computed: {
            impactAreasTree() {
                if (!this.parseResult) return [];
                
                const categories = this.parseResult.categories.testCasesByCategory;
                return this.parseResult.impactAreas.map(area => ({
                    id: area,
                    label: area,
                    count: categories[area] ? categories[area].length : 0,
                    children: categories[area] ? categories[area].map((testCase, index) => ({
                        id: `${area}-${index}`,
                        label: testCase
                    })) : []
                }));
            }
        },
        beforeCreate() {
            // 在Vue实例创建之前确保全局变量存在
            if (typeof window.currentMenu === 'undefined') {
                window.currentMenu = 'dashboard';
            }
        },
        created() {
            // 确保currentMenu有默认值
            this.currentMenu = this.currentMenu || 'dashboard';
            this.username = this.username || '';
            this.iframeLoading = this.iframeLoading || false;
            this.currentIframeSrc = this.currentIframeSrc || '/dashboard.html';
            this.loggingOut = this.loggingOut || false;
            this.sidebarCollapsed = this.sidebarCollapsed || false;
            
            // 恢复侧边栏折叠状态
            const savedCollapsed = localStorage.getItem('sidebarCollapsed');
            if (savedCollapsed !== null) {
                this.sidebarCollapsed = savedCollapsed === 'true';
            }
            
            this.username = this.getCookie('userName') || '';
            this.initStatistics();
            
            console.log('Vue created, sidebarCollapsed:', this.sidebarCollapsed);
        },
        methods: {
            handleUploadClick() {
                this.$refs.fileInput.click();
            },

            handleFileChange(event) {
                const files = event.target.files;
                if (files.length > 0) {
                    this.handleFile(files[0]);
                }
            },

            handleDrop(event) {
                event.preventDefault();
                const files = event.dataTransfer.files;
                if (files.length > 0) {
                    this.handleFile(files[0]);
                }
            },

            async handleFile(file) {
                if (!file.name.endsWith('.xmind')) {
                    this.$message.error('请上传 .xmind 文件');
                    return;
                }

                const chunkSize = 1024 * 1024; // 1MB chunks
                const totalChunks = Math.ceil(file.size / chunkSize);
                this.uploading = true;

                // 创建 loading 实例
                const loading = this.$loading({
                    lock: true,
                    text: '文件上传中...',
                    spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0.7)'
                });

                try {
                    for (let i = 0; i < totalChunks; i++) {
                        const start = i * chunkSize;
                        const end = Math.min(start + chunkSize, file.size);
                        const chunk = file.slice(start, end);

                        const formData = new FormData();
                        formData.append('file', chunk);
                        formData.append('chunkNumber', i);
                        formData.append('totalChunks', totalChunks);
                        formData.append('filename', file.name);

                        const response = await fetch('/api/analyze', {
                            method: 'POST',
                            body: formData,
                            credentials: 'include'
                        });

                        if (!response.ok) {
                            throw new Error(`分片 ${i + 1} 上传失败`);
                        }

                        // 如果是最后一片，获取解析结果
                        if (i === totalChunks - 1) {
                            const result = await response.json();
                            this.parseResult = result;
                            this.activeNames = this.parseResult.impactAreas.length > 0 ? [this.parseResult.impactAreas[0]] : [];
                            this.uploadedFileName = file.name;
                            loading.close(); // 关闭加载动画
                            this.$message.success('文件解析成功');
                        }
                    }
                } catch (error) {
                    console.error('文件处理错误:', error);
                    this.$message.error('文件处理失败：' + error.message);
                } finally {
                    this.uploading = false;
                    loading.close(); // 确保关闭 loading
                }
            },

            async requestAICorrection() {
                if (!this.parseResult) {
                    this.$message.warning('请先上传并解析文件');
                    return;
                }

                // 创建 loading 实例
                const loading = this.$loading({
                    lock: true,
                    text: 'AI 正在分析您的测试用例...',
                    spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0.7)'
                });

                try {
                    this.aiCorrecting = true;
                    const response = await fetch('/api/ai-correct', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(this.parseResult),
                        credentials: 'include'
                    });

                    const result = await response.json();
                    if (result.success) {
                        this.aiCorrection = result.aiCorrection;
                        this.improvedXMindFile = result.improvedXMindFile;
                        
                        this.$notify({
                            title: 'AI 分析完成',
                            message: '已生成优化建议和改进后的测试用例，请查看详细内容',
                            type: 'success',
                            duration: 5000,
                            position: 'top-right'
                        });
                        
                        this.$nextTick(() => {
                            const element = document.querySelector('.ai-correction-container');
                            if (element) {
                                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
                            }
                        });
                    } else {
                        throw new Error(result.message || 'AI 校正失败');
                    }
                } catch (error) {
                    console.error('AI 校正失败:', error);
                    this.$notify.error({
                        title: 'AI 校正失败',
                        message: error.message || '校正过程中发生错误',
                        duration: 5000
                    });
                } finally {
                    this.aiCorrecting = false;
                    loading.close(); // 确保关闭 loading
                }
            },

            async handleLogout() {
                if (this.loggingOut) return;
                
                try {
                    this.loggingOut = true;
                    const response = await fetch('/api/logout', {
                        method: 'POST',
                        credentials: 'include',
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    });

                    if (!response.ok) {
                        throw new Error('退出登录请求失败');
                    }

                    const result = await response.json();
                    if (result.success) {
                        // 清除本地存储
                        localStorage.clear();
                        sessionStorage.clear();
                        
                        // 清除所有 cookies
                        document.cookie.split(';').forEach(cookie => {
                            const eqPos = cookie.indexOf('=');
                            const name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;
                            document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';
                        });

                        this.$message({
                            type: 'success',
                            message: '退出登录成功',
                            duration: 1000,
                            onClose: () => {
                                // 使用 replace 并添加时间戳防止缓存
                                window.location.replace('/login.html?t=' + new Date().getTime());
                            }
                        });
                    } else {
                        throw new Error(result.message || '退出登录失败');
                    }
                } catch (error) {
                    console.error('退出登录失败:', error);
                    this.$message.error('退出登录失败：' + error.message);
                } finally {
                    this.loggingOut = false;
                }
            },

            redirectToLogin() {
                window.location.href = '/login.html';
            },

            formatAICorrection(correction) {
                if (!correction) return '';
                return correction.replace(/\n/g, '<br>')
                               .replace(/- /g, '• ')
                               .replace(/•([^<]+)/g, '<div style="margin-left: 20px">•$1</div>');
            },

            exportResult() {
                if (!this.parseResult) {
                    this.$message.warning('没有可导出的数据');
                    return;
                }

                const data = {
                    统计信息: {
                        总节点数: this.parseResult.totalTopics,
                        影响范围数: this.parseResult.impactAreas.length,
                        测试用例数: this.parseResult.testCases
                    },
                    影响范围: this.parseResult.impactAreas,
                    测试用例: this.parseResult.categories.testCasesByCategory
                };

                const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'analysis-result.json';
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
            },

            async downloadImprovedXMind() {
                if (!this.improvedXMindFile) {
                    return;
                }

                try {
                    const response = await fetch(`/api/download/${this.improvedXMindFile}`, {
                        method: 'GET',
                        credentials: 'include',
                    });

                    if (response.status === 401) {
                        this.$notify.error({
                            title: '未登录',
                            message: '请先登录后再下载文件',
                            duration: 3000
                        });
                        // 可以添加跳转到登录页的逻辑
                        return;
                    }

                    if (!response.ok) {
                        const error = await response.json();
                        throw new Error(error.message || '下载失败');
                    }

                    // 获取文件名
                    const contentDisposition = response.headers.get('content-disposition');
                    let fileName = this.improvedXMindFile;
                    if (contentDisposition) {
                        const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
                        if (matches != null && matches[1]) {
                            fileName = matches[1].replace(/['"]/g, '');
                        }
                    }

                    // 下载文件
                    const blob = await response.blob();
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = fileName;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);

                    this.$notify.success({
                        title: '下载成功',
                        message: '文件已开始下载',
                        duration: 3000
                    });

                } catch (error) {
                    console.error('下载文件失败:', error);
                    this.$notify.error({
                        title: '下载失败',
                        message: error.message || '下载文件时发生错误',
                        duration: 3000
                    });
                }
            },

            async handleFileUpload(options) {
                try {
                    this.loading = true;
                    await this.handleFile(options.file);
                } catch (error) {
                    console.error('文件上传失败:', error);
                    this.$message.error('文件上传失败：' + error.message);
                } finally {
                    this.loading = false;
                }
            },

            resetUpload() {
                this.uploadedFileName = '';
                this.parseResult = null;
                this.aiCorrection = null;
                this.improvedXMindFile = null;
                this.$message.info('已清除当前文件，请重新上传');
            },

            async typeText() {
                while (true) {  // 持续循环播放
                    // 开始新一轮打字
                    this.isTyping = true;
                    this.currentText = '';

                    // 打字效果
                    for (let i = 0; i < this.fullText.length; i++) {
                        this.currentText += this.fullText[i];
                        // 随机延迟，使打字效果更自然
                        await new Promise(resolve => setTimeout(resolve, 
                            this.typingSpeed + Math.random() * 50
                        ));
                    }

                    // 完整显示后暂停
                    await new Promise(resolve => setTimeout(resolve, this.pauseTime));

                    // 渐隐效果
                    this.isTyping = false;
                    
                    // 等待渐隐完成
                    await new Promise(resolve => setTimeout(resolve, 500));
                }
            },
            openCodeGuardian() {
                window.open('/code-guardian.html', '_blank');
            },
            openTestGuardian() {
                window.open('/test-guardian.html', '_blank');
            },
            openReviewGuardian() {
                window.location.href = '/change-review.html';
            },
            openOpsGuardian() {
                window.location.href = '/ops-guardian.html';
            },
            getCookie(name) {
                const value = `; ${document.cookie}`;
                const parts = value.split(`; ${name}=`);
                if (parts.length === 2) return parts.pop().split(';').shift();
                return '';
            },
            async exportTestCaseToXMind(testCase) {
                try {
                    const response = await axios.post('/api/test-cases/export', {
                        testCase: testCase
                    }, {
                        responseType: 'blob'
                    });
                    
                    // 创建下载链接
                    const url = window.URL.createObjectURL(new Blob([response.data]));
                    const link = document.createElement('a');
                    link.href = url;
                    link.setAttribute('download', `test-case-${testCase.id}.xmind`);
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);
                    
                    this.$message.success('测试用例导出成功');
                } catch (error) {
                    console.error('导出测试用例失败:', error);
                    this.$message.error('导出测试用例失败');
                }
            },
            initStatistics() {
                // 模拟从后端获取数据
                this.codeStats = {
                    issues: 10,
                    passed: 8
                };
                this.testStats = {
                    running: 2,
                    completed: 5
                };
                this.reviewStats = {
                    pending: 3,
                    approved: 4
                };
                this.opsStats = {
                    alerts: 1,
                    healthy: 6
                };
                this.codeQualityScore = 88;
                this.testCoverage = 80;
                this.reviewEfficiency = 2.8;
                this.systemUptime = 99.9;
            },
            startStatisticsUpdate() {
                // 每30秒更新一次统计数据
                this.statisticsTimer = setInterval(() => {
                    this.updateStatistics();
                }, 30000);
            },
            updateStatistics() {
                // 模拟数据更新，实际项目中应该从后端API获取
                this.codeStats.issues = Math.floor(Math.random() * 15) + 5;
                this.codeStats.passed = Math.floor(Math.random() * 10) + 5;
                this.testStats.running = Math.floor(Math.random() * 5) + 1;
                this.testStats.completed = Math.floor(Math.random() * 8) + 3;
                this.reviewStats.pending = Math.floor(Math.random() * 6) + 1;
                this.reviewStats.approved = Math.floor(Math.random() * 8) + 2;
                this.opsStats.alerts = Math.floor(Math.random() * 3) + 0;
                this.opsStats.healthy = Math.floor(Math.random() * 10) + 5;
                
                // 更新图表数据
                this.codeQualityScore = Math.floor(Math.random() * 20) + 80;
                this.testCoverage = Math.floor(Math.random() * 25) + 70;
                this.reviewEfficiency = (Math.random() * 2 + 1.5).toFixed(1);
                this.systemUptime = (Math.random() * 0.4 + 99.6).toFixed(1);
            },
            
            // 菜单切换方法
            switchMenu(menuKey) {
                if (this.currentMenu === menuKey) return;
                
                this.currentMenu = menuKey;
                this.iframeLoading = true;
                
                const menuItem = this.menuConfig[menuKey];
                if (menuItem) {
                    this.currentIframeSrc = menuItem.src;
                }
            },
            
            // 获取菜单标题
            getMenuTitle() {
                const menuItem = this.menuConfig[this.currentMenu];
                return menuItem ? menuItem.title : '系统概览';
            },
            
            // iframe加载完成
            onIframeLoad() {
                this.iframeLoading = false;
            },
            
            // iframe加载错误
            onIframeError() {
                this.iframeLoading = false;
                this.$message.error('页面加载失败，请检查网络连接');
            },
            
            // 初始化默认页面
            initDefaultPage() {
                // 检查URL参数，如果有指定页面则跳转
                const urlParams = new URLSearchParams(window.location.search);
                const page = urlParams.get('page');
                
                if (page && this.menuConfig[page]) {
                    this.switchMenu(page);
                } else {
                    // 默认显示系统概览
                    this.switchMenu('dashboard');
                }
            },
            
            // 检查登录状态
            checkLoginStatus() {
                // 从localStorage获取用户信息
                const userInfo = localStorage.getItem('userInfo');
                if (userInfo) {
                    try {
                        const user = JSON.parse(userInfo);
                        this.username = user.username || user.name || '用户';
                    } catch (e) {
                        console.error('解析用户信息失败:', e);
                        this.username = '用户';
                    }
                }
            },
            
            // 启动打字机效果
            startTyping() {
                this.typeText();
            },
            
            // 监听iframe内的消息
            handleIframeMessage(event) {
                if (event.data && event.data.type === 'switchMenu') {
                    this.switchMenu(event.data.menu);
                }
            },
            
            // 切换侧边栏折叠状态
            toggleSidebar() {
                console.log('=== Vue toggleSidebar method called ===');
                console.log('toggleSidebar called, current state:', this.sidebarCollapsed);
                this.sidebarCollapsed = !this.sidebarCollapsed;
                console.log('new state:', this.sidebarCollapsed);
                
                // 检查DOM元素
                const sidebar = document.querySelector('.sidebar');
                if (sidebar) {
                    console.log('Sidebar element found, classes:', sidebar.className);
                    console.log('Data collapsed:', sidebar.getAttribute('data-collapsed'));
                    
                    // 强制更新DOM
                    if (this.sidebarCollapsed) {
                        sidebar.style.width = '80px';
                        sidebar.classList.add('collapsed');
                    } else {
                        sidebar.style.width = '280px';
                        sidebar.classList.remove('collapsed');
                    }
                    console.log('DOM updated, new width:', sidebar.style.width);
                } else {
                    console.log('Sidebar element not found');
                }
                
                // 保存到localStorage
                localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed);
            }
        },
        mounted() {
            // 检查登录状态
            this.checkLoginStatus();
            
            // 启动打字机效果
            this.startTyping();
            
            // 启动定时更新统计数据
            this.startStatisticsUpdate();
            this.initDefaultPage();
            
            // 监听iframe内的消息
            window.addEventListener('message', this.handleIframeMessage);
            
            // 在mounted钩子中覆盖全局函数，确保Vue实例完全初始化
            window.switchMenu = this.switchMenu.bind(this);
            window.handleLogout = this.handleLogout.bind(this);
            window.onIframeLoad = this.onIframeLoad.bind(this);
            window.onIframeError = this.onIframeError.bind(this);
            window.getMenuTitle = this.getMenuTitle.bind(this);
            window.toggleSidebar = this.toggleSidebar.bind(this);
            
            console.log('Vue mounted, methods overridden in mounted hook');
        },
        
        beforeDestroy() {
            // 清理定时器
            if (this.statisticsTimer) {
                clearInterval(this.statisticsTimer);
            }
            
            // 清理消息监听器
            window.removeEventListener('message', this.handleIframeMessage);
        }
    });
    
    console.log('Vue instance created successfully');
    console.log('Vue instance:', app);
    console.log('toggleSidebar method:', app.toggleSidebar);
    
    // Vue实例创建完成后立即覆盖全局函数
    if (app && app.toggleSidebar) {
        window.switchMenu = app.switchMenu.bind(app);
        window.handleLogout = app.handleLogout.bind(app);
        window.onIframeLoad = app.onIframeLoad.bind(app);
        window.onIframeError = app.onIframeError.bind(app);
        window.getMenuTitle = app.getMenuTitle.bind(app);
        window.toggleSidebar = app.toggleSidebar.bind(app);
        
        // 将Vue实例保存到全局变量
        window.app = app;
        
        console.log('Vue instance created, methods overridden');
        console.log('app.toggleSidebar available:', typeof app.toggleSidebar);
        console.log('window.toggleSidebar available:', typeof window.toggleSidebar);
    } else {
        console.error('Vue app or toggleSidebar method not available');
        console.log('app:', app);
        console.log('app.toggleSidebar:', app ? app.toggleSidebar : 'app is null');
    }
}); 