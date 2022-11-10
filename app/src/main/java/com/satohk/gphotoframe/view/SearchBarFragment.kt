package com.satohk.gphotoframe.view

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.*
import com.satohk.gphotoframe.*
import com.satohk.gphotoframe.databinding.FragmentSearchBarBinding
import com.satohk.gphotoframe.viewmodel.MenuBarViewModel
import com.satohk.gphotoframe.viewmodel.SearchBarViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Loads a grid of cards with movies to browse.
 */
class SearchBarFragment() : Fragment(), SideBarFragmentInterface {
    private val _viewModel by sharedViewModel<SearchBarViewModel>()
    private var _binding: FragmentSearchBarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBarBinding.inflate(inflater, container, false)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val showDatePicker = fun(parentEditText: View) {
            val currentDate: ZonedDateTime? =
                if(parentEditText == binding.editTextFromDate){
                    _viewModel.fromDate
                } else {
                    _viewModel.toDate
                }
            val (yearCurrent, monthCurrent, dayOfMonthCurrent) =
                if(currentDate === null){
                    val calendar = Calendar.getInstance()
                    Triple(calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH])
                } else{
                    Triple(currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth)
                }

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    (parentEditText as EditText).setText(int2datestr(year, month + 1, dayOfMonth))
                },
                yearCurrent,
                monthCurrent,
                dayOfMonthCurrent)
            datePickerDialog.show()
        }

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
            binding.spinnerMediaType,
            binding.spinnerContent,
            binding.switchFavorite,
            binding.buttonOK
        )
        keyHandleView.forEach{v: View ->
            v.setOnKeyListener(onKey)
        }

        binding.buttonOK.setOnClickListener{_viewModel.enterToGrid()}
        binding.editTextFromDate.setOnClickListener(showDatePicker)
        binding.editTextToDate.setOnClickListener(showDatePicker)
    }

    override fun onFocus(){
        binding.spinnerMediaType.requestFocus()
    }

    private fun int2datestr(year:Int, month:Int, day:Int): String{
        return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}