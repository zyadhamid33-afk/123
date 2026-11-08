package com.zyad.crashwatch

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import kotlin.math.sqrt

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("crashwatch", MODE_PRIVATE) }
    private val rounds = mutableListOf<Double>()
    private lateinit var summary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = prefs.getString("rounds", "[]") ?: "[]"
        val array = JSONArray(saved)
        for (i in 0 until array.length()) rounds.add(array.getDouble(i))
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
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
            text = "متابعة إحصائية للمضاعفات المسجلة. التحليل وصفي ولا يتنبأ بنتيجة الجولة القادمة."
            textSize = 15f
            setPadding(0, 12, 0, 20)
        }
        root.addView(note)
        val input = EditText(this).apply {
            hint = "أدخل المضاعف مثل 1.75"
            inputType = 8194
        }
        root.addView(input, ViewGroup.LayoutParams(-1, -2))
        val add = Button(this).apply {
            text = "إضافة نتيجة"
            setOnClickListener {
                val value = input.text.toString().trim().replace(',', '.').toDoubleOrNull()
                if (value == null || value < 1.0 || value > 100000.0) {
                    input.error = "أدخل رقمًا صحيحًا أكبر من أو يساوي 1"
                } else {
                    rounds.add(value)
                    while (rounds.size > 5000) rounds.removeAt(0)
                    save()
                    input.text.clear()
                    updateSummary()
                }
            }
        }
        root.addView(add)
        val clear = Button(this).apply {
            text = "مسح السجل"
            setOnClickListener { rounds.clear(); save(); updateSummary() }
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
            append("عدد النتائج المسجلة: ${rounds.size}\n")
            append("متوسط آخر 100 نتيجة: ${"%.2f".format(mean)}x\n")
            append("الانحراف المعياري: ${"%.2f".format(sqrt(variance))}\n")
            append("نتائج أقل من 2x في آخر 100: $low\n")
            append("متوسط آخر 20 نتيجة: ${"%.2f".format(recent)}x\n\n")
            append("آخر النتائج:\n")
            append(rounds.takeLast(20).asReversed().joinToString("  •  ") { "${"%.2f".format(it)}x" })
            append("\n\nتنبيه: النتائج السابقة لا تكشف بشكل موثوق عن المضاعف القادم في الألعاب العشوائية.")
        }
    }
}
