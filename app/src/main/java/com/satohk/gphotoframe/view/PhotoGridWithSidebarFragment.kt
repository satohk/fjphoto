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
import com.satohk.gphotoframe.viewmodel.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridWithSidebarFragment() : Fragment(R.layout.fragment_photo_grid_with_sidebar) {
    private val _viewModel by activityViewModels<PhotoGridWithSidebarViewModel>()
    private val _gridViewModel by activityViewModels<PhotoGridViewModel>()
    private val _menuBarViewModel by activityViewModels<MenuBarViewModel>()
    private val _searchBarViewModel by activityViewModels<SearchBarViewModel>()
    private var _sideBarFragment:Fragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PhotoGridWidthSidebarFragment", "onViewCreated")

        setSidebar(_viewModel.sidebarType.value)

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
                    _viewModel.sidebarFocused.collect {
                        changeFocus(
                            this@PhotoGridWithSidebarFragment.requireView(),
                            _viewModel.sidebarFocused.value,
                            true
                        )
                    }
                }
                launch{
                    Log.d("lifecycleScope", "_viewModel.sideBarType.collect")
                    _viewModel.sidebarType.collect {
                        setSidebar(it)
                    }
                }
                launch{
                    Log.d("lifecycleScope", "_viewModel.gridContents.collect ")
                    _viewModel.gridContents.collect {
                        Log.d("_viewModel.gridContents.collect", it.toString())
                        _gridViewModel.setGridContents(it)
                    }
                }
                val sideBarViewModels: List<SideBarViewModel> = listOf(_menuBarViewModel, _searchBarViewModel)
                for(sideBarViewModel in sideBarViewModels){
                    launch{
                        sideBarViewModel.sideBarAction.collect {
                            if (it != null) {
                                _viewModel.onSidebarAction(it)
                            }
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

    private fun setSidebar(sideBarType:SideBarType){
        if(sideBarType == SideBarType.SEARCH){
            val searchBar = SearchBarFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.sidebar_container, searchBar)
                .commit()
            _sideBarFragment = searchBar
        }
        else if(sideBarType == SideBarType.SETTING){

        }
        else{ // Menu bar
            if(!(_sideBarFragment is MenuBarFragment)){
                val menuBar = MenuBarFragment()
                childFragmentManager.beginTransaction()
                    .replace(R.id.sidebar_container, menuBar)
                    .commit()
                _sideBarFragment = menuBar
            }
            _menuBarViewModel.initItemList(_viewModel.sidebarType.value)
        }
    }

    override fun onResume() {
        super.onResume()

        changeFocus(this.requireView(), _viewModel.sidebarFocused.value, false)
    }

    private fun changeFocus(view:View, focusSidebar: Boolean, animation:Boolean){
        val sidebarContainer = view.findViewById<FrameLayout>(R.id.sidebar_container)
        val gridContainer = view.findViewById<FrameLayout>(R.id.grid)

        // change focus
        if (focusSidebar) {
            sidebarContainer.focusable = View.NOT_FOCUSABLE
            sidebarContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            if(_sideBarFragment is SideBarFragmentInterface) {
                (_sideBarFragment as SideBarFragmentInterface).onFocus()
            }
        } else {
            sidebarContainer.focusable = View.NOT_FOCUSABLE
            sidebarContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }

        // show/hide sidebar
        val alpha = if (focusSidebar) {1f} else {0f}
        ObjectAnimator.ofFloat(sidebarContainer, "alpha", alpha).apply {
            duration = if (animation) {200} else {0}
            start()
        }
    }
}