package com.satohk.fjphoto.view

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.satohk.fjphoto.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.leanback.widget.VerticalGridView
import androidx.fragment.app.Fragment

import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.ToggleButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.satohk.fjphoto.repository.data.OrderBy
import com.satohk.fjphoto.viewmodel.GridContents
import com.satohk.fjphoto.viewmodel.PhotoGridItem
import com.satohk.fjphoto.viewmodel.PhotoGridViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoGridFragment() : Fragment(R.layout.fragment_photo_grid) {
    private val _viewModel by sharedViewModel<PhotoGridViewModel>()
    private lateinit var _verticalGridView: VerticalGridView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PhotoGridFragment", "onViewCreated")

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
        setToSlideshowButton.setOnClickListener {
            Toast.makeText(this.context, this.getText(R.string.msg_set_to_slideshow), Toast.LENGTH_LONG).show()
            _viewModel.onClickSetToSlideshowButton()
        }

        val slideShowButton = view.findViewById<Button>(R.id.playButton)
        slideShowButton.setOnClickListener{ _viewModel.onClickSlideshowButton() }

        val selectModeToggleButton = view.findViewById<ToggleButton>(R.id.selectModeToggleButton)
        selectModeToggleButton.setOnCheckedChangeListener { _, isChecked -> _viewModel.isSelectMode = isChecked }
        _viewModel.isSelectMode = selectModeToggleButton.isChecked

        val ascToggleButton = view.findViewById<ToggleButton>(R.id.ascToggleButton)
        ascToggleButton.isChecked = _viewModel.gridContents?.searchQuery?.queryRemote?.orderBy == OrderBy.CREATION_TIME_ASC
        ascToggleButton.setOnCheckedChangeListener { _, isChecked -> _viewModel.toggleOrder() }
    }

    private fun initRecyclerView(view: View) {
        // set up the RecyclerView
        _verticalGridView = view.findViewById(R.id.photo_grid)
        _verticalGridView.layoutManager = GridLayoutManager(requireContext(), _viewModel.numColumns.value)
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
        _verticalGridView.setOnScrollChangeListener { _, _, _, _, _ ->
            _viewModel.firstVisibleItemIndex = (_verticalGridView.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
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

        adapter.loadThumbnail = fun(photoGridItem: PhotoGridItem, width:Int?, height:Int?, position:Int, callback:(position:Int, bmp:Bitmap?)->Unit) {
            _viewModel.loadThumbnail(photoGridItem, width, height, position, callback)
        }

        _verticalGridView.adapter = adapter

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
                        _verticalGridView.layoutManager = GridLayoutManager(requireContext(), it)
                    }
                }
                launch{
                    _viewModel.changedItemIndex.collect{
                        val holder = _verticalGridView.findViewHolderForAdapterPosition(_viewModel.focusIndex)
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
        _verticalGridView.scrollToPosition(_viewModel.firstVisibleItemIndex)
    }
}