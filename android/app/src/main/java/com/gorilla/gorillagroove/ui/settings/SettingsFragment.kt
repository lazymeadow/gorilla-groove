package com.gorilla.gorillagroove.ui.settings

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.database.GorillaDatabase
import com.gorilla.gorillagroove.database.entity.OfflineAvailabilityType
import com.gorilla.gorillagroove.databinding.FragmentSettingsBinding
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGSettings
import com.gorilla.gorillagroove.service.MarkListenedService
import com.gorilla.gorillagroove.service.OfflineStorageMode
import com.gorilla.gorillagroove.service.sync.ServerSynchronizer
import com.gorilla.gorillagroove.ui.ChangeType
import com.gorilla.gorillagroove.ui.OfflineModeService
import com.gorilla.gorillagroove.ui.TrackCacheEvent
import com.gorilla.gorillagroove.util.KtLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.max
import kotlin.math.min

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val vm: SettingsViewModel by viewModels()

    private val trackDao get() = GorillaDatabase.trackDao

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSettingsBinding.inflate(inflater)
        binding.vm = vm
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logInfo("Loading Settings view")
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val storageUsed = trackDao.getCachedTrackSizeBytes()

            vm.alwaysOfflineTracksCachedTotal = trackDao.getTrackCount(offlineAvailabilityType = OfflineAvailabilityType.AVAILABLE_OFFLINE, isCached = null)
            vm.alwaysOfflineTracksCachedCurrent = trackDao.getTrackCount(offlineAvailabilityType = OfflineAvailabilityType.AVAILABLE_OFFLINE, isCached = true)

            val tracksTemporarilyCached = trackDao.getTrackCount(offlineAvailabilityType = OfflineAvailabilityType.NORMAL, isCached = true)

            withContext(Dispatchers.Main) {
                vm.offlineStorageUsedRaw = storageUsed
                vm.offlineStorageUsed.value = storageUsed.toReadableByteString()
                vm.alwaysOfflineTracksCached.value = "${vm.alwaysOfflineTracksCachedCurrent} / ${vm.alwaysOfflineTracksCachedTotal}"
                vm.tracksTemporarilyCached.value = tracksTemporarilyCached
            }
        }
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    @Synchronized
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTrackCacheEvent(event: TrackCacheEvent) {
        when (event.changeType) {
            ChangeType.ADDED -> {
                if (event.offlineAvailabilityType == OfflineAvailabilityType.NORMAL) {
                    vm.tracksTemporarilyCached.value = vm.tracksTemporarilyCached.value + 1
                } else if (event.offlineAvailabilityType == OfflineAvailabilityType.AVAILABLE_OFFLINE) {
                    vm.alwaysOfflineTracksCachedCurrent = vm.alwaysOfflineTracksCachedCurrent + 1
                    vm.alwaysOfflineTracksCached.value = "${vm.alwaysOfflineTracksCachedCurrent} / ${vm.alwaysOfflineTracksCachedTotal}"
                }
            }
            ChangeType.UPDATED -> { /* Nothing specific to do here */ }
            ChangeType.DELETED -> {
                if (event.offlineAvailabilityType == OfflineAvailabilityType.NORMAL) {
                    vm.tracksTemporarilyCached.value = vm.tracksTemporarilyCached.value - 1
                } else if (event.offlineAvailabilityType == OfflineAvailabilityType.AVAILABLE_OFFLINE) {
                    vm.alwaysOfflineTracksCachedCurrent = vm.alwaysOfflineTracksCachedCurrent - 1
                    vm.alwaysOfflineTracksCached.value = "${vm.alwaysOfflineTracksCachedCurrent} / ${vm.alwaysOfflineTracksCachedTotal}"
                }
            }
        }

        vm.offlineStorageUsedRaw = vm.offlineStorageUsedRaw + event.bytesChanged // event.bytesChanged is negative if it's deleted so no need to subtract
        vm.offlineStorageUsed.value = vm.offlineStorageUsedRaw.toReadableByteString()
    }
}

