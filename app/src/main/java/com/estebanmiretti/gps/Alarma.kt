package com.estebanmiretti.gps

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class Alarma : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarma)
        val webview = findViewById<WebView>(R.id.webview)
        val intentdata = intent.getStringExtra("wv")
        //webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK)
        webview.loadData(
            "<html><head><style>body{margin-left: auto; margin-top: 20px; margin-right: auto; max-width: 85%; background: red; color: white;}img{display: inline; height: auto;display: block; margin-left: auto; margin-right: auto; max-width: 100%; }</style>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>\n<body>"
                    + intentdata
                    + "</body></html>", "text/html", "UTF-8"
        )
    }
}