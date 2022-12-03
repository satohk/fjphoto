package com.satohk.gphotoframe.view

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satohk.gphotoframe.databinding.FragmentSettingBarBinding
import com.satohk.gphotoframe.viewmodel.SettingBarViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class SettingBarFragment() : Fragment(), SideBarFragmentInterface {
    private val _viewModel by sharedViewModel<SettingBarViewModel>()
    private var _binding: FragmentSettingBarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingBarBinding.inflate(inflater, container, false)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onKey = fun(view: View, i: Int, keyEvent: KeyEvent): Boolean {
            if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    _viewModel.enterToGrid()
                } else if (keyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    _viewModel.goBack()
                }
            }
            return false
        }

        Utils.initUITable(binding.table, this)

        // set key listener
        val keyHandleView: List<View> = listOf(
            binding.spinnerSlideshowInterval,
            binding.buttonOK
        )
        keyHandleView.forEach{v: View ->
            v.setOnKeyListener(onKey)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    _viewModel.displayMessageId.collect { it ->
                        it?.let { messageId ->
                            Toast.makeText(context, getText(messageId), Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    _viewModel.inTraining.collect {
                        binding.trainProgressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE
                    }
                }
            }
        }

        binding.buttonOK.setOnClickListener{_viewModel.enterToGrid()}
        binding.buttonTrain.setOnClickListener { _viewModel.doTrainAIModel() }
    }

    override fun onFocus(){
        binding.spinnerSlideshowInterval.requestFocus()
    }
}