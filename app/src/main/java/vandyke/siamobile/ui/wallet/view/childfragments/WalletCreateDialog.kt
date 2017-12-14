/*
 * Copyright (c) 2017 Nicholas van Dyke
 *
 * This file is subject to the terms and conditions defined in 'LICENSE.md'
 */

package vandyke.siamobile.ui.wallet.view.childfragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_wallet_create.*
import vandyke.siamobile.R
import vandyke.siamobile.util.GenUtil
import vandyke.siamobile.util.SnackbarUtil

class WalletCreateDialog : BaseWalletFragment() {
    override val layout: Int = R.layout.fragment_wallet_create

    override fun create(view: View, savedInstanceState: Bundle?) {
        walletCreateSeed.visibility = View.GONE
        walletCreateFromSeed.setOnClickListener {
            if (walletCreateFromSeed.isChecked)
                walletCreateSeed.visibility = View.VISIBLE
            else
                walletCreateSeed.visibility = View.GONE
        }

        walletCreateForceWarning.visibility = View.GONE
        walletCreateForce.setOnClickListener {
            if (walletCreateForce.isChecked)
                walletCreateForceWarning.visibility = View.VISIBLE
            else
                walletCreateForceWarning.visibility = View.GONE
        }

        walletCreateButton.setOnClickListener(View.OnClickListener {
            val password = newPasswordCreate.text.toString()
            if (password != confirmNewPasswordCreate.text.toString()) {
                SnackbarUtil.snackbar(view, "New passwords don't match", Snackbar.LENGTH_SHORT)
                return@OnClickListener
            }
            val force = walletCreateForce.isChecked
            if (!walletCreateFromSeed.isChecked) {
                viewModel.create(password, force)
            } else {
                viewModel.create(password, force, walletCreateSeed.text.toString())
            }
        })
    }


    companion object {

        fun showSeed(seed: String, context: Context) {
            val msg = "Below is your wallet seed. Your wallet's addresses are generated using this seed. Therefore, any coins you " +
                    "send to this wallet and its addresses will \"belong\" to this seed. It's what you will need" +
                    " in order to recover your coins if something happens to your wallet, or to load your wallet on another device. Record it elsewhere, and keep it safe."
            AlertDialog.Builder(context)
                    .setTitle("Wallet seed")
                    .setMessage("$msg\n\n$seed")
                    .setPositiveButton("Copy seed", { _, _ ->
                        GenUtil.copyToClipboard(context, seed)
                        Toast.makeText(context, "Copied seed", Toast.LENGTH_SHORT).show()
                    })
                    .show()
        }
    }
}
