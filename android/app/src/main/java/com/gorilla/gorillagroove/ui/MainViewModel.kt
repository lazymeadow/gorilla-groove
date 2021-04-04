package com.gorilla.gorillagroove.ui

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.gorilla.gorillagroove.database.entity.DbTrack
import com.gorilla.gorillagroove.repository.Sort
import com.gorilla.gorillagroove.network.login.LoginRequest
import com.gorilla.gorillagroove.network.track.TrackUpdate
import com.gorilla.gorillagroove.repository.SelectionOperation
import com.gorilla.gorillagroove.util.DataState
import com.gorilla.gorillagroove.util.SessionState
import com.gorilla.gorillagroove.util.StateEvent
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

    private val _updateStatus: MutableLiveData<DataState<*>> = MutableLiveData()

    private val _selectedTrack: MutableLiveData<DataState<out DbTrack>> = MutableLiveData()
    val selectedTrack: LiveData<DataState<out DbTrack>>
        get() = _selectedTrack

    private val _libraryTracks: MutableLiveData<DataState<out List<DbTrack>>> = MutableLiveData()

    private val _nowPlayingTracks: MutableLiveData<List<DbTrack>> = MutableLiveData()

    @ExperimentalCoroutinesApi
    fun setLoginStateEvent(loginStateEvent: LoginStateEvent<LoginRequest>) {
        viewModelScope.launch {
            when (loginStateEvent) {
                is LoginStateEvent.LoginEvent<LoginRequest> -> {
                    mainRepository.getToken(loginStateEvent.data)
                        .onEach {
                            _loginState.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }
                is LoginStateEvent.LogoutEvent -> {
                    mainRepository.logoutUser()
                }
                is LoginStateEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setNowPlayingEvent(nowPlayingEvent: NowPlayingEvent<Nothing>) {
        viewModelScope.launch {
            when (nowPlayingEvent) {
                is NowPlayingEvent.GetNowPlayingTracksEvent -> {
                    mainRepository.getNowPlayingTracks()
                        .onEach {
                            _nowPlayingTracks.postValue(it.data)
//                            //Log.d(TAG, "setNowPlayingEvent: updated track listing!")
                        }
                        .launchIn(viewModelScope)
                }
                is NowPlayingEvent.None -> {
                    //ignored
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun setUpdateEvent(updateEvent: UpdateEvent<TrackUpdate>) {
        viewModelScope.launch {
            when (updateEvent) {
                is UpdateEvent.UpdateTrack<TrackUpdate> -> {
                    mainRepository.updateTrack(updateEvent.data)
                        .onEach {
                            _updateStatus.postValue(it)
                        }
                        .launchIn(viewModelScope)
                }

            }
        }
    }

    fun sortTracks(sorting: Sort) {
        mainRepository.sortLibrary(sorting)
        _libraryTracks.postValue(DataState(mainRepository.allLibraryTracks, StateEvent.Success))
    }

    @ExperimentalCoroutinesApi
    fun setSelectedTracks(trackIds: List<Long>, selectionOperation: SelectionOperation) {
        mainRepository.setSelectedTracks(trackIds, selectionOperation)
//        //Log.d(TAG, "setSelectedTracks: setting new tracks")
        setNowPlayingEvent(NowPlayingEvent.GetNowPlayingTracksEvent)
    }

}

sealed class LoginStateEvent<out R> {
    data class LoginEvent<out T>(val data: T): LoginStateEvent<T>()
    object LogoutEvent : LoginStateEvent<Nothing>()
    object None: LoginStateEvent<Nothing>()
}

sealed class NowPlayingEvent<out R> {
    object GetNowPlayingTracksEvent: NowPlayingEvent<Nothing>()
    object None: NowPlayingEvent<Nothing>()
}

sealed class UpdateEvent<out R> {
    data class UpdateTrack<out T>(val data: T): UpdateEvent<T>()
}
