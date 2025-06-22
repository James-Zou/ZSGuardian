// 动态加载公共头部
function loadCommonHeader() {
    fetch('/common/header.html')
        .then(response => response.text())
        .then(html => {
            document.head.insertAdjacentHTML('beforeend', html);
        });
}

// 页面加载时执行
document.addEventListener('DOMContentLoaded', loadCommonHeader);

// 添加登录检查函数
async function checkLogin() {
    try {
        const response = await fetch('/api/users/current');
        if (!response.ok) {
            window.location.href = '/login.html';
            return false;
        }
        return true;
    } catch (error) {
        console.error('检查登录状态失败:', error);
        window.location.href = '/login.html';
        return false;
    }
}

// 在页面加载完成后执行登录检查
document.addEventListener('DOMContentLoaded', checkLogin); 