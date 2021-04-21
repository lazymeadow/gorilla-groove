package com.gorilla.gorillagroove.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.databinding.FragmentSettingsBinding
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.util.KtLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val vm: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSettingsBinding.inflate(inflater)
        binding.vm = vm
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Settings view")
    }
}

class SettingsViewModel : ViewModel() {
    var locationEnabled = KtLiveData(GGSettings.locationEnabled)
    val onLocationChanged = { isChecked: Boolean ->
        locationEnabled.value = isChecked
        GGSettings.locationEnabled = isChecked
    }

    var minimumRequiredBattery = KtLiveData("${GGSettings.locationMinimumBattery}%")
    val onMinimumBatteryChanged = { newMinimum: String ->
        val validatedPercent = max(0.0, min(100.0, newMinimum.toDouble())).toInt()
        minimumRequiredBattery.value = "$validatedPercent%"
        GGSettings.locationMinimumBattery = validatedPercent
    }
}