class SettingsViewModel : ViewModel() {
    var offlineModeEnabled = KtLiveData(GGSettings.offlineModeEnabled)
    val onOfflineModeChanged = { isChecked: Boolean ->
        offlineModeEnabled.value = isChecked
        GGSettings.offlineModeEnabled = isChecked

        if (!isChecked) {
            GlobalScope.launch {
                ServerSynchronizer.syncWithServer(abortIfRecentlySynced = false)

                MarkListenedService.sendAndClearFailedListens()
            }
        }
    }

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

    var offlineStorageEnabled = KtLiveData(GGSettings.offlineStorageEnabled)
    val onOfflineStorageEnabledChanged = { isChecked: Boolean ->
        offlineStorageEnabled.value = isChecked
        GGSettings.offlineStorageEnabled = isChecked

        if (isChecked) {
            GlobalScope.launch {
                OfflineModeService.downloadAlwaysOfflineTracks()
            }
        }
    }

    var offlineDownloadCondition = KtLiveData(GGSettings.offlineStorageMode)
    val offlineDownloadOptions = linkedMapOf(
        OfflineStorageMode.ALWAYS.displayName to (offlineDownloadCondition.value == OfflineStorageMode.ALWAYS),
        OfflineStorageMode.WIFI.displayName to (offlineDownloadCondition.value == OfflineStorageMode.WIFI),
        OfflineStorageMode.NEVER.displayName to (offlineDownloadCondition.value == OfflineStorageMode.NEVER),
    )
    val onOfflineDownloadConditionPicked = { newOptionDisplayName: String ->
        val newOption = OfflineStorageMode.findByDisplayName(newOptionDisplayName)
        offlineDownloadCondition.value = newOption

        offlineDownloadOptions.forEach { (option, _) -> offlineDownloadOptions[option] = false }
        offlineDownloadOptions[newOptionDisplayName] = true

        GGSettings.offlineStorageMode = newOption
    }

    var maximumOfflineStorage = KtLiveData(GGSettings.maximumOfflineStorageBytes.value.toReadableByteString())
    val onMaximumOfflineStorageChanged: (String) -> Unit = { newMaximumGb: String ->
        val bytes = (newMaximumGb.toDouble() * 1_000_000_000L).toLong()

        val oldStorage = maximumOfflineStorage.value
        maximumOfflineStorage.value = bytes.toReadableByteString()
        GGSettings.maximumOfflineStorageBytes.value = bytes

        logInfo("User changed their offline storage from $oldStorage to ${bytes.toReadableByteString()}")
    }

    var offlineStorageUsedRaw = 0L
    var offlineStorageUsed = KtLiveData(offlineStorageUsedRaw.toReadableByteString())

    var alwaysOfflineTracksCachedTotal = 0
    var alwaysOfflineTracksCachedCurrent = 0

    var alwaysOfflineTracksCached = KtLiveData("$alwaysOfflineTracksCachedTotal / $alwaysOfflineTracksCachedCurrent")
    var tracksTemporarilyCached = KtLiveData(0)

    var automaticErrorReporting = KtLiveData(GGSettings.automaticErrorReportingEnabled)
    val onAutomaticErrorReportingChanged = { isChecked: Boolean ->
        automaticErrorReporting.value = isChecked
        GGSettings.automaticErrorReportingEnabled = isChecked
    }

    var showCriticalErrors = KtLiveData(GGSettings.showCriticalErrorsEnabled)
    val onShowCriticalErrorsChanged = { isChecked: Boolean ->
        showCriticalErrors.value = isChecked
        GGSettings.showCriticalErrorsEnabled = isChecked
    }

    init {
        viewModelScope.launch {
            GGSettings.maximumOfflineStorageBytes.collect { newStorageBytes ->
                maximumOfflineStorage.value = newStorageBytes.toReadableByteString()
            }
        }
    }
}

fun Long.toReadableByteString(): String = Formatter.formatShortFileSize(GGApplication.application, this)
