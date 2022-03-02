package com.satohk.gphotoframe.view

import android.os.Bundle
import android.view.ViewGroup
import com.satohk.gphotoframe.*

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.*
import androidx.navigation.fragment.navArgs


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
            R.layout.fragment_photo_grid_with_menu,
            null
        )

        val menuBar = MenuBarFragment()
        val bundle = Bundle()
        bundle.putSerializable("menuType", _args.menuType)
        menuBar.arguments = bundle

        parentFragmentManager.beginTransaction()
            .add(R.id.side_menu_container, menuBar)
            .commit()

        return view
    }
}