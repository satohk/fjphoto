package com.satohk.gphotoframe.view

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*


class DatePickerDialogFragment : DialogFragment(), OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
        return DatePickerDialog(context as Context, this, year, month, dayOfMonth)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        //日付が選択されたときの処理
    }
}