package com.satohk.gphotoframe.view

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.satohk.gphotoframe.*

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


/**
 * Loads a grid of cards with movies to browse.
 */
class MenuBarFragment() : Fragment(), MenuBarItemAdapter.ItemClickListener {
    private lateinit var _adapter: MenuBarItemAdapter
    private val _viewModel by activityViewModels<MenuBarViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_menu_bar,
            null
        )
        if(arguments != null) {
            val menuType = requireArguments().getSerializable("menuType")!! as MenuBarViewModel.MenuType
            _viewModel.selectedMenuType = menuType
        }


        // set up menubar
        val recyclerView = view.findViewById<RecyclerView>(R.id.menu_bar)
        val numberOfColumns = 1
        recyclerView.layoutManager =
            GridLayoutManager(requireContext(), numberOfColumns)
        _adapter = MenuBarItemAdapter(this, _viewModel)
        recyclerView.adapter = _adapter
        _adapter.submitList(_viewModel.itemList)

        lifecycleScope.launch {
            if(_viewModel.selectedMenuType == MenuBarViewModel.MenuType.ALBUM_LIST) {
                _viewModel.albumListLoaded.collect() {
                    _adapter.submitList(_viewModel.itemList)
                }
            }
        }

        return view
    }

    override fun onItemClick(view: View?, position: Int) {
        Log.i(
            "TAG",
            "You clicked number " + position
                .toString() + ", which is at cell position " + position
        )
        val action = PhotoGridWithMenuFragmentDirections.actionMenuFragmentSelf(MenuBarViewModel.MenuType.ALBUM_LIST)
        findNavController().navigate(action)
    }
}