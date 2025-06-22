// 请求拦截器
axios.interceptors.request.use(
    config => {
        // 从cookie中获取用户信息
        const userId = getCookie('userId');
        const userName = getCookie('userName');
        
        if (userId && userName) {
            config.headers['userId'] = userId;
            config.headers['userName'] = userName;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
axios.interceptors.response.use(
    response => {
        return response;
    },
    error => {
        if (error.response && error.response.status === 401) {
            // 未登录或登录过期，跳转到登录页
            window.location.href = '/login.html';
        }
        return Promise.reject(error);
    }
);

// 获取cookie的辅助函数
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
} 