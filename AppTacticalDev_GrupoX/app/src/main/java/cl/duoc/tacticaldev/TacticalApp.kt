package cl.duoc.tacticaldev

import android.app.Application

class TacticalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
