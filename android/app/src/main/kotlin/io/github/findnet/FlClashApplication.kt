package io.github.findnet

import android.app.Application
import android.content.Context
import io.github.findnet.common.GlobalState

class FlClashApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        GlobalState.init(this)
    }
}
