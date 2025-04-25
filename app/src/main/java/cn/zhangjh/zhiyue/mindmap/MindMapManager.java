package cn.zhangjh.zhiyue.mindmap;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class MindMapManager {
    private final WebView webView;
    private final MindMapCallback callback;

    public MindMapManager(WebView webView, MindMapCallback callback) {
        this.webView = webView;
        this.callback = callback;
    }

    public void renderMarkdown(String markdown) {
        String escapedMarkdown = markdown.replace("'", "\\'").replace("\n", "\\n");
        webView.evaluateJavascript("renderMarkdown('" + escapedMarkdown + "')", null);
    }

    public void zoomIn() {
        webView.evaluateJavascript("zoomIn()", null);
    }

    public void zoomOut() {
        webView.evaluateJavascript("zoomOut()", null);
    }

    public void resetZoom() {
        webView.evaluateJavascript("resetZoom()", null);
    }

    public class JsInterface {
        @JavascriptInterface
        public void onMindMapRendered() {
            if (callback != null) {
                callback.onMindMapRendered();
            }
        }
    }

    public interface MindMapCallback {
        void onMindMapRendered();
    }
}