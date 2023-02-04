package com.satohk.fjphoto.viewmodel

import android.util.Log
import android.widget.AdapterView
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class Utils {
    companion object{
        fun spinnerIndex2str(index: Int, values: List<String>):String?{
            return if(index == AdapterView.INVALID_POSITION || index >= values.size) {
                null
            } else{
                values[index]
            }
        }

        fun str2date(dateStr: String): ZonedDateTime?{
            return try {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneId.systemDefault())
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }
}