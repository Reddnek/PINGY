package app.logic

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.*
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.sql.Timestamp

object PingyUtils {

    /** This function is used to calculate the ICMP Ping to a certain server or end host.
     * This is basically the most important function in our entire app **/
    fun pingIcmp(host: String, packet: Int): Double {
        val result: Double
        try {
            val p = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 1 -s $packet $host")
            val i = p.inputStream
            val s = i.reader().readText()
            result = when {
                s.contains("100% packet loss") -> {
                    -1.0
                }
                else -> {
                    ((s.substringAfter("time=").substringBefore(" ms").trim()
                        .toDouble()))
                }
            }
        } catch (_: Exception) {
            return -1.0
        }
        return result
    }

    /** Looks for the smallest minimum ping in a ping list, except for -1 which equals an undefined ping.
     * We use our own implementation of 'List().min()' because Kotlin's min() will return -1 if it exists
     * in the list, while our implementation should ignore a -1 if found */
    fun List<Int>.minPing(): Int {
        var min = 1000
        for (p in this) {
            if (p > 0) {
                if (p < min) {
                    min = p
                }
            }
        }
        return min
    }



    /** This function is used to print/format the timestamp used in determining video values
     * @param seconds Unix epoch timestamp in seconds
     ***/
    fun timeStamper(seconds: Int): String {
        return if (seconds < 3600) {
            String.format("%02d:%02d", (seconds / 60) % 60, seconds % 60)
        } else {
            String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60)
        }
    }

    /** This is used to generate chat messages' timestamp ready-for-use strings **/
    fun generateTimestamp(): String {
        var s = Timestamp(System.currentTimeMillis()).toString().trim()
        s = s.removeRange(19 until s.length).removeRange(0..10)
        return s
    }

    /** This one converts screen resolution units. DP to PX (Pixel), not used yet */
    fun convertUnit(dp: Float, context: Context?): Float {
        val metrics = context?.resources?.displayMetrics
        return dp * (metrics?.densityDpi?.toFloat()?.div(DisplayMetrics.DENSITY_DEFAULT)!!)
    }


    /** Basically a convenience log function that will log stuff to error pool if it's a debug build **/
    fun loggy(string: String) {
        Log.e("STUFF", string)
    }


    /** Completely revised and working versions for "System UI" manipulators. **/
    @Suppress("DEPRECATION") //We know they're deprecated. Yet they work better than stupid modern functions.
    fun hideSystemUI(activity: Activity, newTrick: Boolean) {
        val window = activity.window
        activity.runOnUiThread {
            if (newTrick) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                }
            } else {
                val decorView: View = window.decorView
                val uiOptions = decorView.systemUiVisibility
                var newUiOptions = uiOptions
                newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_LOW_PROFILE
                newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
                newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE
                newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                decorView.systemUiVisibility = newUiOptions
                View.OnSystemUiVisibilityChangeListener { newmode ->
                    if (newmode != newUiOptions) {
                        hideSystemUI(activity, false)
                    }
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

            }
        }
    }

    @Suppress("DEPRECATION")
    fun showSystemUI(window: Window) {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

    }


    /** A function to control cutout mode **/
    @TargetApi(Build.VERSION_CODES.P)
    fun cutoutMode(enable: Boolean, window: Window) {
        if (enable) {
            window.attributes?.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            window.attributes?.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }
    }

    /** This basically just changes the Status Bar color [Unused]*/
    fun setStatusBarColor(@ColorInt color: Int, window: Window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
    }

    /** Syncplay servers accept passwords in the form of MD5 hashes digested in hexadecimal **/
    fun md5(str: String): ByteArray =
        MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))

    /** Hex Digester for hashers **/
    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    /** Syncplay uses SHA256 hex-digested to hash file names and sizes **/
    fun sha256(str: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(str.toByteArray(UTF_8))
}