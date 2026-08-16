package com.tvtime.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SyncingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_syncing, container, false)

        TubiAccount.init(requireContext())

        val etEmail = view.findViewById<EditText>(R.id.et_tubi_email)
        val etPass = view.findViewById<EditText>(R.id.et_tubi_pass)
        val btnTubiLogin = view.findViewById<Button>(R.id.btn_tubi_login)
        val tvTubiStatus = view.findViewById<TextView>(R.id.tv_tubi_status)
        val btnTubiSync = view.findViewById<Button>(R.id.btn_tubi_sync)
        val tvTubiSyncStatus = view.findViewById<TextView>(R.id.tv_tubi_sync_status)

        fun refreshTubiUi() {
            if (TubiAccount.isLoggedIn()) {
                tvTubiStatus?.text = "Signed in as ${TubiAccount.email()}"
                btnTubiLogin?.text = "Sign out"
                etEmail?.isEnabled = false
                etPass?.isEnabled = false
            } else {
                tvTubiStatus?.text = "Not signed in"
                btnTubiLogin?.text = "Sign in to Tubi"
                etEmail?.isEnabled = true
                etPass?.isEnabled = true
            }
        }
        refreshTubiUi()

        fun syncTubi() {
            if (!TubiAccount.isLoggedIn()) {
                tvTubiSyncStatus?.text = "Sign in to Tubi first"
                return
            }
            btnTubiSync?.isEnabled = false
            tvTubiSyncStatus?.text = "Syncing your Tubi lists…"
            TubiRepository.fetchUserLists { wl, ms, diag ->
                requireActivity().runOnUiThread {
                    btnTubiSync?.isEnabled = true
                    if (ms == null) {
                        tvTubiSyncStatus?.text = "Sync failed: $diag"
                        return@runOnUiThread
                    }
                    val msCount = ms.size
                    WatchlistStore.init(requireContext())
                    // My Stuff = your real saved Tubi bookmarks only (continue-watching /
                    // WatchList is intentionally not shown in the widget).
                    WatchlistStore.saveMyStuff(ms)
                    TVTimeWidgetProvider.updateAll(requireContext())
                    tvTubiSyncStatus?.text = buildString {
                        append("Synced: $msCount bookmarks into My Stuff")
                        if (wl != null) append(" · ${wl.size} continue-watching ignored (WatchList removed)")
                    }
                }
            }
        }

        btnTubiSync?.setOnClickListener { syncTubi() }

        btnTubiLogin?.setOnClickListener {
            if (TubiAccount.isLoggedIn()) {
                TubiAccount.signOut()
                etEmail?.text?.clear()
                etPass?.text?.clear()
                refreshTubiUi()
                return@setOnClickListener
            }
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val pass = etPass?.text?.toString() ?: ""
            if (email.isEmpty() || pass.isEmpty()) {
                tvTubiStatus?.text = "Enter email and password"
                return@setOnClickListener
            }
            btnTubiLogin.isEnabled = false
            tvTubiStatus?.text = "Signing in…"
            TubiAccount.login(email, pass) { ok, err ->
                requireActivity().runOnUiThread {
                    btnTubiLogin.isEnabled = true
                    if (ok) {
                        refreshTubiUi()
                        syncTubi()
                    } else {
                        tvTubiStatus?.text = "Sign-in failed: ${err ?: "unknown error"}"
                    }
                }
            }
        }

        return view
    }
}
