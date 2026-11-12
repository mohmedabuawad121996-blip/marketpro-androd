package com.marketpro.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** عميل الاتصال بخادم الكمبيوتر (المرحلة 1: قراءة فقط) */
class Api(baseInput: String) {
    private val base: String = if (baseInput.startsWith("http")) baseInput.trimEnd('/') else "http://" + baseInput.trimEnd('/')
    private var cookie: String = ""

    /** تسجيل الدخول بكلمة مرور الشبكة. يرجع null عند النجاح أو رسالة الخطأ */
    fun login(pin: String): String? {
        val conn = URL("$base/login").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.instanceFollowRedirects = false
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
            it.write("pin=" + URLEncoder.encode(pin, "UTF-8"))
        }
        val code = conn.responseCode
        val setCookie = conn.getHeaderField("Set-Cookie") ?: ""
        conn.disconnect()
        if (code == 303 && setCookie.startsWith("mp=")) {
            cookie = setCookie.substringBefore(";")
            return null
        }
        return "كلمة المرور غير صحيحة أو الخادم غير متاح"
    }

    fun products(): JSONArray = getArray("/api/products")

    fun parties(kind: Int): JSONArray = getArray("/api/parties?kind=$kind")

    private fun getArray(path: String): JSONArray {
        val conn = URL(base + path).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 15000
        conn.setRequestProperty("Cookie", cookie)
        conn.instanceFollowRedirects = false
        val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        conn.disconnect()
        return JSONArray(text)
    }
}

/** صنف مبسّط للعرض */
data class ProductItem(val name: String, val barcode: String, val price: Double, val qty: Double, val unit: String)

/** عميل أو مورد مبسّط للعرض */
data class PartyItem(val name: String, val phone: String, val opening: Double)

fun JSONArray.toProducts(): List<ProductItem> = (0 until length()).map {
    val o: JSONObject = getJSONObject(it)
    ProductItem(o.optString("name"), o.optString("barcode"), o.optDouble("price"), o.optDouble("qty"), o.optString("unit"))
}

fun JSONArray.toParties(): List<PartyItem> = (0 until length()).map {
    val o: JSONObject = getJSONObject(it)
    PartyItem(o.optString("name"), o.optString("phone"), o.optDouble("opening"))
}
