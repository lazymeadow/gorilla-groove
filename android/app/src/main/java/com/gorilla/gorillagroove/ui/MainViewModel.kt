package com.gorilla.gorillagroove.ui

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.util.DataState
import com.gorilla.gorillagroove.util.SessionState
import kotlinx.coroutines.*


class MainViewModel
@ViewModelInject
constructor(
    private val mainRepository: MainRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _loginState: MutableLiveData<SessionState<*>> = MutableLiveData()
    val loginState: LiveData<SessionState<*>>
        get() = _loginState

    private val _selectedTrack: MutableLiveData<DataState<out DbTrack>> = MutableLiveData()
    val selectedTrack: LiveData<DataState<out DbTrack>>
        get() = _selectedTrack

    private val _nowPlayingTracks: MutableLiveData<List<DbTrack>> = MutableLiveData()

    @ExperimentalCoroutinesApi
    fun setSelectedTracks(trackIds: List<Long>, selectionOperation: SelectionOperation) {
        mainRepository.setSelectedTracks(trackIds, selectionOperation)
    }
}

sealed class NowPlayingEvent<out R> {
    object GetNowPlayingTracksEvent: NowPlayingEvent<Nothing>()
    object None: NowPlayingEvent<Nothing>()
}

sealed class UpdateEvent<out R> {
    data class UpdateTrack<out T>(val data: T): UpdateEvent<T>()
}
