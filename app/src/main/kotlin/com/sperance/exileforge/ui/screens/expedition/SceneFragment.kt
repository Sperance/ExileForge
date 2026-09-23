package com.sperance.exileforge.ui.screens.expedition

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.sperance.exileforge.ui.screens.expedition.gdx.ExpeditionScene

/**
 * libGDX's own fragment, hosting the campaign's scene inside the Compose tree.
 *
 * The scene takes its run from [com.sperance.exileforge.ui.screens.expedition.gdx.SceneHost]:
 * a fragment is rebuilt by the system with no arguments but its class, so a run cannot be passed
 * in. No sensor is asked for — the stick is the overlay's — and the screen stays on while a run does.
 */
class SceneFragment : AndroidFragmentApplication() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        initializeForView(ExpeditionScene(), AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useWakelock = true
            disableAudio = true
            numSamples = 2
        })
}
