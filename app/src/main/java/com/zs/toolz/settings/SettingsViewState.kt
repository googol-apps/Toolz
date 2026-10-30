package com.zs.toolz.settings

import com.zs.preferences.Key
import com.zs.toolz.common.SystemFacade

interface SettingsViewState {

    /**
     * State variable that tracks if the settings has uncommited changes.
     */
    val isDirty: Boolean

    var isLabsModeOn: Boolean

    /**
     * Commits [com.zs.toolz.common.AppConfig] to memory.
     */
    fun commit(facade: SystemFacade)

    /**
     * Discard the proposed changes to [com.zs.toolz.common.AppConfig]
     */
    fun discard()

    /** Sets the value of the given [key] to [value]. */
    fun <S, O> set(key: Key<S, O>, value: O)
}