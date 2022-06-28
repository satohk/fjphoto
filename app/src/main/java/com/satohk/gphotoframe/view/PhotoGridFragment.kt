package com.satohk.gphotoframe.view

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.satohk.gphotoframe.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satohk.gphotoframe.viewmodel.GridContents
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridFragment() : Fragment(R.layout.fragment_photo_grid) {
    private val _viewModel by activityViewModels<PhotoGridViewModel>()
    private lateinit var _recyclerView: RecyclerView
    private lateinit var _adapter: PhotoAdapter
    private lateinit var _layoutManager: GridLayoutManager
    private val _numberOfColumns = 6
    var onBack: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set up the RecyclerView
        _recyclerView = view.findViewById(R.id.photo_grid)
        _layoutManager = GridLayoutManager(requireContext(), _numberOfColumns)
        _recyclerView.layoutManager =_layoutManager
        _adapter = PhotoAdapter(_viewModel.itemList)
        _adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT

        // event handler
        _adapter.onClick = fun(_:View?, position:Int){
            Log.i(
                "TAG",
                "You clicked number " + position
                    .toString() + ", which is at cell position " + position
            )
        }
        _adapter.onFocus = fun(_:View?, position:Int) {
            Log.d("menu onFocus",  position.toString())
            if(position >= _viewModel.dataSize.value - 12) {
                _viewModel.loadNextImageList()
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

        _adapter.loadThumbnail = fun(photoGridItem: PhotoGridViewModel.PhotoGridItem, width:Int?, height:Int?, callback:(bmp: Bitmap?)->Unit) {
            _viewModel.loadThumbnail(photoGridItem, width, height, callback)
        }

        _recyclerView.adapter = _adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                _viewModel.dataSize.collect {
                    if(_viewModel.dataSize.value == 0){
                        _adapter.notifyItemRangeRemoved(0, _viewModel.lastDataSize)
                    }
                    else {
                        _adapter.notifyItemRangeInserted(
                            _viewModel.lastDataSize,
                            _viewModel.dataSize.value - _viewModel.lastDataSize
                        )
                    }
                }
            }
        }
    }
}