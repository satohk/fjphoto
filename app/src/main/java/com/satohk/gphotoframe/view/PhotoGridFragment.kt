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
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ToggleButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import com.satohk.gphotoframe.viewmodel.GridContents
import com.satohk.gphotoframe.viewmodel.PhotoGridItem
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridFragment() : Fragment(R.layout.fragment_photo_grid) {
    private val _viewModel by sharedViewModel<PhotoGridViewModel>()
    private lateinit var _recyclerView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView(view)

        _viewModel.onChangeToPhotoViewListener = fun(gridContents:GridContents, autoPlay:Boolean, position:Int){
            val action =
                PhotoGridWithSideBarFragmentDirections.actionPhotoGridWithSidebarFragmentToPhotoFragment(
                    gridContents,
                    autoPlay,
                    position
                )
            view.findNavController().navigate(action)
        }

        val setToSlideshowButton = view.findViewById<Button>(R.id.setToSlideshowButton)
        setToSlideshowButton.setOnClickListener { _viewModel.onClickSetToSlideshowButton() }

        val slideShowButton = view.findViewById<Button>(R.id.playButton)
        slideShowButton.setOnClickListener{ _viewModel.onClickSlideshowButton() }

        val selectModeToggleButton = view.findViewById<ToggleButton>(R.id.selectModeToggleButton)
        selectModeToggleButton.setOnCheckedChangeListener { _, isChecked -> _viewModel.isSelectMode = isChecked }
        _viewModel.isSelectMode = selectModeToggleButton.isChecked
    }

    private fun initRecyclerView(view: View){
        // set up the RecyclerView
        _recyclerView = view.findViewById(R.id.photo_grid)
        _recyclerView.layoutManager = GridLayoutManager(requireContext(), _viewModel.numColumns.value)
        val adapter = PhotoAdapter(_viewModel.gridItemList)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT

        // event handler
        adapter.onClick = fun(_:View?, position:Int){
            _viewModel.onClickItem(position)
        }
        adapter.onFocus = fun(_:View?, position:Int) {
            _viewModel.focusIndex = position
            Log.d("menu onFocus",  "position:${position.toString()} vislbleItemPos:${_viewModel.firstVisibleItemIndex}")
        }
        _recyclerView.setOnScrollChangeListener {_, _, _, _, _ ->
            _viewModel.firstVisibleItemIndex = (_recyclerView.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            Log.d("menu onscroll",  "vislbleItemPos:${_viewModel.firstVisibleItemIndex}")
        }
        adapter.onKeyDown = fun(view:View?, position:Int, keyEvent: KeyEvent):Boolean{
            view?.let {
                Log.d("keydown", it.x.toString())
                if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && (it.x < 10.0f)) {
                    _viewModel.goBack()
                }
                else if(keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                    _viewModel.onClickItem(position)
                }
            }
            return false
        }

        adapter.loadThumbnail = fun(photoGridItem: PhotoGridItem, width:Int?, height:Int?, callback:(bmp: Bitmap?)->Unit) {
            _viewModel.loadThumbnail(photoGridItem, width, height, callback)
        }

        _recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    _viewModel.dataSize.collect {
                        adapter.notifyDataChange()
                    }
                }
                launch{
                    _viewModel.loading.collect{
                        view.findViewById<ProgressBar>(R.id.progress).visibility = if(it) View.VISIBLE else View.INVISIBLE
                    }
                }
                launch{
                    _viewModel.numColumns.collect{
                        _recyclerView.layoutManager = GridLayoutManager(requireContext(), it)
                    }
                }
                launch{
                    _viewModel.changedItemIndex.collect{
                        val holder = _recyclerView.findViewHolderForAdapterPosition(_viewModel.focusIndex)
                            as PhotoAdapter.PhotoViewHolder?
                        holder?.photoGridItem = _viewModel.gridItemList[it]
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("_viewModel.focusIndex", _viewModel.focusIndex.toString())
        setGridItemFocus()
    }

    private fun setGridItemFocus(){
        _recyclerView.scrollToPosition(_viewModel.firstVisibleItemIndex)
    }
}