package com.appkings.murbs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import java.util.Date

class AppRate private constructor(context: Context) {
    companion object {
        @Volatile
        private var singleton: AppRate? = null

        fun with(context: Context): AppRate {
            return singleton ?: synchronized(this) {
                singleton ?: AppRate(context).also { singleton = it }
            }
        }

        fun showRateDialogIfMeetsConditions(context: Context) {
            val isMeetsConditions = singleton?.isDebug == true || singleton?.shouldShowRateDialog() == true
            if (isMeetsConditions && context is Activity) {
                singleton?.showRateDialog(context)
            }
        }

        private fun isOverDate(targetDate: Long, threshold: Int): Boolean {
            return Date().time - targetDate >= threshold * 24L * 60 * 60 * 1000
        }

        // Preference helper methods
        private const val PREF_FILE_NAME = "android_rate_pref_file"
        private const val PREF_KEY_INSTALL_DATE = "android_rate_install_date"
        private const val PREF_KEY_LAUNCH_TIMES = "android_rate_launch_times"
        private const val PREF_KEY_IS_AGREE_SHOW_DIALOG = "android_rate_is_agree_show_dialog"
        private const val PREF_KEY_REMIND_INTERVAL = "android_rate_remind_interval"

        private fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }

        private fun getPreferencesEditor(context: Context): SharedPreferences.Editor {
            return getPreferences(context).edit()
        }

        /**
         * Clear data in shared preferences.<br/>
         *
         * @param context context
         */
        fun clearSharedPreferences(context: Context) {
            val editor = getPreferencesEditor(context)
            editor.remove(PREF_KEY_INSTALL_DATE)
            editor.remove(PREF_KEY_LAUNCH_TIMES)
            editor.apply()
        }

        /**
         * Set agree flag about show dialog.<br/>
         * If it is false, rate dialog will never shown unless data is cleared.
         *
         * @param context context
         * @param isAgree agree with showing rate dialog
         */
        fun setAgreeShowDialog(context: Context, isAgree: Boolean) {
            val editor = getPreferencesEditor(context)
            editor.putBoolean(PREF_KEY_IS_AGREE_SHOW_DIALOG, isAgree)
            editor.apply()
        }

        fun getIsAgreeShowDialog(context: Context): Boolean {
            return getPreferences(context).getBoolean(PREF_KEY_IS_AGREE_SHOW_DIALOG, true)
        }

        fun setRemindInterval(context: Context) {
            val editor = getPreferencesEditor(context)
            editor.remove(PREF_KEY_REMIND_INTERVAL)
            editor.putLong(PREF_KEY_REMIND_INTERVAL, Date().time)
            editor.apply()
        }

        fun getRemindInterval(context: Context): Long {
            return getPreferences(context).getLong(PREF_KEY_REMIND_INTERVAL, 0)
        }

        fun setInstallDate(context: Context) {
            val editor = getPreferencesEditor(context)
            editor.putLong(PREF_KEY_INSTALL_DATE, Date().time)
            editor.apply()
        }

        fun getInstallDate(context: Context): Long {
            return getPreferences(context).getLong(PREF_KEY_INSTALL_DATE, 0)
        }

        fun setLaunchTimes(context: Context, launchTimes: Int) {
            val editor = getPreferencesEditor(context)
            editor.putInt(PREF_KEY_LAUNCH_TIMES, launchTimes)
            editor.apply()
        }

        fun getLaunchTimes(context: Context): Int {
            return getPreferences(context).getInt(PREF_KEY_LAUNCH_TIMES, 0)
        }

