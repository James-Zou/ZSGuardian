new Vue({
    el: '#app',
    data() {
        return {
            loginForm: {
                username: '',
                password: ''
            },
            rules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' }
                ]
            },
            loading: false
        }
    },
    methods: {
        handleLogin() {
            this.$refs.loginForm.validate(async (valid) => {
                if (!valid) return;
                
                this.loading = true;
                try {
                    const response = await fetch('/api/login', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(this.loginForm),
                        credentials: 'include'
                    });

                    const result = await response.json();
                    
                    if (result.success) {
                        this.$message({
                            type: 'success',
                            message: '登录成功',
                            duration: 1500,
                            onClose: () => {
                                window.location.replace('/index.html');
                            }
                        });
                    } else {
                        this.$message.error(result.message || '登录失败');
                    }
                } catch (error) {
                    console.error('登录错误:', error);
                    this.$message.error('登录失败：' + error.message);
                } finally {
                    this.loading = false;
                }
            });
        }
    }
}); 