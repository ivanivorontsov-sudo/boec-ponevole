package org.ivanivorontsov.boec

import android.app.Application
import org.ivanivorontsov.boec.alarm.Notifier
import org.ivanivorontsov.boec.data.Store

class BoecApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Notifier.createChannels(this)
    }
}
