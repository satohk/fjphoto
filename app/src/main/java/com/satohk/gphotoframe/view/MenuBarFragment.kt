package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import com.satohk.gphotoframe.*

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.viewmodel.MenuBarItem
import com.satohk.gphotoframe.viewmodel.MenuBarType
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


/**
 * Loads a grid of cards with movies to browse.
 */
class MenuBarFragment() : Fragment() {
    private lateinit var _adapter: MenuBarItemAdapter
    private lateinit var _recyclerView: RecyclerView
    private val _viewModel by activityViewModels<MenuBarViewModel>()
    var onSelectMenuItem: ((MenuBarItem) -> Unit)? = null
    var onFocusMenuItem: ((MenuBarItem) -> Unit)? = null
    var onBack: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_menu_bar,
            null
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(arguments != null) {
            val menuType = requireArguments().getSerializable("menuType")!! as MenuBarType
            _viewModel.selectedMenuBarType = menuType
        }

        // set up menubar
        _recyclerView = view.findViewById<RecyclerView>(R.id.menu_bar)
        val numberOfColumns = 1
        _recyclerView.layoutManager =
            GridLayoutManager(requireContext(), numberOfColumns)
        _adapter = MenuBarItemAdapter(this._viewModel.itemList.value)

        _adapter.onClick = fun(_:View?, position:Int):Unit{
            Log.d("click",  position.toString())
            onSelectMenuItem?.invoke(_viewModel.selectedItem)
        }

        _adapter.onFocus = fun(_:View?, position:Int):Unit {
            Log.d("menu onFocus",  position.toString())
            _viewModel.selectedItemIndex = position
            onFocusMenuItem?.invoke(_viewModel.selectedItem)
        }

        _adapter.onKeyDown = fun(_:View?, position:Int, keyEvent: KeyEvent):Boolean{
            Log.d("keydown", keyEvent.toString())
            if(keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                onSelectMenuItem?.invoke(_viewModel.selectedItem)
            }
            else if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                onBack?.invoke()
            }
            return false
        }

        _adapter.loadIcon = fun(menuBarItem: MenuBarItem, width:Int?, height:Int?, callback:(bmp: Bitmap?)->Unit) {
            _viewModel.loadIcon(menuBarItem, width, height, callback)
        }

        _recyclerView.adapter = _adapter

        lifecycleScope.launch {
            _viewModel.itemList.collect() {
                _adapter.notifyDataSetChanged()
                restoreLastFocus()
            }
        }

        restoreLastFocus()
    }

    fun restoreLastFocus(){
        Log.d("restoreLastFocus", _viewModel?.selectedItemIndex.toString())
        if(_recyclerView != null) {
            val holder = _recyclerView.findViewHolderForAdapterPosition(_viewModel.selectedItemIndex)
                as MenuBarItemAdapter.MenuBarItemViewHolder?
            holder?.binding?.root?.requestFocus()
        }
    }
}