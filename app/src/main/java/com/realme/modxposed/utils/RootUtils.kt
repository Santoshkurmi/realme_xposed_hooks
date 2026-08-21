package com.realme.modxposed.utils

import android.content.Context
import android.widget.Toast

object RootUtils {

    fun killAndRestartPackage(context: Context, packageName: String, appName: String) {
        Thread {
            try {
                val cmd = if ("com.android.systemui".equals(packageName, ignoreCase = true)) {
                    "pkill -f com.android.systemui"
                } else {
                    "am force-stop $packageName"
                }

                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val exitCode = process.waitFor()

                (context as? android.app.Activity)?.runOnUiThread {
                    if (exitCode == 0) {
                        Toast.makeText(context, "Restarted $appName ($packageName)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Root command sent for $appName", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (t: Throwable) {
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Root Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
