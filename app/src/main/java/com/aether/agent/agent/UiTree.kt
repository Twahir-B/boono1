package com.aether.agent.agent

import android.graphics.Rect
import org.json.JSONArray
import org.json.JSONObject

data class UiElement(
    val id: Int,
    val type: String,
    val text: String?,
    val contentDescription: String?,
    val clickable: Boolean,
    val editable: Boolean,
    val bounds: Rect,
    val packageName: String?
) {
    fun centerX(): Int = bounds.centerX()
    fun centerY(): Int = bounds.centerY()
}

data class UiTree(
    val packageName: String?,
    val elements: List<UiElement>
) {
    /** Compact JSON for the LLM (token-efficient). */
    fun toCompactJson(): String {
        val arr = JSONArray()
        for (e in elements) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("type", e.type)
            e.text?.let { o.put("text", it) }
            e.contentDescription?.let { o.put("desc", it) }
            if (e.clickable) o.put("click", true)
            if (e.editable) o.put("edit", true)
            o.put("x", e.centerX())
            o.put("y", e.centerY())
            o.put("w", e.bounds.width())
            o.put("h", e.bounds.height())
            arr.put(o)
        }
        return JSONObject()
            .put("pkg", packageName)
            .put("elements", arr)
            .toString()
    }
}
