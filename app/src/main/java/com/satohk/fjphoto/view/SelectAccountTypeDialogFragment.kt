package com.satohk.fjphoto.view

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.gms.common.SignInButton
import com.satohk.fjphoto.R
import com.satohk.fjphoto.domain.ServiceProvider
import java.util.*


class SelectAccountTypeDialogFragment : DialogFragment() {
    var onSelected: ((accountType: ServiceProvider) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            //Dialogレイアウトにviewを取得
            val inflater = requireActivity().layoutInflater;
            val root = inflater.inflate(R.layout.dialog_select_account_type, null)
            val signInButton = root.findViewById<SignInButton>(R.id.google_button)
            signInButton.setOnClickListener {
                onSelected?.invoke(ServiceProvider.GOOGLE)
            }

            //DialogBuilderにdialogのviewをセット
            builder.setView(root)
                .setTitle(R.string.caption_select_login_method)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}