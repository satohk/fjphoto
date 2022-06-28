package com.satohk.gphotoframe.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.satohk.gphotoframe.*

import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.satohk.gphotoframe.viewmodel.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridWithSidebarFragment() : Fragment(R.layout.fragment_photo_grid_with_menu) {
    private val _viewModel by activityViewModels<PhotoGridWithSidebarViewModel>()
    private val _gridViewModel by activityViewModels<PhotoGridViewModel>()
    private val _menuBarViewModel by activityViewModels<MenuBarViewModel>()
    private var _menuBar:MenuBarFragment? = null
    private var _grid :PhotoGridFragment? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PhotoGridWidthSidebarFragment", "onViewCreated")

        val menuBar = MenuBarFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.side_menu_container, menuBar)
            .commit()
        _menuBar = menuBar

        val grid = PhotoGridFragment()
        grid.onBack = { _viewModel.onBackFromGrid() }
        childFragmentManager.beginTransaction()
            .replace(R.id.grid, grid)
            .commitAllowingStateLoss()

        Log.d("lifecycleScope", "viewLifecycleOwner.lifecycleScope.launch")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                launch {
                    Log.d("lifecycleScope", "_viewModel.menuBarFocused.collect")
                    _viewModel.sideBarFocused.collect {
                        changeFocus(
                            this@PhotoGridWithSidebarFragment.requireView(),
                            _viewModel.sideBarFocused.value,
                            true
                        )
                    }
                }
                launch{
                    Log.d("lifecycleScope", "_viewModel.sideBarType.collect")
                    _viewModel.sideBarType.collect {
                        _menuBarViewModel.initItemList(_viewModel.sideBarType.value)
                    }
                }
                launch{
                    Log.d("lifecycleScope", "_viewModel.gridContents.collect ")
                    _viewModel.gridContents.collect {
                        Log.d("_viewModel.gridContents.collect", it.toString())
                        _gridViewModel.setGridContents(it)
                    }
                }
                launch{
                    _menuBarViewModel.sideBarAction.collect {
                        if (it != null) {
                            _viewModel.onSidebarAction(it)
                        }
                    }
                }
            }
        }

        //backボタンの処理を追加
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true)
            {
                override fun handleOnBackPressed() {
                    Log.d("onBackPressedDispatcher", "back button pressed")
                    //findNavController().popBackStack()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onResume() {
        super.onResume()
        Log.d("onresume", this.context.toString())
        Log.d("onresume", _menuBar!!.context.toString())
        Log.d("onresume menuber", _menuBar.toString())

        changeFocus(this.requireView(), _viewModel.sideBarFocused.value, false)
    }

    private fun changeFocus(view:View, focusMenuBar: Boolean, animation:Boolean){
        val menuBarContainer = view.findViewById<FrameLayout>(R.id.side_menu_container)
        val gridContainer = view.findViewById<FrameLayout>(R.id.grid)

        // change focus
        if (focusMenuBar) {
            menuBarContainer.focusable = View.NOT_FOCUSABLE
            menuBarContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            _menuBar?.restoreLastFocus()
        } else {
            menuBarContainer.focusable = View.NOT_FOCUSABLE
            menuBarContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }

        // show/hide menubar
        val alpha = if (focusMenuBar) {1f} else {0f}
        ObjectAnimator.ofFloat(menuBarContainer, "alpha", alpha).apply {
            duration = if (animation) {200} else {0}
            start()
        }
    }
}