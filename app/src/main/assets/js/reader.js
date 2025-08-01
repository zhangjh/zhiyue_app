let i18n = {};

let book;
let rendition;
let isSelecting = false;
let tocGenerated = false;

const annotationMenu = document.getElementById('annotation-menu');
const selectionMenu = document.getElementById('selection-menu');
let currentCfiRange = null;
let currentContents = null;
let currentAnnotationElement = null;
let startX, startY;
let touchStartTime = 0;
let longPressTimer = null;
const SWIPE_THRESHOLD = 50;
const TAP_THRESHOLD = 200;
const LONG_PRESS_THRESHOLD = 500;
let isDarkMode = false;

// 在加载书籍时保存标注数据的数组
let annotations = [];
// 添加当前页面引用，用于切换时重新标注
let currentLocation = null;

// 应用翻译到HTML元素
function applyTranslations() {
    if (!i18n || !i18n.reader) return;

    // 更新标题
    document.title = i18n.reader.title;

    // 更新目录按钮和标题
    document.getElementById('toc-button').textContent = i18n.reader.toc;
    document.querySelector('#toc-header h3').textContent = i18n.reader.toc;
    document.querySelector('#toc-header .close').textContent = i18n.reader.close;
    document.querySelector('#toc-content').textContent = i18n.reader.loading_toc;

    // 更新菜单按钮
    document.getElementById('copy-btn').textContent = i18n.reader.copy;
    document.getElementById('highlight-btn').textContent = i18n.reader.highlight;
    document.getElementById('underline-btn').textContent = i18n.reader.underline;
    document.getElementById('delete-annotation-btn').textContent = i18n.reader.delete;

    document.getElementById('progress-container').textContent = i18n.reader.progress_loading;
}

// 更新进度文本
function updateProgressText(percent) {
    if (i18n && i18n.reader) {
        const progressText = i18n.reader.progress.replace('%s', percent);
        document.getElementById('progress-container').textContent = progressText;
    }
}

window.setLanguageResource = function(langRes) {
    // console.log("setLangRes: ");
    // console.log(langRes);
    // 应用翻译
    if(langRes) {
        i18n = JSON.parse(langRes);
        // console.log(i18n);
        applyTranslations();
    }
}

// 设置主题模式
window.setThemeMode = function(darkMode) {
    isDarkMode = darkMode;
    const themeCss = document.getElementById('theme-css');
    if (isDarkMode) {
        themeCss.href = 'file:///android_asset/css/dark.css';
    } else {
        themeCss.href = 'file:///android_asset/css/light.css';
    }

    // CSS加载完成后显示页面
    themeCss.onload = function() {
        document.body.style.visibility = 'visible';
    };

    // 通知Android主题已变更
    if (window.Android) {
        window.Android.onThemeChanged(isDarkMode ? 'dark' : 'light');
    }

    // 如果书籍已经加载，需要重新应用样式到iframe内容
    if (rendition && rendition.manager && rendition.manager.views) {
        rendition.manager.views.forEach(view => {
            if (view.contents) {
                applyThemeToContent(view.contents);
            }
        });
    }
}

// 应用主题到内容
function applyThemeToContent(contents) {
    if (!contents || !contents.document) return;

    const doc = contents.document;
    const body = doc.body;
    if (!body) return;

    // 移除之前的主题样式
    let existingStyle = doc.getElementById('reader-theme-style');
    if (existingStyle) {
        existingStyle.remove();
    }

    // 创建新的样式元素
    const style = doc.createElement('style');
    style.id = 'reader-theme-style';

    if (isDarkMode) {
        style.textContent = `
            body, html {
                background-color: #121212 !important;
                color: #E6E6E6 !important;
            }
            * {
                color: #E6E6E6 !important;
                background-color: transparent !important;
            }
            a {
                color: #82B1FF !important;
            }
            h1, h2, h3, h4, h5, h6 {
                color: #FFFFFF !important;
            }
        `;
    } else {
        style.textContent = `
            body, html {
                background-color: #FFFFFF !important;
                color: #333333 !important;
            }
            * {
                color: #333333 !important;
                background-color: transparent !important;
            }
            a {
                color: #2196F3 !important;
            }
            h1, h2, h3, h4, h5, h6 {
                color: #000000 !important;
            }
        `;
    }

    doc.head.appendChild(style);
}

