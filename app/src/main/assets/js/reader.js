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

// 朗读功能相关变量
let isTTSPlaying = false;
let currentTTSText = '';
let autoPageTurn = false;
const ttsPanel = document.getElementById('tts-panel');

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
    
    // 更新朗读相关按钮
    document.getElementById('tts-button').textContent = i18n.reader.read_aloud || '朗读';
    document.getElementById('tts-play-btn').textContent = i18n.reader.tts_play || '播放';
    document.getElementById('tts-pause-btn').textContent = i18n.reader.tts_pause || '暂停';
    document.getElementById('tts-stop-btn').textContent = i18n.reader.tts_stop || '停止';

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
    
    // 添加朗读按钮点击事件
    document.getElementById('tts-button').addEventListener('click', toggleTTSPanel);
    
    // 添加朗读控制按钮事件
    initTTSControls();

    rendition.hooks.content.register((contents) => {
        currentContents = contents;
        const doc = contents.document;

        doc.documentElement.style.webkitUserSelect = 'text';
        doc.documentElement.style.userSelect = 'text';

        // 应用主题样式
        applyThemeToContent(contents);

        // 触摸事件处理
        doc.addEventListener('touchstart', (e) => {
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
            touchStartTime = Date.now();
            isSelecting = false;

            // 添加长按定时器
            longPressTimer = setTimeout(() => {
                isSelecting = true;
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
            // 在选择结束后，添加一个短暂延迟再重置isSelecting
            // 这样可以确保selectionchange事件有足够时间处理
            setTimeout(() => {
                isSelecting = false;
            }, 300);
        }, { passive: true });

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

        // 点击空白区域隐藏菜单
        doc.addEventListener('click', (e) => {
            // 检查点击是否在菜单内
            const isClickInMenu = e.target.closest('#selection-menu') || e.target.closest('#annotation-menu');
            // 如果点击不在菜单内，则隐藏所有菜单
            if (!isClickInMenu) {
                selectionMenu.style.display = 'none';
                annotationMenu.style.display = 'none';
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
    let touchStartX;
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

            // 启用手势支持
            rendition.on("touchstart", event => {
                // 只有当不在选择模式时才记录起始位置用于翻页
                if (!isSelecting) {
                    const touches = event.changedTouches[0];
                    touchStartX = touches.screenX;
                }
            });

            rendition.on("touchend", event => {
                // 只有当不在选择模式时才处理翻页
                if (!isSelecting) {
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
                currentLocation = location;
                // 清空当前文本，确保获取新页面内容
                currentTTSText = '';
                
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

// 朗读功能相关函数
function toggleTTSPanel() {
    const ttsPanel = document.getElementById('tts-panel');
    ttsPanel.classList.toggle('active');
}

function closeTTSPanel() {
    const ttsPanel = document.getElementById('tts-panel');
    ttsPanel.classList.remove('active');
}

function initTTSControls() {
    const playBtn = document.getElementById('tts-play-btn');
    const stopBtn = document.getElementById('tts-stop-btn');
    const speedSlider = document.getElementById('speed-slider');
    const speedValue = document.getElementById('speed-value');
    const autoPageCheckbox = document.getElementById('auto-page-checkbox');
    
    playBtn.addEventListener('click', function() {
        if (isTTSPlaying) {
            pauseTTS();
        } else {
            startTTS();
        }
    });
    
    stopBtn.addEventListener('click', stopTTS);
    
    speedSlider.addEventListener('input', function() {
        const rate = parseFloat(this.value);
        speedValue.textContent = rate + 'x';
        if (window.Android) {
            window.Android.setSpeechRate(rate);
        }
        // 如果正在播放，停止并重新开始以应用新语速
        if (isTTSPlaying) {
            window.Android.stopTTS();
            setTimeout(() => {
                if (currentTTSText) {
                    window.Android.startTTS(currentTTSText);
                }
            }, 100);
        }
    });
    
    autoPageCheckbox.addEventListener('change', function() {
        autoPageTurn = this.checked;
    });
}

function startTTS() {
    if (!rendition || !rendition.manager || !rendition.manager.views) {
        updateTTSStatus('请先加载书籍');
        return;
    }
    
    // 每次都重新获取当前页面的文本
    let visibleText = '';
    
    if (currentContents && currentContents.document) {
        const doc = currentContents.document;
        const body = doc.body;
        if (body) {
            visibleText = body.innerText || body.textContent || '';
        }
    }
    
    if (!visibleText.trim()) {
        updateTTSStatus('没有可朗读的文本');
        return;
    }
    
    // 限制文本长度，只读前1000个字符
    currentTTSText = visibleText.trim().substring(0, 1000);
    
    if (window.Android) {
        window.Android.startTTS(currentTTSText);
    }
}

function pauseTTS() {
    if (window.Android) {
        window.Android.pauseTTS();
        isTTSPlaying = false;
        updateTTSStatus('已暂停 - 点击继续将重新开始');
        document.getElementById('tts-play-btn').textContent = '继续';
    }
}

function stopTTS() {
    if (window.Android) {
        window.Android.stopTTS();
    }
    isTTSPlaying = false;
    currentTTSText = '';
    updateTTSStatus('已停止');
    document.getElementById('tts-play-btn').textContent = '播放';
}

function updateTTSStatus(status) {
    const statusElement = document.getElementById('tts-status');
    if (statusElement) {
        statusElement.textContent = status;
    }
}

// Android回调函数
function onTTSStart() {
    isTTSPlaying = true;
    updateTTSStatus('正在朗读...');
    document.getElementById('tts-play-btn').textContent = '暂停';
}

function onTTSFinished() {
    isTTSPlaying = false;
    updateTTSStatus('朗读完成');
    document.getElementById('tts-play-btn').textContent = '播放';
    
    // 自动翻页功能
    if (autoPageTurn && rendition) {
        setTimeout(() => {
            rendition.next().then(() => {
                setTimeout(() => {
                    startTTS();
                }, 500);
            });
        }, 1000);
    }
}

function onTTSError() {
    isTTSPlaying = false;
    updateTTSStatus('朗读出错');
    document.getElementById('tts-play-btn').textContent = '播放';
}