package org.mediadownloader.ui.main.settings

import org.mediadownloader.data.remote.model.CobaltInfo

sealed class ServerInfoState {
    object Idle : ServerInfoState()
    object Loading : ServerInfoState()
    data class Success(val info: CobaltInfo) : ServerInfoState()
    data class Error(val message: String) : ServerInfoState()
}
