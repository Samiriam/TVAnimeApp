package com.tvanime.app.data.crawl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tvanime.app.domain.model.CrawlItem
import com.tvanime.app.domain.model.CrawlResult
import com.tvanime.app.domain.model.SiteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CrawlService @Inject constructor(
    private val context: Context
) {
    suspend fun crawlCategory(
        category: String,
        sites: List<SiteConfig>,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): CrawlResult = withContext(Dispatchers.Main) {
        val allItems = mutableListOf<CrawlItem>()
        var lastError: String? = null

        sites.forEachIndexed { index, site ->
            onProgress((index.toFloat() / sites.size), site.name)
            try {
                val items = crawlSite(site, category)
                allItems.addAll(items)
            } catch (e: Exception) {
                lastError = "${site.name}: ${e.message}"
            }
        }

        CrawlResult(
            category = category,
            site = sites.joinToString(", ") { it.name },
            items = allItems,
            crawledAt = System.currentTimeMillis(),
            success = allItems.isNotEmpty(),
            errorMessage = lastError
        )
    }

    private suspend fun crawlSite(site: SiteConfig, category: String): List<CrawlItem> {
        return suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            var finished = false
            lateinit var webView: WebView

            val timeoutRunnable = Runnable {
                if (!finished) {
                    finished = true
                    if (cont.isActive) cont.resume(emptyList())
                    webView.destroy()
                }
            }

            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 11; Build/RKQ1.200826.002) AppleWebKit/537.36 Chrome/90.0.4430.210 Mobile Safari/537.36"
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        handler.removeCallbacks(timeoutRunnable)
                        if (!finished) {
                            finished = true
                            view?.let { wv ->
                                extractCatalog(wv, site, category) { items ->
                                    if (cont.isActive) cont.resume(items)
                                    wv.destroy()
                                }
                            }
                        }
                    }
                }
            }

            cont.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                webView.destroy()
            }

            handler.postDelayed(timeoutRunnable, 30000L)
            webView.loadUrl(site.baseUrl)
        }
    }

    private fun extractCatalog(
        webView: WebView,
        site: SiteConfig,
        category: String,
        onResult: (List<CrawlItem>) -> Unit
    ) {
        val listSel = site.listSelector.replace("'", "\\'")
        val titleSel = site.titleSelector.replace("'", "\\'")
        val thumbSel = site.thumbnailSelector.replace("'", "\\'")
        val detailSel = site.detailUrlSelector.replace("'", "\\'")
        val baseUrl = site.baseUrl

        val js = "(function(){" +
            "var items=[];" +
            "var els=document.querySelectorAll('$listSel');" +
            "if(els.length===0){" +
            "els=Array.from(document.querySelectorAll('a[href*=\"/anime/\"],a[href*=\"/movie/\"],a[href*=\"/ver/\"]')).reduce(function(a,c){" +
            "var p=c.closest('div,article,li');" +
            "if(p&&a.indexOf(p)===-1)a.push(p);return a;},[]);}" +
            "els.forEach(function(el){" +
            "var t=el.querySelector('$titleSel');" +
            "var img=el.querySelector('$thumbSel');" +
            "var lnk=el.querySelector('$detailSel')||el;" +
            "var yr=el.querySelector('.year,.date,.release,span');" +
            "var rt=el.querySelector('.rating,.score,.stars');" +
            "var title=t?(t.textContent||t.title||'').trim():(lnk?lnk.textContent:'').trim();" +
            "var thumb='';" +
            "if(img)thumb=img.getAttribute('data-src')||img.getAttribute('src')||'';" +
            "if(!thumb){var bg=window.getComputedStyle(el).backgroundImage;if(bg&&bg!=='none'){var m=bg.match(/url\\(['\"]?([^'\"]+)['\"]?\\)/);if(m)thumb=m[1];}}" +
            "var href=lnk?(lnk.getAttribute('href')||''):'';" +
            "if(href&&!href.startsWith('http'))href='$baseUrl'.replace(/\\/$/,'')+href;" +
            "var year=yr?(yr.textContent||'').trim():'';" +
            "var rtText=rt?(rt.textContent||'').trim():'';" +
            "var rating=parseFloat(rtText.replace(/[^0-9.]/g,''))||0;" +
            "if(title)items.push({title:title.substring(0,200),thumbnail:thumb,year:year.substring(0,10),rating:rating,detailUrl:href});" +
            "});" +
            "return JSON.stringify(items);" +
            "})();"

        webView.evaluateJavascript(js) { jsonResult ->
            try {
                val items = mutableListOf<CrawlItem>()
                val arr = JSONArray(jsonResult)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    items.add(
                        CrawlItem(
                            title = obj.getString("title"),
                            thumbnail = obj.getString("thumbnail"),
                            year = obj.getString("year"),
                            rating = obj.getDouble("rating").toFloat(),
                            detailUrl = obj.getString("detailUrl"),
                            category = category,
                            source = site.name
                        )
                    )
                }
                onResult(items)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
}