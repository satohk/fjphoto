package com.satohk.gphotoframe.view

import android.util.Log
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.satohk.gphotoframe.R

class Utils {
    companion object{
        fun initUITable(table: TableLayout, parentFragment: Fragment) {
            val onFocusChange = fun(focusedView: View, focused: Boolean) {
                // focusのあたっているRowのテキストボックスをハイライト
                val parentView = focusedView.parent
                if (parentView !is TableRow) {
                    return
                }
                parentView.setBackgroundColor(
                    parentFragment.resources.getColor(
                        if(focused) R.color.menu_bar_button_background_highlight
                        else R.color.menu_bar_button_background,
                    parentFragment.context!!.theme)
                )
            }
            table.children.forEach { tableRow: View ->
                if(tableRow is TableRow){
                    Log.d("Utils", "tableRow:$tableRow")
                    tableRow.children.forEach { it:View ->
                        if(it.javaClass.simpleName != "TextView")
                            it.setOnFocusChangeListener(onFocusChange)
                    }
                }
            }
        }
    }
}