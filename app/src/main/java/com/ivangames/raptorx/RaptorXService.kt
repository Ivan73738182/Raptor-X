package com.ivangames.raptorx

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.GridView

class RaptorXService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var keyboardEn: Keyboard? = null
    private var keyboardRu: Keyboard? = null
    private var keyboardEnAction: Keyboard? = null
    private var keyboardRuAction: Keyboard? = null
    
    private var isRussian = false
    private var isShifted = false
    private var vibrator: Vibrator? = null
    private var lastSpaceTime: Long = 0
    private var emojiGrid: GridView? = null
    private var isEmojiMode = false
    private var isActionKey = false // Нужно ли менять Enter на действие

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view.findViewById(R.id.keyboard_view)
        emojiGrid = view.findViewById(R.id.emoji_grid)

        keyboardEn = Keyboard(this, R.xml.qwerty)
        keyboardRu = Keyboard(this, R.xml.qwerty_ru)
        keyboardEnAction = Keyboard(this, R.xml.qwerty_action)
        keyboardRuAction = Keyboard(this, R.xml.qwerty_ru_action)
        
        keyboardView?.setOnKeyboardActionListener(this)

        // Настраиваем панель эмодзи
        val emojis = resources.getStringArray(R.array.emojis)
        val adapter = EmojiAdapter(this, emojis)
        emojiGrid?.adapter = adapter
        emojiGrid?.setOnItemClickListener { _, _, position, _ ->
            vibrate()
            currentInputConnection?.commitText(emojis[position], 1)
        }

        updateKeyboard()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Проверяем, куда мы печатаем
        if (info != null) {
            val inputType = info.inputType and InputType.TYPE_MASK_CLASS
            val imeOptions = info.imeOptions and EditorInfo.IME_MASK_ACTION
            
            // Если это поиск или отправка сообщения
            isActionKey = (imeOptions == EditorInfo.IME_ACTION_SEARCH || 
                           imeOptions == EditorInfo.IME_ACTION_SEND || 
                           imeOptions == EditorInfo.IME_ACTION_GO)
            
            updateKeyboard()
            
            // Меняем надпись на кнопке Enter
            val enterKey = keyboardView?.keyboard?.keys?.find { it.codes[0] == -4 }
            if (enterKey != null) {
                enterKey.label = when (imeOptions) {
                    EditorInfo.IME_ACTION_SEARCH -> "🔍"
                    EditorInfo.IME_ACTION_SEND -> "➤"
                    EditorInfo.IME_ACTION_GO -> "➔"
                    else -> "Enter"
                }
            }
            keyboardView?.invalidateAllKeys()
        }
    }

    private fun updateKeyboard() {
        val kb = if (isRussian) {
            if (isActionKey) keyboardRuAction else keyboardRu
        } else {
            if (isActionKey) keyboardEnAction else keyboardEn
        }
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

    private fun toggleEmojiPanel() {
        isEmojiMode = !isEmojiMode
        if (isEmojiMode) {
            emojiGrid?.visibility = View.VISIBLE
            keyboardView?.visibility = View.GONE
        } else {
            emojiGrid?.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection: InputConnection = currentInputConnection ?: return
        vibrate()

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> inputConnection.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT -> {
                isShifted = !isShifted
                updateKeyboard()
            }
            -100 -> { // Смена языка
                isRussian = !isRussian
                updateKeyboard()
            }
            -200 -> toggleEmojiPanel()
            32 -> {
                val now = System.currentTimeMillis()
                if (now - lastSpaceTime < 400) {
                    inputConnection.deleteSurroundingText(1, 0)
                    inputConnection.commitText(". ", 1)
                    lastSpaceTime = 0
                } else {
                    inputConnection.commitText(" ", 1)
                    lastSpaceTime = now
                }
            }
            -4 -> { // Умный Enter
                if (isActionKey) {
                    // Отправляем действие приложению (поиск / отправка)
                    inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                } else {
                    // Обычный перенос строки
                    inputConnection.commitText("\n", 1)
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

    override fun onLongPress(primaryCode: Int) {
        val inputConnection: InputConnection = currentInputConnection ?: return
        vibrate()
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                val text = inputConnection.getTextBeforeCursor(100, 0)
                if (text != null) inputConnection.deleteSurroundingText(text.length, 0)
            }
            32 -> inputConnection.commitText(". ", 1)
            1077 -> inputConnection.commitText("ё", 1)
            1045 -> inputConnection.commitText("Ё", 1)
            1100 -> inputConnection.commitText("ъ", 1)
            1068 -> inputConnection.commitText("Ъ", 1)
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
