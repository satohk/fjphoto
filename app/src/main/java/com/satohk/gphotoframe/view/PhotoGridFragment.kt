package com.satohk.gphotoframe.view

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.satohk.gphotoframe.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridFragment() : Fragment(), PhotoAdapter.ItemClickListener {
    private lateinit var _adapter: PhotoAdapter
    private val _viewModel by activityViewModels<PhotoGridViewModel>()
    private lateinit var _recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_grid, null)

        // set up the RecyclerView
        _recyclerView = view.findViewById<RecyclerView>(R.id.photo_grid)
        val numberOfColumns = 6
        _recyclerView.layoutManager =
            GridLayoutManager(requireContext(), numberOfColumns)
        _adapter = PhotoAdapter(this)
        _adapter.submitList(ArrayList(_viewModel.urlList.value))
        _adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT
        _recyclerView.adapter = _adapter

        lifecycleScope.launch {
            _viewModel.loadedDataSize.collect(){
                _adapter.submitList(ArrayList(_viewModel.urlList.value))
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
    }
}