package com.satohk.gphotoframe.view

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridFragment() : Fragment() {
    private lateinit var _adapter: PhotoAdapter
    private val _viewModel by activityViewModels<PhotoGridViewModel>()
    private lateinit var _recyclerView: RecyclerView
    private val _numberOfColumns = 6
    var onBack: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photo_grid, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set up the RecyclerView
        _recyclerView = view.findViewById<RecyclerView>(R.id.photo_grid)
        _recyclerView.layoutManager =
            GridLayoutManager(requireContext(), _numberOfColumns)
        _adapter = PhotoAdapter(_viewModel.itemList.value, _viewModel)
        _adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT

        // event handler
        _adapter.onClick = fun(_:View?, position:Int):Unit{
            Log.i(
                "TAG",
                "You clicked number " + position
                    .toString() + ", which is at cell position " + position
            )
        }
        _adapter.onFocus = fun(_:View?, position:Int):Unit {
            Log.d("menu onFocus",  position.toString())
            if(position >= _viewModel.loadedDataSize.value - 10) {
                loadNextImages()
            }
        }
        _adapter.onKeyDown = fun(view:View?, position:Int, keyEvent: KeyEvent):Boolean{
            if(view != null) {
                Log.i("keydown", view?.x.toString())
                if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && (view.x < 10.0f)) {
                    onBack?.invoke()
                }
            }
            return false
        }

        _recyclerView.adapter = _adapter

        lifecycleScope.launch {
            _viewModel.loadedDataSize.collect(){
                val allDataSize = _viewModel.loadedDataSize.value
                val itemCount = _viewModel.lastLoadedDataSize
                Log.d("loadedDataSize", allDataSize.toString())
                _adapter.notifyItemRangeInserted(allDataSize - itemCount, itemCount)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNextImages()
    }

    private fun loadNextImages(){
        _viewModel.loadNextImageList()
    }
}