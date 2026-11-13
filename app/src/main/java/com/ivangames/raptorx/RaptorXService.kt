package com.ivangames.raptorx

import android.inputmethodservice.InputMethodService
import android.view.View

class RaptorXService : InputMethodService() {

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard_view, null)
    }
}
