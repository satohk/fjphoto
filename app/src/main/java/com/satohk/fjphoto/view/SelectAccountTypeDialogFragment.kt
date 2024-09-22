package com.satohk.fjphoto.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.satohk.fjphoto.R
import com.satohk.fjphoto.domain.ServiceProvider


class SelectAccountTypeDialogFragment : DialogFragment() {
    var onSelected: ((accountType: ServiceProvider) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            //Dialogレイアウトにviewを取得
            val inflater = requireActivity().layoutInflater;
            val root = inflater.inflate(R.layout.dialog_select_account_type, null)
            val signInButton = root.findViewById<Button>(R.id.google_photo_button)
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