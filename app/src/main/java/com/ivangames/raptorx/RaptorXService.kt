package com.ivangames.raptorx

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection

class RaptorXService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var keyboardEn: Keyboard? = null
    private var keyboardRu: Keyboard? = null
    private var isRussian = false
    private var isShifted = false
    private var vibrator: Vibrator? = null
    private var lastSpaceTime: Long = 0

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
        vibrate()

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                // Если кнопка backspace зажата - удаляем слово
                val selected = inputConnection.getSelectedText(0)
                if (selected.isNullOrEmpty()) {
                    inputConnection.deleteSurroundingText(1, 0)
                } else {
                    inputConnection.commitText("", 1)
                }
            }
            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                updateKeyboard()
            }
            -100 -> { // Смена языка
                isRussian = !isRussian
                updateKeyboard()
            }
            -200 -> { // Смайлик
                inputConnection.commitText("😊", 1)
            }
            32 -> { // Пробел
                val now = System.currentTimeMillis()
                if (now - lastSpaceTime < 400) { // Двойной пробел = точка
                    inputConnection.deleteSurroundingText(1, 0)
                    inputConnection.commitText(". ", 1)
                    lastSpaceTime = 0
                } else {
                    inputConnection.commitText(" ", 1)
                    lastSpaceTime = now
                }
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

    // Обработка долгого нажатия
    override fun onLongPress(primaryCode: Int) {
        val inputConnection: InputConnection = currentInputConnection ?: return
        vibrate()
        
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                // Долгое нажатие на backspace = удалить всё
                val text = inputConnection.getTextBeforeCursor(100, 0)
                if (text != null) {
                    inputConnection.deleteSurroundingText(text.length, 0)
                }
            }
            32 -> {
                // Долгое нажатие на пробел = точка с пробелом
                inputConnection.commitText(". ", 1)
            }
            1077 -> { // русская 'е'
                inputConnection.commitText("ё", 1)
            }
            1045 -> { // русская 'Е'
                inputConnection.commitText("Ё", 1)
            }
            1100 -> { // русская 'ь'
                inputConnection.commitText("ъ", 1)
            }
            1068 -> { // русская 'Ь'
                inputConnection.commitText("Ъ", 1)
            }
            46 -> { // точка
                inputConnection.commitText(",", 1)
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
