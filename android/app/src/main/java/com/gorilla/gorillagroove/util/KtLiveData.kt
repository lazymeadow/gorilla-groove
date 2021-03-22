package com.gorilla.gorillagroove.util

import androidx.lifecycle.MutableLiveData

// These overrides aren't redundant. Android Studio is wrong.
// They allow us to use Kotlin's non-null typing because we are overriding inferior Java methods
@Suppress("UNCHECKED_CAST", "RedundantOverride")
class KtLiveData<T>(initialValue: T) : MutableLiveData<T>(initialValue) {
    override fun getValue() = super.getValue() as T
    override fun setValue(value: T) = super.setValue(value)
    override fun postValue(value: T) = super.postValue(value)
}
