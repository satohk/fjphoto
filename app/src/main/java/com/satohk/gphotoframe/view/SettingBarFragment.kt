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
import androidx.navigation.findNavController
import com.satohk.gphotoframe.BuildConfig
import com.satohk.gphotoframe.databinding.FragmentSettingBarBinding
import com.satohk.gphotoframe.viewmodel.SettingBarViewModel
import kotlinx.coroutines.launch
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

        val onKey = fun(_: View, _: Int, keyEvent: KeyEvent): Boolean {
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
            binding.spinnerSlideshowInterval
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    _viewModel.displayMessageId.collect { it ->
                        it?.let { messageId ->
                            Toast.makeText(context, getText(messageId), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        binding.buttonOSS.setOnClickListener {
            val action = PhotoGridWithSideBarFragmentDirections.actionPhotoGridWithSidebarFragmentToOssListFragment()
            view.findNavController().navigate(action)
        }

        binding.textVersion.text = BuildConfig.VERSION_NAME
    }

    override fun onFocus(){
        binding.spinnerSlideshowInterval.requestFocus()
    }
}