package com.satohk.fjphoto.view

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.satohk.fjphoto.*

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.satohk.fjphoto.viewmodel.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class MenuBarFragment() : Fragment(R.layout.fragment_menu_bar), SideBarFragmentInterface {
    private val _viewModel by sharedViewModel<MenuBarViewModel>()
    private lateinit var _adapter: MenuBarItemAdapter
    private lateinit var _recyclerView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("MenuBarFragment", "onViewCreated")

        // init accouont button
        val accountButton = view.findViewById<Button>(R.id.account_button)
        accountButton.setCompoundDrawablePadding(20)
        accountButton.setOnClickListener {
            _viewModel.changeAccount()
        }

        // init menubar
        _recyclerView = view.findViewById<RecyclerView>(R.id.menu_bar)
        val numberOfColumns = 1
        _recyclerView.layoutManager =
            GridLayoutManager(requireContext(), numberOfColumns)
        _adapter = MenuBarItemAdapter()

        _adapter.onClick = fun(_:View?, position:Int):Unit{
            Log.d("click",  position.toString())
            _viewModel.enterToGrid(position)
        }

        _adapter.onFocus = fun(_:View?, position:Int):Unit {
            Log.d("menu onFocus",  position.toString())
            _viewModel.changeFocus(position)
        }

        _adapter.onKeyDown = fun(_:View?, position:Int, keyEvent: KeyEvent):Boolean{
            Log.d("keydown", keyEvent.toString())
            if(keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                _viewModel.enterToGrid(position)
            }
            else if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                _viewModel.goBack()
            }
            return false
        }

        _adapter.loadIcon = fun(menuBarItem: MenuBarViewModel.MenuBarItem, width:Int?, height:Int?, callback:(bmp: Bitmap?)->Unit) {
            _viewModel.loadIcon(menuBarItem, width, height, callback)
        }

        _recyclerView.adapter = _adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d("lifecycleScope", "_viewModel.itemList.collect")
                launch {
                    _viewModel.itemList.collect() {
                        _adapter.setList(_viewModel.itemList.value)
                        _adapter.notifyDataSetChanged()
                        if(_viewModel.itemList.value.size > 0) {
                            setFocusToMenuBarItem(_viewModel.lastFocusIndex)
                            _viewModel.changeFocus(_viewModel.lastFocusIndex)
                        }
                    }
                }
                launch {
                    _viewModel.activeUserName.collect() {
                        it?.let {
                            accountButton.text = it
                        }
                    }
                }
            }
        }
    }

    override fun onFocus(){
        setFocusToMenuBarItem(_viewModel.lastFocusIndex)
    }

    private fun setFocusToMenuBarItem(index: Int){
        val holder = _recyclerView.findViewHolderForAdapterPosition(index)
                as MenuBarItemAdapter.MenuBarItemViewHolder?
        holder?.binding?.root?.requestFocus()
    }
}