function clearAllAnnotations() {
    annotations.forEach(annotation => {
        rendition.annotations.remove(annotation.cfi, annotation.type);
    });
}

window.renderAnnotations = function(annotations) {
    if(!annotations || annotations.length <= 0) {
        return;
    }
    clearAllAnnotations();
    // console.log("reRender annotations");
    // 先删除再重新渲染
    annotations.forEach(annotation => {
        if (annotation.type === 'highlight') {
            rendition.annotations.highlight(
                annotation.cfi,
                {},
                (e) => {
                    e.target.style.backgroundColor = annotation.color || 'rgba(255,255,0,0.3)';
                    // 添加点击事件用于删除
                    e.target.classList.add('annotation-highlight');
                    e.target.setAttribute('data-type', annotation.type);
                    e.target.setAttribute('data-cfi', annotation.cfi);
                    e.target.setAttribute('data-text', annotation.text);
                    e.target.addEventListener('click', handleAnnotationClick);
                    // console.log("add highlight event：" + annotation.text);
                }
            );
        } else if (annotation.type === 'underline') {
            rendition.annotations.underline(
                annotation.cfi,
                {},
                (e) => {
                    // 添加点击事件用于删除
                    e.target.classList.add('annotation-underline');
                    e.target.setAttribute('data-type', annotation.type);
                    e.target.setAttribute('data-cfi', annotation.cfi);
                    e.target.setAttribute('data-text', annotation.text);
                    e.target.addEventListener('click', handleAnnotationClick);
                    // console.log("add underline event：" + annotation.text);
                },
                "ul",
                {
                    "border": "none",
                    "stroke": annotation.color || "#3366ff",
                    "stroke-width": "2px"
                }
            );
        }
    });
}

// 添加加载已有标注时添加点击事件
window.loadAnnotations = function(annotationsJson) {
    annotations = JSON.parse(annotationsJson);
    // console.log("annotations init:" + annotationsJson);
    window.renderAnnotations(annotations);
};

// 操作处理函数
function handleCopy() {
    const selection = currentContents.document.getSelection();
    if (selection && window.Android) {
        window.Android.copyText(selection.toString());
    }
    selectionMenu.style.display = 'none';
}

async function handleHighlight() {
    if (currentCfiRange) {
        const selection = currentContents.document.getSelection();
        const text = selection.toString();
        annotations.push({
            type: "highlight",
            cfi: currentCfiRange,
            text,
            className: "highlight"
        });
        await rendition.annotations.highlight(currentCfiRange, {}, (e) => {
            e.target.style.backgroundColor = 'rgba(255,255,0,0.3)';
            // 添加点击事件用于删除
            e.target.classList.add('annotation-highlight');
            e.target.setAttribute('data-type', "highlight");
            e.target.setAttribute('data-text', text);
            e.target.setAttribute('data-cfi', currentCfiRange);
            e.target.addEventListener('click', handleAnnotationClick);
        });
        // 保存高亮
        if (window.Android) {
            window.Android.saveAnnotation(
                currentCfiRange,
                'highlight',
                'rgba(255,255,0,0.3)',
                text
            );
        }
        selectionMenu.style.display = 'none';
    }
}

