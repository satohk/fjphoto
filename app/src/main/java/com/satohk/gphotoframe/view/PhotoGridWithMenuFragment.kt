package com.satohk.gphotoframe.view

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
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridWithMenuFragment() : Fragment() {
    private val _args: PhotoGridWithMenuFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            if(_args.focusMenu){ R.layout.fragment_photo_grid_with_menu }
            else{ R.layout.fragment_photo_grid_with_menu_small },
            null
        )

        // set menuBar
        val menuBar = MenuBarFragment()
        val bundle = Bundle()
        bundle.putSerializable("menuType", _args.menuType)
        menuBar.arguments = bundle

        menuBar.onSelectMenuItem = {item:MenuBarViewModel.MenuBarItem ->
            Log.d("debug", item.toString())

            when(item.itemType){
                MenuBarViewModel.MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST -> {
                    val action = PhotoGridWithMenuFragmentDirections.actionPhotoGridWithMenuFragmentSelf(
                        MenuBarViewModel.MenuType.ALBUM_LIST,
                        true
                    )
                    findNavController().navigate(action)
                }
                MenuBarViewModel.MenuBarItem.MenuBarItemType.SEARCH -> {

                }
                else -> {
                    val action = PhotoGridWithMenuFragmentDirections.actionPhotoGridWithMenuFragmentSelf(
                        _args.menuType,
                        false
                    )
                    findNavController().navigate(action)
                }
            }
        }
        // set menubar width
        if(!_args.focusMenu){
            menuBar
        }

        menuBar.onBack = {
            Log.d("onBack", "onback")
        }

        parentFragmentManager.beginTransaction()
            .add(R.id.side_menu_container, menuBar)
            .commit()

        // set gridView
        val grid = PhotoGridFragment()
        parentFragmentManager.beginTransaction()
            .add(R.id.grid, grid)
            .commit()

        changeFocus(view, _args.focusMenu)

        view.c

        return view
    }

    private fun changeFocus(view:View, focusMenuBar: Boolean){
        val menuBarContainer = view.findViewById<FrameLayout>(R.id.side_menu_container)
        val gridContainer = view.findViewById<FrameLayout>(R.id.grid)

        if(focusMenuBar) {
            menuBarContainer.focusable = View.NOT_FOCUSABLE
            menuBarContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
        else{
            menuBarContainer.focusable = View.NOT_FOCUSABLE
            menuBarContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            gridContainer.focusable = View.NOT_FOCUSABLE
            gridContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
    }
}