package com.satohk.gphotoframe.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satohk.gphotoframe.*
import com.satohk.gphotoframe.viewmodel.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridWithSideBarFragment() : Fragment(R.layout.fragment_photo_grid_with_sidebar) {
    private val _viewModel by sharedViewModel<PhotoGridWithSideBarViewModel>()
    private val _gridViewModel by sharedViewModel<PhotoGridViewModel>()
    private val _searchBarViewModel by sharedViewModel<SearchBarViewModel>()
    private val _menuBarViewModel by sharedViewModel<MenuBarViewModel>()
    private var _sideBarFragment: Fragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PhotoGridWidthSideBarFragment", "onViewCreated")

        setSideBar(_viewModel.sideBarType.value)

        val grid = PhotoGridFragment()
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
                            this@PhotoGridWithSideBarFragment.requireView(),
                            _viewModel.sideBarFocused.value,
                            true
                        )
                    }
                }
                launch{
                    Log.d("lifecycleScope", "_viewModel.sideBarType.collect")
                    _viewModel.sideBarType.collect {
                        setSideBar(it)
                    }
                }
                launch{
                    Log.d("lifecycleScope", "_viewModel.gridContents.collect ")
                    _viewModel.gridContents.collect {
                        Log.d("_viewModel.gridContents.collect", it.toString())
                        _gridViewModel.setGridContents(it)
                    }
                }

                val sidebarViewModels: List<SideBarActionPublisherViewModel> =
                    listOf(_menuBarViewModel, _searchBarViewModel, _gridViewModel)
                sidebarViewModels.forEach{vm ->
                    launch{
                        vm.action.collect{ action ->
                            _viewModel.subscribeSideBarAction(action, true)
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
                    if(_viewModel.backStackSize > 0) {
                        _viewModel.subscribeSideBarAction(
                            SideBarAction(
                                SideBarActionType.BACK,
                                null,
                                null
                            ), false
                        )
                    }
                    else{
                        Log.d("onBackPressedDispatcher", "requireActivity().onBackPressed()")
                        requireActivity().finish()
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setSideBar(sideBarType:SideBarType){
        if(sideBarType == SideBarType.SEARCH){
            val searchBar = SearchBarFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.sidebar_container, searchBar)
                .commit()
            _sideBarFragment = searchBar
        }
        else if(sideBarType == SideBarType.SETTING){
            val settingBar = SettingBarFragment()
            childFragmentManager.beginTransaction()
                .replace(R.id.sidebar_container, settingBar)
                .commit()
            _sideBarFragment = settingBar
        }
        else{ // Menu bar
            if(_sideBarFragment !is MenuBarFragment){
                val menuBar = MenuBarFragment()
                childFragmentManager.beginTransaction()
                    .replace(R.id.sidebar_container, menuBar)
                    .commit()
                _sideBarFragment = menuBar
            }
            _menuBarViewModel.initItemList(_viewModel.sideBarType.value)
        }
    }

    override fun onResume() {
        super.onResume()

        changeFocus(this.requireView(), _viewModel.sideBarFocused.value, false)
    }

    private fun changeFocus(view:View, focusSideBar: Boolean, animation:Boolean){
        val sideBarContainer = view.findViewById<FrameLayout>(R.id.sidebar_container)
        val gridContainer = view.findViewById<FrameLayout>(R.id.grid)

        // change focus
        if (focusSideBar) {
            sideBarContainer.focusable = View.NOT_FOCUSABLE
            sideBarContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            if(_sideBarFragment is SideBarFragmentInterface) {
                (_sideBarFragment as SideBarFragmentInterface).onFocus()
            }
        } else {
            sideBarContainer.focusable = View.NOT_FOCUSABLE
            sideBarContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }

        // show/hide sidebar
        val alpha = if (focusSideBar) {1f} else {0f}
        ObjectAnimator.ofFloat(sideBarContainer, "alpha", alpha).apply {
            duration = if (animation) {200} else {0}
            start()
        }
    }
}