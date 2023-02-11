package com.satohk.fjphoto.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.webkit.WebView
import com.satohk.fjphoto.R

class WebFragment : Fragment(R.layout.fragment_web) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val webView:WebView = view.findViewById(R.id.webView);
        val url = requireArguments().getString("url")
        Log.d("WebFragment", "onViewCreated url=$url")
        url?.let {
            webView.loadUrl(url)
        }
    }
}