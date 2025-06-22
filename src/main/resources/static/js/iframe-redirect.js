/**
 * iframe重定向脚本
 * 当页面被直接访问时，自动跳转到主框架并切换到对应模块
 */

// 页面映射配置
const PAGE_MAPPING = {
    'dashboard.html': 'dashboard',
    'code-guardian.html': 'code',
    'test-guardian.html': 'test',
    'change-review.html': 'review',
    'ops-guardian.html': 'ops',
    'test-case-management.html': 'testcase',
    'data-screen.html': 'data',
    'user-manage.html': 'user',
    'settings.html': 'settings',
    'test-execution.html': 'test',
    'code-audit-list.html': 'code'
};

/**
 * 检查是否在iframe中
 */
function isInIframe() {
    try {
        return window !== window.top;
    } catch (e) {
        return false;
    }
}

/**
 * 检查是否直接访问此页面
 */
function shouldRedirect() {
    return !isInIframe();
}

/**
 * 获取当前页面名称
 */
function getCurrentPageName() {
    const pathname = window.location.pathname;
    const filename = pathname.split('/').pop();
    return filename;
}

/**
 * 执行重定向
 */
function performRedirect() {
    const currentPage = getCurrentPageName();
    const targetMenu = PAGE_MAPPING[currentPage];
    
    if (targetMenu) {
        // 跳转到主框架并指定页面
        window.location.href = `/index.html?page=${targetMenu}`;
    } else {
        // 如果没有找到映射，跳转到默认页面
        window.location.href = '/index.html';
    }
}

/**
 * 初始化重定向检查
 */
function initRedirect() {
    // 排除登录和注册页面
    const currentPage = getCurrentPageName();
    if (currentPage === 'login.html' || currentPage === 'register.html' || currentPage === 'index.html') {
        return;
    }
    
    if (shouldRedirect()) {
        performRedirect();
    }
}

// 页面加载完成后立即执行
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initRedirect);
} else {
    initRedirect();
} 