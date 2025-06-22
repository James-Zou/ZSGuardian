class HeartAndArrow {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.resize();
        this.init();
    }

    resize() {
        this.canvas.width = Math.min(window.innerWidth, 800);
        this.canvas.height = Math.min(window.innerHeight, 600);
    }

    init() {
        this.size = Math.min(this.canvas.width, this.canvas.height) * 0.3;
        this.x = this.canvas.width / 2;
        this.y = this.canvas.height / 2;
        
        // 箭的初始位置（从左侧开始）
        this.arrowX = -this.size;
        this.arrowComplete = false;
        
        // 心形初始大小
        this.heartScale = 0;
        
        // 动画状态
        this.state = 'shooting'; // shooting, growing, beating
    }

    drawHeart(scale = 1) {
        this.ctx.save();
        this.ctx.translate(this.x, this.y);
        this.ctx.scale(scale, scale);

        this.ctx.fillStyle = '#ff0066';
        this.ctx.beginPath();
        this.ctx.moveTo(0, -this.size/2);
        
        // 绘制心形
        for(let i = 0; i < Math.PI * 2; i += 0.01) {
            const x = this.size * Math.sin(i) * Math.sin(i) * Math.sin(i);
            const y = -this.size * (Math.cos(i) - 0.5 * Math.cos(2*i) - 0.15 * Math.cos(3*i));
            this.ctx.lineTo(x, y);
        }

        this.ctx.fill();
        this.ctx.restore();
    }

    drawArrow(x) {
        this.ctx.save();
        this.ctx.translate(x, this.y);
        
        // 箭身
        this.ctx.beginPath();
        this.ctx.strokeStyle = '#4a4a4a';
        this.ctx.lineWidth = 3;
        this.ctx.moveTo(-this.size/2, 0);
        this.ctx.lineTo(this.size/2, 0);
        this.ctx.stroke();

        // 箭头
        this.ctx.beginPath();
        this.ctx.fillStyle = '#4a4a4a';
        this.ctx.moveTo(this.size/2, 0);
        this.ctx.lineTo(this.size/2 - 20, -10);
        this.ctx.lineTo(this.size/2 - 20, 10);
        this.ctx.closePath();
        this.ctx.fill();

        // 箭尾
        this.ctx.beginPath();
        this.ctx.moveTo(-this.size/2, 0);
        this.ctx.lineTo(-this.size/2 + 20, -10);
        this.ctx.lineTo(-this.size/2 + 20, 10);
        this.ctx.closePath();
        this.ctx.fill();

        this.ctx.restore();
    }

    animate() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        if (this.state === 'shooting') {
            // 箭的移动
            this.arrowX += 15;
            this.drawArrow(this.arrowX);
            
            // 当箭到达中心位置
            if (this.arrowX >= this.x) {
                this.state = 'growing';
            }
        }
        
        if (this.state === 'growing') {
            // 绘制箭
            this.drawArrow(this.x);
            
            // 心形逐渐长大
            this.heartScale += 0.05;
            this.drawHeart(this.heartScale);
            
            if (this.heartScale >= 1) {
                this.state = 'beating';
                this.beatScale = 1;
            }
        }
        
        if (this.state === 'beating') {
            // 绘制箭
            this.drawArrow(this.x);
            
            // 心跳动画
            this.beatScale = 1 + Math.sin(Date.now() / 200) * 0.1;
            this.drawHeart(this.beatScale);
        }

        requestAnimationFrame(() => this.animate());
    }
}

// 初始化
window.addEventListener('DOMContentLoaded', () => {
    const canvas = document.getElementById('heartCanvas');
    const heartAndArrow = new HeartAndArrow(canvas);
    heartAndArrow.animate();

    // 响应式处理
    window.addEventListener('resize', () => {
        heartAndArrow.resize();
        heartAndArrow.init();
    });
}); 