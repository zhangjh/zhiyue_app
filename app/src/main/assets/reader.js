let rendition;
// 在文件顶部添加全局变量
window.isSelecting = false; // 添加这行，使isSelecting成为全局变量

// 添加加载标注方法
window.loadAnnotations = function(annotationsJson) {
    console.log("Received annotations:", annotationsJson);  // 添加日志
     // 解析JSON字符串
    const annotations = JSON.parse(annotationsJson);
    // 检查是否有data字段（因为后端返回的是BizListResponse）
    const annotationList = annotations.data || [];

    annotationList.forEach(annotation => {
        if (annotation.type === 'highlight') {
            rendition.annotations.highlight(
                annotation.cfiRange,
                {},
                (e) => {
                    e.target.style.backgroundColor = annotation.color;
                }
            );
        } else if (annotation.type === 'underline') {
            rendition.annotations.underline(
                annotation.cfiRange,
                {},
                undefined,
                "ul",
                {
                    "border": "none",
                    "stroke": annotation.color,
                    "stroke-width": "2px"
                }
            );
        }
    });
};

function initializeSelection() {
    const selectionMenu = document.getElementById('selection-menu');
    let currentCfiRange = null;
    let currentContents = null;
    let startX, startY;
    let touchStartTime = 0;
    let longPressTimer = null;
    const SWIPE_THRESHOLD = 50;
    const TAP_THRESHOLD = 200;
    const LONG_PRESS_THRESHOLD = 500; // 长按阈值设为500毫秒

    // 添加操作按钮事件监听
    document.body.addEventListener('click', function(e) {
        if (e.target.id === 'copy-btn') handleCopy();
        if (e.target.id === 'highlight-btn') handleHighlight();
        if (e.target.id === 'underline-btn') handleUnderline();
    });

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
            await rendition.annotations.highlight(currentCfiRange, {}, (e) => {
                e.target.style.backgroundColor = 'rgba(255,255,0,0.3)';
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
            rendition.annotations.underline(currentCfiRange,
                {},
                undefined,
                "ul",
                {
                    "border": "none",
                    "stroke": "#3366ff",
                    "stroke-width": "2px"
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

    // 添加目录按钮点击事件
    document.getElementById('toc-button').addEventListener('click', toggleToc);

    rendition.hooks.content.register((contents) => {
        currentContents = contents;
        const doc = contents.document;

        doc.documentElement.style.webkitUserSelect = 'text';
        doc.documentElement.style.userSelect = 'text';

        // 触摸事件处理
        doc.addEventListener('touchstart', (e) => {
            console.log("触摸开始");
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
            touchStartTime = Date.now();
            window.isSelecting = false;

            // 添加长按定时器
            longPressTimer = setTimeout(() => {
                console.log("长按触发");
                window.isSelecting = true;
                // 启用文本选择
                const range = doc.caretRangeFromPoint(startX, startY);
                if (range) {
                    const selection = doc.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(range);
                }
            }, LONG_PRESS_THRESHOLD);
        }, { passive: true });

        doc.addEventListener('touchmove', (e) => {
            // 如果移动距离超过阈值，取消长按定时器
            const moveX = e.touches[0].clientX;
            const moveY = e.touches[0].clientY;
            if (Math.abs(moveX - startX) > SWIPE_THRESHOLD ||
                Math.abs(moveY - startY) > SWIPE_THRESHOLD) {
                clearTimeout(longPressTimer);
            }

            if (isSelecting) {
                const touch = e.touches[0];
                const range = doc.caretRangeFromPoint(touch.clientX, touch.clientY);
                const selection = doc.getSelection();

                if (range && selection.rangeCount > 0) {
                    selection.extend(range.startContainer, range.startOffset);
                }
            }
        }, { passive: true });

        doc.addEventListener('touchend', () => {
            clearTimeout(longPressTimer);
            // 不在这里重置 isSelecting，让 selectionchange 事件处理它
        }, { passive: true });

        // 修改选择变化监听
        doc.addEventListener('selectionchange', () => {
            console.log("selection-menu selectionchange");
            const selection = doc.getSelection();
            const selectedText = selection.toString().trim();

            if (selectedText && window.isSelecting) {
                console.log("有选中文本且处于选择模式");
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

        // 点击其他区域隐藏菜单
        doc.addEventListener('click', (e) => {
            if (!selectionMenu.contains(e.target)) {
                selectionMenu.style.display = 'none';
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
    let book;
    let touchStartX; // 添加这行声明变量

    book = ePub(bookUrl);

    rendition = book.renderTo("viewer", {
        flow: "paginated",
        width: "100%",
        height: "100%",
        spread: "none",
        minSpreadWidth: 1000
    });
    
    book.ready.then(() => {
        return book.locations.generate();
    }).then(() => {
        if(cfi) {
            return rendition.display(cfi);
        } else {
            return rendition.display();
        }
    }).then(() => {
        if (window.Android) {
            window.Android.onBookLoaded();
            // 加载已存在的标注
            window.Android.loadAnnotations();
        }
        initializeSelection();
        
        // 启用手势支持 - 修改这部分代码，避免与文本选择冲突
        let lastTapTime = 0;
        rendition.on("touchstart", event => {
            // 只在非选择模式下处理翻页手势
            if (!window.isSelecting) {
                const touches = event.changedTouches[0];
                touchStartX = touches.screenX;
                lastTapTime = Date.now();
            }
        });
        
        rendition.on("touchend", event => {
            // 只在非选择模式下处理翻页手势
            if (!window.isSelecting && Date.now() - lastTapTime < 300) {
                const touches = event.changedTouches[0];
                const swipeDistance = touchStartX - touches.screenX;

                if (Math.abs(swipeDistance) > 50) {
                    if (swipeDistance > 0) {
                        rendition.next();
                    } else {
                        rendition.prev();
                    }
                }
            }
        });
        // 添加章节变化监听
        rendition.on('relocated', (location) => {
                            // 获取当前章节信息
                            for(let item of book.navigation.toc) {
                                if(item.href.indexOf(location.start.href) !== -1) {
                                    let chapterTitle = item.label.trim();
                                    document.getElementById('chapter-title').textContent = chapterTitle || '无标题';
                                    break;
                                }
                            }

                            // 现有的进度更新代码
                            let progress = book.locations.percentageFromCfi(location.start.cfi);
                            let progressPercent = Math.round(progress * 100);
                            document.getElementById('progress-container').textContent = `阅读进度：${progressPercent}%`;
                            if (window.Android) {
                                window.Android.onProgressUpdate(progressPercent, location.start.cfi);
                            }
                        });
    }).catch(error => {
         console.error("Failed to load book:", error);
         if (window.Android) {
             window.Android.onLoadError("加载电子书失败：" + error.message);
         }
    });

    book.loaded.metadata.then(metadata => {
        // 调用Android接口更新书籍信息
        window.Android.onBookMetadata(metadata.title, metadata.creator);
    });

    // 添加目录功能
    book.loaded.navigation.then(nav => {
        const toc = nav.toc;
        const tocContent = document.getElementById('toc-content');
        tocContent.innerHTML = ''; // 清空现有内容

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
function toggleToc() {
    const tocContainer = document.getElementById('toc-container');
    tocContainer.classList.toggle('active');
}

function closeToc() {
    const tocContainer = document.getElementById('toc-container');
    tocContainer.classList.remove('active');
}