        fun isFirstLaunch(context: Context): Boolean {
            return getPreferences(context).getLong(PREF_KEY_INSTALL_DATE, 0) == 0L
        }
    }

    @SuppressLint("StaticFieldLeak")
    private val context: Context = context.applicationContext

    private var installDate = 10
    private var launchTimes = 10
    private var remindInterval = 1
    private var isDebug = false
    val options = DialogManager()

    fun setLaunchTimes(launchTimes: Int): AppRate {
        this.launchTimes = launchTimes
        return this
    }

    fun setInstallDays(installDate: Int): AppRate {
        this.installDate = installDate
        return this
    }

    fun setRemindInterval(): AppRate {
        this.remindInterval = 2
        return this
    }

    fun setShowLaterButton(isShowNeutralButton: Boolean): AppRate {
        options.setShowNeutralButton(isShowNeutralButton)
        return this
    }

    fun setShowNeverButton(isShowNeverButton: Boolean): AppRate {
        options.setShowNegativeButton(isShowNeverButton)
        return this
    }

    fun setShowTitle(isShowTitle: Boolean): AppRate {
        options.setShowTitle(isShowTitle)
        return this
    }

    fun clearAgreeShowDialog(): AppRate {
        setAgreeShowDialog(context, true)
        return this
    }

    fun clearSettingsParam(): AppRate {
        setAgreeShowDialog(context, true)
        clearSharedPreferences(context)
        return this
    }

    fun setAgreeShowDialog(clear: Boolean): AppRate {
        setAgreeShowDialog(context, clear)
        return this
    }

    fun setView(view: View): AppRate {
        options.setView(view)
        return this
    }

    fun setOnClickButtonListener(listener: OnClickButtonListener): AppRate {
        options.setListener(listener)
        return this
    }

    fun setTitle(resourceId: Int): AppRate {
        options.setTitleResId(resourceId)
        return this
    }

    fun setTitle(title: String): AppRate {
        options.setTitleText(title)
        return this
    }

    fun setMessage(resourceId: Int): AppRate {
        options.setMessageResId(resourceId)
        return this
    }

    fun setMessage(message: String): AppRate {
        options.setMessageText(message)
        return this
    }

    fun setTextRateNow(resourceId: Int): AppRate {
        options.setTextPositiveResId(resourceId)
        return this
    }

    fun setTextRateNow(positiveText: String): AppRate {
        options.setPositiveText(positiveText)
        return this
    }

    fun setTextLater(resourceId: Int): AppRate {
        options.setTextNeutralResId(resourceId)
        return this
    }

    fun setTextLater(neutralText: String): AppRate {
        options.setNeutralText(neutralText)
        return this
    }

    fun setTextNever(resourceId: Int): AppRate {
        options.setTextNegativeResId(resourceId)
        return this
    }

    fun setTextNever(negativeText: String): AppRate {
        options.setNegativeText(negativeText)
        return this
    }

    fun setCancelable(cancelable: Boolean): AppRate {
        options.setCancelable(cancelable)
        return this
    }

    fun setStoreType(appstore: StoreType): AppRate {
        options.setStoreType(appstore)
        return this
    }

    fun monitor() {
        if (isFirstLaunch(context)) {
            setInstallDate(context)
        }
        setLaunchTimes(context, getLaunchTimes(context) + 1)
    }

    private fun showRateDialog(activity: Activity) {
        if (!activity.isFinishing) {
            options.create(activity, options).show()
        }
    }

    private fun shouldShowRateDialog(): Boolean {
        return getIsAgreeShowDialog(context) &&
                isOverLaunchTimes() &&
                isOverInstallDate() &&
                isOverRemindDate()
    }

    private fun isOverLaunchTimes(): Boolean {
        return getLaunchTimes(context) >= launchTimes
    }

    private fun isOverInstallDate(): Boolean {
        return isOverDate(getInstallDate(context), installDate)
    }

    private fun isOverRemindDate(): Boolean {
        return isOverDate(getRemindInterval(context), remindInterval)
    }

    fun isDebug(): Boolean {
        return isDebug
    }

    fun setDebug(isDebug: Boolean): AppRate {
        this.isDebug = isDebug
        return this
    }
}