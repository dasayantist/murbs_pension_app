package com.appkings.murbs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class DialogManager {

    companion object {
        private var showNeutralButton = true
        private var showNegativeButton = true
        private var showTitle = true
        private var cancelable = false

        private var storeType = StoreType.GOOGLEPLAY

        private var titleResId = R.string.rate_dialog_title
        private var messageResId = R.string.rate_dialog_message
        private var textPositiveResId = R.string.rate_dialog_ok
        private var textNeutralResId = R.string.rate_dialog_cancel
        private var textNegativeResId = R.string.rate_dialog_no

        private var titleText: String? = null
        private var messageText: String? = null
        private var positiveText: String? = null
        private var neutralText: String? = null
        private var negativeText: String? = null

        private const val GOOGLE_PLAY_PACKAGE_NAME = "com.appkings.murbs"
        private const val GOOGLE_PLAY = "https://play.google.com/store/apps/details?id=com.appkings.murbs&pli=1"
        private const val AMAZON_APPSTORE = "amzn://apps/android?p="

        private var listener: Reference<OnClickButtonListener>? = null

        private fun getListener(): OnClickButtonListener? {
            return listener?.get()
        }

        fun getTitleText(context: Context): String {
            return titleText ?: context.getString(titleResId)
        }

        fun getMessageText(context: Context): String {
            return messageText ?: context.getString(messageResId)
        }

        fun getPositiveText(context: Context): String {
            return positiveText ?: context.getString(textPositiveResId)
        }

        fun getNeutralText(context: Context): String {
            return neutralText ?: context.getString(textNeutralResId)
        }

        fun getNegativeText(context: Context): String {
            return negativeText ?: context.getString(textNegativeResId)
        }

        fun getStoreType(): StoreType {
            return storeType
        }

        fun createIntentForGooglePlay(context: Context): Intent {
            val packageName = context.packageName
            val intent = Intent(Intent.ACTION_VIEW, getGooglePlay(packageName))
            if (isPackageExists(context, GOOGLE_PLAY_PACKAGE_NAME)) {
                intent.setPackage(GOOGLE_PLAY_PACKAGE_NAME)
            }
            return intent
        }

        fun createIntentForAmazonAppstore(context: Context): Intent {
            val packageName = context.packageName
            return Intent(Intent.ACTION_VIEW, getAmazonAppstore(packageName))
        }

        fun getGooglePlay(packageName: String): Uri? {
            return packageName.takeIf { it.isNotEmpty() }?.let { Uri.parse(GOOGLE_PLAY + it) }
        }

        fun getAmazonAppstore(packageName: String): Uri? {
            return packageName.takeIf { it.isNotEmpty() }?.let { Uri.parse(AMAZON_APPSTORE + it) }
        }

        fun isPackageExists(context: Context, targetPackage: String): Boolean {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            return packages.any { it.packageName == targetPackage }
        }

        private fun underHoneyComb(): Boolean {
            return false
        }

        private fun isLollipop(): Boolean {
            return Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP || Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1
        }

        private fun getDialogTheme(): Int {
            return if (isLollipop()) R.style.CustomLollipopDialogStyle else 0
        }

        @SuppressLint("NewApi")
        fun getDialogBuilder(context: Context): AlertDialog.Builder {
            return if (underHoneyComb()) {
                AlertDialog.Builder(context)
            } else {
                AlertDialog.Builder(context, getDialogTheme())
            }
        }
    }

    private var view: View? = null

    fun create(context: Context, options: DialogManager): Dialog {
        val builder = getDialogBuilder(context)
        builder.setMessage(getMessageText(context))

        if (shouldShowTitle()) builder.setTitle(getTitleText(context))

        builder.setCancelable(getCancelable())

        view?.let { builder.setView(it) }

        val listener = getListener()

        builder.setPositiveButton(getPositiveText(context)) { dialog, which ->
            val intentToAppstore = if (getStoreType() == StoreType.GOOGLEPLAY) {
                createIntentForGooglePlay(context)
            } else {
                createIntentForAmazonAppstore(context)
            }
            context.startActivity(intentToAppstore)
            AppRate.setAgreeShowDialog(context, false)
            listener?.onClickButton(which)
        }

        if (shouldShowNeutralButton()) {
            builder.setNeutralButton(getNeutralText(context)) { dialog, which ->
                AppRate.setRemindInterval(context)
                listener?.onClickButton(which)
            }
        }

        if (shouldShowNegativeButton()) {
            builder.setNegativeButton(getNegativeText(context)) { dialog, which ->
                AppRate.setAgreeShowDialog(context, false)
                listener?.onClickButton(which)
            }
        }
        return builder.create()
    }

    fun shouldShowNeutralButton(): Boolean {
        return showNeutralButton
    }

    fun setShowNeutralButton(showNeutralButton: Boolean) {
        DialogManager.showNeutralButton = showNeutralButton
    }

    fun shouldShowNegativeButton(): Boolean {
        return showNegativeButton
    }

    fun setShowNegativeButton(showNegativeButton: Boolean) {
        DialogManager.showNegativeButton = showNegativeButton
    }

    fun shouldShowTitle(): Boolean {
        return showTitle
    }

    fun setShowTitle(showTitle: Boolean) {
        DialogManager.showTitle = showTitle
    }

    fun getCancelable(): Boolean {
        return cancelable
    }

    fun setCancelable(cancelable: Boolean) {
        DialogManager.cancelable = cancelable
    }

    fun setStoreType(appstore: StoreType) {
        DialogManager.storeType = appstore
    }

    fun setTitleResId(titleResId: Int) {
        DialogManager.titleResId = titleResId
    }

    fun setMessageResId(messageResId: Int) {
        DialogManager.messageResId = messageResId
    }

    fun setTextPositiveResId(textPositiveResId: Int) {
        DialogManager.textPositiveResId = textPositiveResId
    }

    fun setTextNeutralResId(textNeutralResId: Int) {
        DialogManager.textNeutralResId = textNeutralResId
    }

    fun setTextNegativeResId(textNegativeResId: Int) {
        DialogManager.textNegativeResId = textNegativeResId
    }

    fun getView(): View? {
        return view
    }

    fun setView(view: View) {
        this.view = view
    }

    fun setListener(listener: OnClickButtonListener) {
        DialogManager.listener = WeakReference(listener)
    }

    fun setTitleText(titleText: String) {
        DialogManager.titleText = titleText
    }

    fun setMessageText(messageText: String) {
        DialogManager.messageText = messageText
    }

    fun setPositiveText(positiveText: String) {
        DialogManager.positiveText = positiveText
    }

    fun setNeutralText(neutralText: String) {
        DialogManager.neutralText = neutralText
    }

    fun setNegativeText(negativeText: String) {
        DialogManager.negativeText = negativeText
    }
}

enum class StoreType {
    GOOGLEPLAY,
    AMAZON
}

interface OnClickButtonListener {
    fun onClickButton(which: Int)
}