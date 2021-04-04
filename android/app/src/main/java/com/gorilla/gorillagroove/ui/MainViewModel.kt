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

    private val _selectedTrack: MutableLiveData<DataState<out DbTrack>> = MutableLiveData()
    val selectedTrack: LiveData<DataState<out DbTrack>>
        get() = _selectedTrack

    fun setSelectedTracks(tracks: List<DbTrack>, selectionOperation: SelectionOperation) {
        mainRepository.setSelectedTracks(tracks, selectionOperation)
    }
}
