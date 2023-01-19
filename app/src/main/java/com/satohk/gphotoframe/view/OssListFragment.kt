package com.satohk.gphotoframe.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.satohk.gphotoframe.R

class OssListFragment : Fragment(R.layout.fragment_oss_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val webView:WebView = view.findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/licenses.html")
    }
}