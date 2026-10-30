package com.zs.toolz.common.impl

import com.zs.preferences.Key
import com.zs.toolz.common.SystemFacade
import com.zs.toolz.settings.SettingsViewState

class SettingsViewModel: KoinViewModel(), SettingsViewState {

    override val isDirty: Boolean
        get() = false
    override var isLabsModeOn: Boolean = false

    override fun commit(facade: SystemFacade) {
        TODO("Not yet implemented")
    }

    override fun discard() {
        TODO("Not yet implemented")
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        TODO("Not yet implemented")
    }
}
