package com.andrey6570.greek;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.webkit.WebViewAssetLoader;

/** Оболочка: приложение целиком лежит в assets и работает без интернета. */
public class MainActivity extends Activity {
    private static final int PICK = 41;
    private WebView web;
    private FrameLayout box;
    private ValueCallback<Uri[]> chooser;

    /** Мост: сохранение прогресса и цвет системных панелей */
    /* (описание ниже) */
    /** Мост для сохранения прогресса: отправляет текст через системное меню «Поделиться». */
    public class Bridge {
        @JavascriptInterface
        public void setTheme(boolean dark) {
            runOnUiThread(() -> applyBars(dark));
        }

        @JavascriptInterface
        public void shareText(String name, String text) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, name);
                i.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(i, name));
            });
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        box = new FrameLayout(this);
        box.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(box);
        applyBars(false);
        // targetSdk 35: приложение рисуется под системными панелями; отступы задаём контейнеру
        // (у самого WebView padding игнорируется, поэтому отдельный FrameLayout)
        box.setOnApplyWindowInsetsListener((v, insets) -> {
            int t, b, l, r;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets s = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                t = s.top; b = s.bottom; l = s.left; r = s.right;
            } else {
                t = insets.getSystemWindowInsetTop(); b = insets.getSystemWindowInsetBottom();
                l = insets.getSystemWindowInsetLeft(); r = insets.getSystemWindowInsetRight();
            }
            v.setPadding(l, t, r, b);
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
        });
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true); // localStorage: прогресс сохраняется
        s.setAllowFileAccess(false);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try {
                    startActivityForResult(p.createIntent(), PICK);
                } catch (Exception e) {
                    chooser = null;
                    return false;
                }
                return true;
            }
        });
        if (state != null) web.restoreState(state);
        else web.loadUrl("https://appassets.androidplatform.net/index.html");
    }

    /** Цвет фона и значков системных панелей под тему приложения */
    private void applyBars(boolean dark) {
        int bg = dark ? 0xFF000000 : 0xFFF2F2F7;
        box.setBackgroundColor(bg);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        int f = box.getSystemUiVisibility();
        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        box.setSystemUiVisibility(dark ? (f & ~light) : (f | light));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        } else super.onActivityResult(req, res, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