async function handleUnderline() {
    if (currentCfiRange) {
        const selection = currentContents.document.getSelection();
        const text = selection.toString();
        annotations.push({
            type: "underline",
            cfi: currentCfiRange,
            text,
            className: "underline"
        });
        rendition.annotations.underline(currentCfiRange,
            {},
            undefined,
            "ul",
            {
                "border": "none",
                "stroke": "#3366ff",
                "stroke-width": "2px"
            },
            (e) => {
                // 添加点击事件用于删除
                e.target.classList.add('annotation-underline');
                e.target.setAttribute('data-type', "underline");
                e.target.setAttribute('data-text', text);
                e.target.setAttribute('data-cfi', currentCfiRange);
                e.target.addEventListener('click', handleAnnotationClick);
            }
        );
        // 保存下划线
        if (window.Android) {
            window.Android.saveAnnotation(
                currentCfiRange,
                'underline',
                '#3366ff',
                text
            );
        }
        selectionMenu.style.display = 'none';
    }
}

// 添加删除标注处理函数
function handleDeleteAnnotation() {
    if (currentAnnotationElement) {
        const type = currentAnnotationElement.getAttribute('data-type');
        const cfi = currentAnnotationElement.getAttribute('data-cfi');
        const text = currentAnnotationElement.getAttribute('data-text');
        // console.log("删除标注: cfi:" + cfi + "text:" + text);
        if (cfi && text) {
            // 删除当前
            rendition.annotations.remove(cfi, type);
            // 把要删除的标注从当前缓存里删除(以便重新渲染)
            annotations = annotations.filter(annotation => annotation.text !== text);
            // 通知Android删除标注
            if (window.Android) {
                window.Android.deleteAnnotation(cfi, text);
            }
            // 删除标注后重新渲染标注
            window.renderAnnotations(annotations);
        }
        // 隐藏菜单
        annotationMenu.style.display = 'none';
        currentAnnotationElement = null;
    }
}

// 添加标注点击处理函数
function handleAnnotationClick(event) {
    event.preventDefault();
    event.stopPropagation();

    // 保存当前点击的标注元素
    currentAnnotationElement = event.target;
    const cfi = currentAnnotationElement.getAttribute('data-cfi');
    const text = currentAnnotationElement.getAttribute('data-text');
    if (!cfi || !text) return;

    // 获取选中文本的位置
    const rect = event.target.getBoundingClientRect();
    const iframe = document.querySelector('#viewer iframe');
    const iframeRect = iframe.getBoundingClientRect();

    // 计算菜单位置
    const menuWidth = annotationMenu.offsetWidth;
    const menuHeight = annotationMenu.offsetHeight;

    // 修改计算方式，使其与划线标注时的计算方式更一致
    let menuLeft = iframeRect.left + rect.left + (rect.width / 2) - (menuWidth / 2) - 20;
    let menuTop = iframeRect.top + rect.top + rect.height - 30;

    // 确保菜单在视口内
    menuLeft = Math.max(10, Math.min(menuLeft, window.innerWidth - menuWidth - 10));
    if (menuTop + menuHeight > window.innerHeight) {
        menuTop = iframeRect.top + rect.top - menuHeight - 30;
    }

    // 显示菜单
    annotationMenu.style.left = `${menuLeft}px`;
    annotationMenu.style.top = `${menuTop}px`;
    annotationMenu.style.display = 'flex';
    annotationMenu.style.justifyContent = 'center';

    // 隐藏选择菜单（如果有）
    selectionMenu.style.display = 'none';
}

