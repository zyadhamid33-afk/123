package com.zyad.crashwatch

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import kotlin.math.sqrt

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("crashwatch", MODE_PRIVATE) }
    private val rounds = mutableListOf<Double>()
    private lateinit var summary: TextView
    private lateinit var status: TextView
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = prefs.getString("rounds", "[]") ?: "[]"
        val array = JSONArray(saved)
        for (i in 0 until array.length()) rounds.add(array.getDouble(i))
        render()
    }

    private fun render() {
        scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFFF4F6F8.toInt())
        }
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "CrashWatch - زياد"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF14213D.toInt())
        }
        root.addView(title)

        val note = TextView(this).apply {
            text = "سجّل المضاعفات لمتابعة الإحصائيات. التحليل وصفي ولا يتنبأ بنتيجة الجولة القادمة."
            textSize = 15f
            setPadding(0, 12, 0, 20)
        }
        root.addView(note)

        val input = EditText(this).apply {
            hint = "أدخل المضاعف مثل 1.75"
            inputType = 8194
            singleLine = true
        }
        root.addView(input, ViewGroup.LayoutParams(-1, -2))

        status = TextView(this).apply {
            text = "أدخل مضاعفًا ثم اضغط إضافة نتيجة."
            textSize = 14f
            setTextColor(0xFF36506C.toInt())
            setPadding(0, 8, 0, 8)
        }
        root.addView(status)

        val add = Button(this).apply {
            text = "إضافة نتيجة وتحليل"
            setOnClickListener {
                val raw = input.text.toString().trim()
                    .replace(',', '.')
                    .replace('٫', '.')
                    .replace('٠', '0').replace('١', '1').replace('٢', '2')
                    .replace('٣', '3').replace('٤', '4').replace('٥', '5')
                    .replace('٦', '6').replace('٧', '7').replace('٨', '8')
                    .replace('٩', '9')
                val value = raw.toDoubleOrNull()
                if (value == null || value < 1.0 || value > 100000.0) {
                    input.error = "أدخل رقمًا صحيحًا مثل 1.75 أو 2.30"
                    status.text = "لم تُسجّل النتيجة. راجع الرقم المدخل."
                } else {
                    rounds.add(value)
                    while (rounds.size > 5000) rounds.removeAt(0)
                    save()
                    input.text.clear()
                    input.clearFocus()
                    updateSummary()
                    status.text = "تم تسجيل " + "%.2f".format(value) + "x بنجاح. إجمالي النتائج: " + rounds.size
                    Toast.makeText(this@MainActivity, "تم تسجيل المضاعف بنجاح", Toast.LENGTH_SHORT).show()
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }
        root.addView(add)

        val clear = Button(this).apply {
            text = "مسح السجل"
            setOnClickListener {
                rounds.clear()
                save()
                updateSummary()
                status.text = "تم مسح سجل النتائج."
            }
        }
        root.addView(clear)

        summary = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF172033.toInt())
            setPadding(0, 24, 0, 12)
        }
        root.addView(summary)

        setContentView(scroll)
        updateSummary()
    }

    private fun save() {
        val array = JSONArray()
        rounds.forEach { array.put(it) }
        prefs.edit().putString("rounds", array.toString()).apply()
    }

    private fun updateSummary() {
        val last20 = rounds.takeLast(20)
        val last100 = rounds.takeLast(100)
        val mean = if (last100.isNotEmpty()) last100.average() else 0.0
        val variance = if (last100.isNotEmpty()) last100.map { (it - mean) * (it - mean) }.average() else 0.0
        val low = last100.count { it < 2.0 }
        val recent = if (last20.isNotEmpty()) last20.average() else 0.0
        summary.text = buildString {
            append("التحليل الإحصائي\n")
            append("عدد النتائج المسجلة: " + rounds.size + "\n")
            append("متوسط آخر 100 نتيجة: " + "%.2f".format(mean) + "x\n")
            append("الانحراف المعياري: " + "%.2f".format(sqrt(variance)) + "\n")
            append("نتائج أقل من 2x في آخر 100: " + low + "\n")
            append("متوسط آخر 20 نتيجة: " + "%.2f".format(recent) + "x\n\n")
            append("آخر النتائج (الأحدث أولًا):\n")
            append(if (last20.isEmpty()) "لا توجد نتائج مسجلة بعد." else last20.asReversed().joinToString("\n") { "• " + "%.2f".format(it) + "x" })
            append("\n\nتنبيه: النتائج السابقة لا تكشف بشكل موثوق عن المضاعف القادم في الألعاب العشوائية.")
        }
    }
}
