package com.ivangames.raptorx

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.InputConnection

class RaptorXService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var keyboardEn: Keyboard? = null
    private var keyboardRu: Keyboard? = null
    private var isRussian = false
    private var isShifted = false

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

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection: InputConnection = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> inputConnection.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                updateKeyboard()
            }
            -100 -> { // Кнопка смены языка
                isRussian = !isRussian
                updateKeyboard()
            }
            else -> {
                var code = primaryCode
                if (isShifted && code in 97..122) { // Если английская буква и включен Shift
                    code = code - 32 // Делаем заглавной
                    isShifted = false // Сбрасываем Shift после одной буквы
                    updateKeyboard()
                } else if (isShifted && code in 1072..1103) { // Если русская буква и Shift
                    code = code - 32 // Делаем заглавной
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