function initializeSelection() {
    // 添加操作按钮事件监听
    document.body.addEventListener('click', function(e) {
        if (e.target.id === 'copy-btn') handleCopy();
        if (e.target.id === 'highlight-btn') handleHighlight();
        if (e.target.id === 'underline-btn') handleUnderline();
        if (e.target.id === 'delete-annotation-btn') handleDeleteAnnotation();
    });

    // 添加目录按钮点击事件
    document.getElementById('toc-button').addEventListener('click', toggleToc);

    rendition.hooks.content.register((contents) => {
        currentContents = contents;
        const doc = contents.document;

        doc.documentElement.style.webkitUserSelect = 'text';
        doc.documentElement.style.userSelect = 'text';

        // 应用主题样式
        applyThemeToContent(contents);

        // 统一的触摸事件处理 - 更加稳健的版本
        let touchStartX, touchStartY;
        let touchStartTime = 0;
        let longPressTimer = null;
        let hasMoved = false;
        let touchIntention = 'unknown'; // 'tap', 'swipe', 'longpress', 'selecting'
        let swipeStarted = false;
        
        const MOVE_THRESHOLD = 15; // 增加移动阈值，减少误触
        const SWIPE_THRESHOLD = 60; // 增加滑动阈值，确保是明确的滑动意图
        const LONG_PRESS_THRESHOLD = 500; // 长按阈值
        const MIN_SWIPE_VELOCITY = 0.3; // 最小滑动速度 (像素/毫秒)

        doc.addEventListener('touchstart', (e) => {
            const touch = e.touches[0];
            touchStartX = touch.clientX;
            touchStartY = touch.clientY;
            touchStartTime = Date.now();
            hasMoved = false;
            touchIntention = 'unknown';
            swipeStarted = false;
            
            // 清除之前的选择状态（如果不是在扩展选择）
            const selection = doc.getSelection();
            const hasExistingSelection = selection && selection.toString().trim().length > 0;
            if (!hasExistingSelection) {
                isSelecting = false;
            }

            // 设置长按定时器
            longPressTimer = setTimeout(() => {
                if (!hasMoved && touchIntention === 'unknown') {
                    touchIntention = 'longpress';
                    isSelecting = true;
                    // 启用文本选择
                    const range = doc.caretRangeFromPoint(touchStartX, touchStartY);
                    if (range) {
                        const selection = doc.getSelection();
                        selection.removeAllRanges();
                        selection.addRange(range);
                        // 添加触觉反馈（如果支持）
                        if (navigator.vibrate) {
                            navigator.vibrate(50);
                        }
                    }
                }
            }, LONG_PRESS_THRESHOLD);
        }, { passive: true });

        doc.addEventListener('touchmove', (e) => {
            const touch = e.touches[0];
            const moveX = Math.abs(touch.clientX - touchStartX);
            const moveY = Math.abs(touch.clientY - touchStartY);
            const totalMove = Math.sqrt(moveX * moveX + moveY * moveY);
            
            // 检查是否有明显移动
            if (totalMove > MOVE_THRESHOLD) {
                hasMoved = true;
                
                // 根据移动方向和距离判断意图
                if (touchIntention === 'unknown') {
                    if (moveX > moveY && moveX > MOVE_THRESHOLD) {
                        // 水平移动更明显，可能是滑动翻页
                        touchIntention = 'swipe';
                        clearTimeout(longPressTimer);
                    } else if (moveY > moveX) {
                        // 垂直移动，不是翻页
                        touchIntention = 'scroll';
                        clearTimeout(longPressTimer);
                    }
                }
                
                // 如果已经开始滑动且距离足够，标记为滑动开始
                if (touchIntention === 'swipe' && moveX > SWIPE_THRESHOLD) {
                    swipeStarted = true;
                }
            }

            // 如果正在选择文本，允许扩展选择范围
            if (isSelecting && (touchIntention === 'longpress' || touchIntention === 'selecting')) {
                touchIntention = 'selecting';
                const range = doc.caretRangeFromPoint(touch.clientX, touch.clientY);
                const selection = doc.getSelection();

                if (range && selection.rangeCount > 0) {
                    selection.extend(range.startContainer, range.startOffset);
                }
            }
        }, { passive: true });

        doc.addEventListener('touchend', (e) => {
            clearTimeout(longPressTimer);
            
            const touch = e.changedTouches[0];
            const touchEndTime = Date.now();
            const touchDuration = touchEndTime - touchStartTime;
            const swipeDistance = touchStartX - touch.clientX;
            const swipeVelocity = Math.abs(swipeDistance) / touchDuration;
            
            // 检查当前是否有选中的文本
            const selection = doc.getSelection();
            const hasSelectedText = selection && selection.toString().trim().length > 0;
            
            // 更严格的翻页条件判断
            const shouldTurnPage = (
                touchIntention === 'swipe' && // 明确的滑动意图
                swipeStarted && // 滑动已经开始
                !isSelecting && // 不在选择模式
                !hasSelectedText && // 没有选中文本
                Math.abs(swipeDistance) > SWIPE_THRESHOLD && // 滑动距离足够
                touchDuration < LONG_PRESS_THRESHOLD && // 不是长按
                Math.abs(touch.clientY - touchStartY) < SWIPE_THRESHOLD && // 垂直移动不大
                swipeVelocity > MIN_SWIPE_VELOCITY // 滑动速度足够
            );
            
            if (shouldTurnPage) {
                e.preventDefault();
                e.stopPropagation();
                
                // 添加防抖，避免连续快速翻页
                if (!window.pageturning) {
                    window.pageturning = true;
                    setTimeout(() => {
                        window.pageturning = false;
                    }, 300);
                    
                    // 调试信息（发布时可移除）
                    console.log('Page turn triggered:', {
                        swipeDistance: swipeDistance,
                        touchDuration: touchDuration,
                        swipeVelocity: swipeVelocity.toFixed(3),
                        touchIntention: touchIntention
                    });
                    
                    if (swipeDistance > 0) {
                        rendition.next();
                    } else {
                        rendition.prev();
                    }
                }
            } else if (Math.abs(swipeDistance) > MOVE_THRESHOLD) {
                // 调试信息：记录未触发翻页的原因
                console.log('Page turn blocked:', {
                    touchIntention: touchIntention,
                    swipeStarted: swipeStarted,
                    isSelecting: isSelecting,
                    hasSelectedText: hasSelectedText,
                    swipeDistance: Math.abs(swipeDistance),
                    touchDuration: touchDuration,
                    swipeVelocity: swipeVelocity.toFixed(3),
                    verticalMove: Math.abs(touch.clientY - touchStartY)
                });
            }
            
            // 重置状态
            setTimeout(() => {
                if (!hasSelectedText) {
                    isSelecting = false;
                }
                touchIntention = 'unknown';
                swipeStarted = false;
            }, 100);
        }, { passive: false }); // 注意这里改为非passive，以便可以preventDefault

        // 修改选择变化监听
        doc.addEventListener('selectionchange', () => {
            const selection = doc.getSelection();
            const selectedText = selection.toString().trim();

            if (selectedText && isSelecting) {
                const range = selection.getRangeAt(0);
                currentCfiRange = contents.cfiFromRange(range);

                // 获取选中文本的位置
                const rect = range.getBoundingClientRect();
                const iframe = document.querySelector('#viewer iframe');
                const iframeRect = iframe.getBoundingClientRect();

                // 计算菜单位置
                const menuWidth = selectionMenu.offsetWidth;
                const menuHeight = selectionMenu.offsetHeight;

                let menuLeft = iframeRect.left + rect.left + (rect.width / 2) - (menuWidth / 2);
                let menuTop = iframeRect.top + rect.bottom + 10;

                // 确保菜单在视口内
                menuLeft = Math.max(10, Math.min(menuLeft, window.innerWidth - menuWidth - 10));
                if (menuTop + menuHeight > window.innerHeight) {
                    menuTop = iframeRect.top + rect.top - menuHeight - 10;
                }

                // 显示菜单
                selectionMenu.style.left = `${menuLeft}px`;
                selectionMenu.style.top = `${menuTop}px`;
                selectionMenu.style.display = 'flex';
            }
        });

        // 点击空白区域隐藏菜单和清除选择
        doc.addEventListener('click', (e) => {
            // 检查点击是否在菜单内
            const isClickInMenu = e.target.closest('#selection-menu') || e.target.closest('#annotation-menu');
            // 如果点击不在菜单内，且不是在翻页过程中，则隐藏所有菜单并清除选择
            if (!isClickInMenu && !window.pageturning) {
                selectionMenu.style.display = 'none';
                annotationMenu.style.display = 'none';
                // 清除文本选择
                const selection = doc.getSelection();
                if (selection) {
                    selection.removeAllRanges();
                }
                isSelecting = false;
            }
        });

        // 阻止上下文菜单
        doc.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            return false;
        });
    });
}

