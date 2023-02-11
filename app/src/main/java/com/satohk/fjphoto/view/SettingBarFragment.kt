package com.satohk.fjphoto.view

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
import com.satohk.fjphoto.BuildConfig
import com.satohk.fjphoto.R
import com.satohk.fjphoto.databinding.FragmentSettingBarBinding
import com.satohk.fjphoto.viewmodel.SettingBarViewModel
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

        Utils.initUITable(binding.table, this)

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

        binding.buttonTermsAndConditions.setOnClickListener {
            val action = PhotoGridWithSideBarFragmentDirections.actionPhotoGridWithSidebarFragmentToOssListFragment(
                getString(R.string.url_terms_and_conditions)
            )
            view.findNavController().navigate(action)
        }

        binding.buttonPrivacyPolicy.setOnClickListener {
            val action = PhotoGridWithSideBarFragmentDirections.actionPhotoGridWithSidebarFragmentToOssListFragment(
                getString(R.string.url_privacy_policy)
            )
            view.findNavController().navigate(action)
        }

        binding.buttonOSS.setOnClickListener {
            val action = PhotoGridWithSideBarFragmentDirections.actionPhotoGridWithSidebarFragmentToOssListFragment(
                getString(R.string.url_oss_list)
            )
            view.findNavController().navigate(action)
        }

        binding.textVersion.text = BuildConfig.VERSION_NAME
    }

    override fun onFocus(){
        binding.spinnerSlideshowInterval.requestFocus()
    }
}