package com.dangersense.app.service

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.dangersense.app.domain.MonitorController

class MonitorTileService : TileService() {

    private fun controller() = MonitorController.get(applicationContext)

    override fun onStartListening() {
        super.onStartListening()
        refresh(controller().enabled.value)
    }

    override fun onClick() {
        super.onClick()
        val c = controller()
        c.toggle()
        refresh(c.enabled.value)
    }

    private fun refresh(on: Boolean) {
        qsTile?.apply {
            state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (on) "Ativo" else "Pausado"
            updateTile()
        }
    }
}