function loadBook(bookUrl, cfi) {
    //console.log("bookUrl: " + bookUrl);
    book = ePub(bookUrl, {
        openAs: "epub",
    });
    // 添加错误处理
    book.on('openFailed', function(error) {
        console.error("Failed to open book:", error);
        if (window.Android) {
            const errorMsg = i18n && i18n.reader ? i18n.reader.loading + error.message : "Loading e-book failed: " + error.message;
            window.Android.onLoadError(errorMsg);
        }
    });
    book.loaded.metadata.then(metadata => {
        // 调用Android接口更新书籍信息
        window.Android.onBookMetadata(metadata.title, metadata.creator);
    });
    rendition = book.renderTo("viewer", {
        flow: "paginated",
        width: "100%",
        height: "100%",
        spread: "none",
        minSpreadWidth: 1000,
        snap: true
    });

    // 添加位置信息生成代码
    book.ready.then(() => {
        rendition.display(cfi || 0);

        // 立即生成位置信息
        book.locations.generate().then(() => {
            let progress = book.locations.percentageFromCfi(cfi);
            let progressPercent = Math.round(progress * 100);
            updateProgressText(progressPercent);

            // 移除原有的rendition级别的触摸事件处理
            // 所有触摸事件现在都在initializeSelection中的content hook里统一处理

            // 添加章节变化监听
            rendition.on('relocated', (location) => {
                currentLocation = location;
                
                // 获取当前章节信息
                for(let item of book.navigation.toc) {
                    if(item.href.indexOf(location.start.href) !== -1) {
                        let chapterTitle = item.label.trim();
                        document.getElementById('chapter-title').textContent = chapterTitle || '';
                        break;
                    }
                }

                // 确保位置信息已生成后再计算进度
                if (book.locations) {
                    let progress = book.locations.percentageFromCfi(location.start.cfi);
                    let progressPercent = Math.round(progress * 100);
                    updateProgressText(progressPercent);
                    if (window.Android) {
                        window.Android.onProgressUpdate(progressPercent, location.start.cfi);
                    }
                }
                // 重新应用标注(先删除再渲染)
                window.renderAnnotations(annotations);

                // 重新应用主题样式
                if (rendition && rendition.manager && rendition.manager.views) {
                    rendition.manager.views.forEach(view => {
                        if (view.contents) {
                            applyThemeToContent(view.contents);
                        }
                    });
                }
            });
        });
    }).then(() => {
        // 位置信息生成完成后，再加载标注
        if (window.Android) {
            window.Android.onBookLoaded();
            window.Android.loadAnnotations();
        }
        initializeSelection();
    });
}

// 生成目录
async function generateToc() {
    // 添加目录功能
    book.loaded.navigation.then(nav => {
        const toc = nav.toc;
        const tocContent = document.getElementById('toc-content');
        tocContent.innerHTML = '';

        toc.forEach(chapter => {
            const div = document.createElement('div');
            div.className = 'toc-item';
            div.textContent = chapter.label;
            div.onclick = () => {
                rendition.display(chapter.href);
                closeToc();
            };
            tocContent.appendChild(div);
        });
    });
}

// 添加目录控制函数
async function toggleToc() {
    const tocContainer = document.getElementById('toc-container');
    tocContainer.classList.toggle('active');
    if(!tocGenerated) {
        await generateToc();
        tocGenerated = true;
    }
}

function closeToc() {
    const tocContainer = document.getElementById('toc-container');
    tocContainer.classList.remove('active');
}