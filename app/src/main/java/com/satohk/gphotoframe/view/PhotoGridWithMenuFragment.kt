package com.satohk.gphotoframe.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.satohk.gphotoframe.*

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.satohk.gphotoframe.viewmodel.MenuBarItem
import com.satohk.gphotoframe.viewmodel.MenuBarType
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridWithMenuFragment() : Fragment() {
    private val _args: PhotoGridWithMenuFragmentArgs by navArgs()
    private var _menuBar:MenuBarFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_photo_grid_with_menu,
            null
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuBar = MenuBarFragment()
        val grid = PhotoGridFragment()

        // set menuBar
        val bundle = Bundle()
        bundle.putSerializable("menuType", _args.menuType)
        menuBar.arguments = bundle

        menuBar.onSelectMenuItem = { item: MenuBarItem ->
            when(item.itemType){
                MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST -> {
                    val action = PhotoGridWithMenuFragmentDirections.actionPhotoGridWithMenuFragmentSelf(
                        MenuBarType.ALBUM_LIST,
                        true
                    )
                    findNavController().navigate(action)
                }
                MenuBarItem.MenuBarItemType.SEARCH -> {
                }
                else -> {
                    changeFocus(view, false, true)
                }
            }
        }

        menuBar.onBack = {
            Log.d("onBack", "onback")
        }

        // set gridView
        grid.onBack = {
            changeFocus(view, true, true)
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.side_menu_container, menuBar)
            .replace(R.id.grid, grid)
            .commit()

        _menuBar = menuBar
    }

    override fun onResume() {
        super.onResume()
        Log.d("onresume", this.context.toString())
        Log.d("onresume", _menuBar!!.context.toString())
        Log.d("onresume menuber", _menuBar.toString())

        changeFocus(this.requireView(), _args.focusMenu, false)
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