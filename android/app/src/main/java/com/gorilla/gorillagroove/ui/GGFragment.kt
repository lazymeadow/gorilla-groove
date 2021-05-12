package com.gorilla.gorillagroove.ui

import androidx.fragment.app.Fragment

abstract class GGFragment(resourceId: Int) : Fragment(resourceId) {
    /**
     * Could handle back press.
     * @return true if back press was handled
     */
    open fun onBackPressed(): Boolean {
        return false
    }
}
