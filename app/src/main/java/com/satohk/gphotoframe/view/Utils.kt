package com.satohk.gphotoframe.view

import android.util.Log
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
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
                parentView.children.forEach { child: View ->
                    if (child is TextView) {
                        child.setTextColor(
                            parentFragment.resources.getColor(
                                if (focused)
                                    R.color.menu_bar_item_foreground_highlight
                                else
                                    R.color.menu_bar_item_foreground,
                                parentFragment.context!!.theme
                            )
                        )
                    }
                }
            }
            table.children.forEach { tableRow: View ->
                if(tableRow is TableRow){
                    Log.d("Utils", "tableRow:$tableRow")
                    tableRow.children.forEach { it:View ->
                        if(it.javaClass.simpleName != "TextView"){
                            it.setOnFocusChangeListener(onFocusChange)
                        }
                    }
                }
            }
        }
    }
}