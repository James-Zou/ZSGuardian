class TestRecorder {
    constructor() {
        this.recording = false;
        this.steps = [];
        this.currentElement = null;
    }
    
    startRecording() {
        this.recording = true;
        this.attachEventListeners();
    }
    
    stopRecording() {
        this.recording = false;
        this.removeEventListeners();
    }
    
    attachEventListeners() {
        document.addEventListener('click', this.handleClick.bind(this));
        document.addEventListener('input', this.handleInput.bind(this));
        document.addEventListener('change', this.handleChange.bind(this));
    }
    
    handleClick(event) {
        if (!this.recording) return;
        
        const element = event.target;
        const selector = this.generateSelector(element);
        
        this.steps.push({
            type: 'click',
            selector: selector,
            timestamp: new Date().getTime()
        });
    }
    
    generateSelector(element) {
        // 生成唯一的CSS选择器
        if (element.id) {
            return `#${element.id}`;
        }
        
        let path = [];
        while (element.parentElement) {
            let selector = element.tagName.toLowerCase();
            if (element.className) {
                selector += `.${element.className.split(' ').join('.')}`;
            }
            path.unshift(selector);
            element = element.parentElement;
        }
        
        return path.join(' > ');
    }
    
    enableElementSelector() {
        document.addEventListener('mouseover', this.highlightElement.bind(this));
        document.addEventListener('click', this.selectElement.bind(this), true);
    }
    
    highlightElement(event) {
        if (this.currentElement) {
            this.currentElement.style.outline = '';
        }
        
        this.currentElement = event.target;
        this.currentElement.style.outline = '2px solid #409EFF';
        event.preventDefault();
        event.stopPropagation();
    }
} 