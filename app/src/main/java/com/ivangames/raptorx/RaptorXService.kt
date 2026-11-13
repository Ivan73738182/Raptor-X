package com.ivangames.raptorx

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputConnection

class RaptorXService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var keyboardEn: Keyboard? = null
    private var keyboardRu: Keyboard? = null
    private var isRussian = false
    private var isShifted = false
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view.findViewById(R.id.keyboard_view)

        keyboardEn = Keyboard(this, R.xml.qwerty)
        keyboardRu = Keyboard(this, R.xml.qwerty_ru)
        
        keyboardView?.keyboard = keyboardEn
        keyboardView?.setOnKeyboardActionListener(this)

        return view
    }

    private fun updateKeyboard() {
        val kb = if (isRussian) keyboardRu else keyboardEn
        keyboardView?.keyboard = kb
        if (isShifted) {
            kb?.isShifted = true
            keyboardView?.setShifted(true)
        } else {
            kb?.isShifted = false
            keyboardView?.setShifted(false)
        }
    }

    private fun vibrate() {
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(20)
            }
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection: InputConnection = currentInputConnection ?: return
        vibrate() // Вибрация при каждом нажатии

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> inputConnection.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                updateKeyboard()
            }
            -100 -> { // Переключение языка
                isRussian = !isRussian
                updateKeyboard()
            }
            -200 -> { // Смайлик
                inputConnection.commitText("😊", 1)
            }
            else -> {
                var code = primaryCode
                if (isShifted && code in 97..122) {
                    code = code - 32
                    isShifted = false
                    updateKeyboard()
                } else if (isShifted && code in 1072..1103) {
                    code = code - 32
                    isShifted = false
                    updateKeyboard()
                }
                inputConnection.commitText(code.toChar().toString(), 1)
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
