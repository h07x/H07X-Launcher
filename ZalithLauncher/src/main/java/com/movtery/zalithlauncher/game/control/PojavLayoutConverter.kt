/*
 * HX Launcher
 * Copyright (C) 2026 h07x
 *
 * Based on Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.control

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.UUID
import kotlin.math.max

/**
 * HX Launcher: convierte layouts de controles en formato PojavLauncher
 * (el que usan PojavLauncher, MojoLauncher y la mayoría de forks de Pojav)
 * al formato nativo de este launcher.
 *
 * Es una conversión "mejor esfuerzo": botones normales (posición, tamaño, teclas,
 * toggle, swipe, penetrable, visibilidad en juego/menú). Se omiten cajones (drawers),
 * joysticks y estilos visuales personalizados.
 */
object PojavLayoutConverter {
    private const val REF_WIDTH_DP = 800.0
    private const val REF_HEIGHT_DP = 360.0
    private const val MARGIN_DP = 2.0
    private const val EDITOR_VERSION = 12

    private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }

    /** ¿El JSON parece un layout de PojavLauncher (o derivado)? */
    fun looksLikePojav(jsonString: String): Boolean = runCatching {
        val obj = lenient.parseToJsonElement(jsonString).jsonObject
        obj["editorVersion"] == null && obj["mControlDataList"] != null
    }.getOrDefault(false)

    /** Convierte a un JSON en formato nativo (editorVersion 12). Lanza excepción si el formato no es válido. */
    fun convert(jsonString: String, layoutName: String = "Imported layout"): String {
        val root = lenient.parseToJsonElement(jsonString).jsonObject
        val list = root["mControlDataList"]?.jsonArray ?: JsonArray(emptyList())

        val buttons = list.mapNotNull { (it as? JsonObject)?.let(::convertButton) }

        val layer = buildJsonObject {
            put("name", "imported")
            put("uuid", newId())
            put("hide", false)
            put("visibilityType", "always")
            put("normalButtons", JsonArray(buttons))
        }

        val result = buildJsonObject {
            putJsonObject("info") {
                put("name", translatable(layoutName))
                put("author", translatable(""))
                put("description", translatable("Imported from a PojavLauncher-format layout"))
                put("versionCode", 0)
                put("versionName", "")
            }
            putJsonArray("layers") { add(layer) }
            putJsonArray("styles") {}
            putJsonArray("joystickStyles") {}
            put("editorVersion", EDITOR_VERSION)
        }
        return result.toString()
    }

    private fun translatable(text: String): JsonElement = buildJsonObject {
        put("default", text)
        putJsonArray("matchQueue") {}
    }

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(18)

    private fun convertButton(obj: JsonObject): JsonObject? {
        val width = (obj["width"]?.num() ?: 50.0).coerceIn(5.0, 1000.0)
        val height = (obj["height"]?.num() ?: 50.0).coerceIn(5.0, 1000.0)

        val events = obj["keycodes"]?.jsonArray
            ?.mapNotNull { it.num()?.toInt() }
            ?.filter { it != 0 }
            ?.mapNotNull(::toClickEvent)
            ?.distinct()
            .orEmpty()
        if (events.isEmpty()) return null //botones sin acción válida (p. ej. toggle de controles)

        val isDynamic = obj["isDynamicBtn"]?.bool() ?: false
        val x = evalPosition(obj["dynamicX"]?.str(), width, height, isDynamic)
        val y = evalPosition(obj["dynamicY"]?.str(), width, height, isDynamic)

        val xPct = pct(x, REF_WIDTH_DP - width)
        val yPct = pct(y, REF_HEIGHT_DP - height)

        val inGame = obj["displayInGame"]?.bool() ?: true
        val inMenu = obj["displayInMenu"]?.bool() ?: true
        val visibility = when {
            inGame && !inMenu -> "in_game"
            !inGame && inMenu -> "in_menu"
            else -> "always"
        }

        return buildJsonObject {
            put("text", translatable(obj["name"]?.str().orEmpty()))
            put("uuid", newId())
            putJsonObject("position") {
                put("x", xPct)
                put("y", yPct)
            }
            putJsonObject("buttonSize") {
                put("type", "dp")
                put("widthDp", width)
                put("heightDp", height)
                put("widthPercentage", 1000)
                put("heightPercentage", 1000)
                put("widthReference", "screen_height")
                put("heightReference", "screen_height")
            }
            put("visibilityType", visibility)
            putJsonArray("clickEvents") { events.forEach { add(it) } }
            put("isSwipple", obj["isSwipeable"]?.bool() ?: false)
            put("isPenetrable", obj["passThruEnabled"]?.bool() ?: false)
            put("isToggleable", obj["isToggle"]?.bool() ?: false)
        }
    }

    private fun pct(value: Double, range: Double): Int {
        if (range <= 1.0) return 0
        return ((value / range) * 10000.0).toInt().coerceIn(0, 10000)
    }

    /** Códigos negativos = botones especiales de Pojav */
    private fun toClickEvent(code: Int): JsonObject? {
        fun key(name: String) = buildJsonObject { put("type", "key"); put("key", name) }
        fun launcher(name: String) = buildJsonObject { put("type", "launcher_event"); put("key", name) }
        return when (code) {
            -1 -> launcher("launcher.event.switch_ime")
            -3 -> key("GLFW_MOUSE_BUTTON_LEFT")
            -4 -> key("GLFW_MOUSE_BUTTON_RIGHT")
            -5 -> key("GLFW_MOUSE_BUTTON_MIDDLE")
            -6 -> launcher("launcher.event.scroll_up.single")
            -7 -> launcher("launcher.event.scroll_down.single")
            -8 -> launcher("launcher.event.switch_menu")
            else -> if (code > 0) KEY_NAMES[code]?.let(::key) else null
        }
    }

    // ---- Evaluador de expresiones de posición de Pojav (+ - * / paréntesis, ${variables}) ----

    private fun evalPosition(expr: String?, width: Double, height: Double, dynamic: Boolean): Double {
        if (expr.isNullOrBlank()) return 0.0
        return runCatching {
            Parser(expr, width, height).parse()
        }.getOrDefault(0.0).let { if (it.isNaN() || it.isInfinite()) 0.0 else it }
    }

    private class Parser(private val src: String, private val w: Double, private val h: Double) {
        private var i = 0

        fun parse(): Double {
            val v = expr()
            return v
        }

        private fun peek(): Char? {
            while (i < src.length && src[i].isWhitespace()) i++
            return src.getOrNull(i)
        }

        private fun expr(): Double {
            var v = term()
            while (true) {
                when (peek()) {
                    '+' -> { i++; v += term() }
                    '-' -> { i++; v -= term() }
                    else -> return v
                }
            }
        }

        private fun term(): Double {
            var v = factor()
            while (true) {
                when (peek()) {
                    '*' -> { i++; v *= factor() }
                    '/' -> { i++; val d = factor(); v = if (d == 0.0) 0.0 else v / d }
                    else -> return v
                }
            }
        }

        private fun factor(): Double {
            when (val c = peek()) {
                '-' -> { i++; return -factor() }
                '+' -> { i++; return factor() }
                '(' -> {
                    i++
                    val v = expr()
                    if (peek() == ')') i++
                    return v
                }
                '$' -> {
                    val end = src.indexOf('}', i)
                    val name = src.substring(i + 2, end)
                    i = end + 1
                    return variable(name)
                }
                else -> {
                    if (c == null) return 0.0
                    val start = i
                    while (i < src.length && (src[i].isDigit() || src[i] == '.')) i++
                    if (start == i) throw IllegalArgumentException("Unexpected '$c'")
                    return src.substring(start, i).toDouble()
                }
            }
        }

        private fun variable(name: String): Double = when (name) {
            "screen_width" -> REF_WIDTH_DP
            "screen_height" -> REF_HEIGHT_DP
            "margin" -> MARGIN_DP
            "preferred_scale" -> 1.0
            "width" -> w
            "height" -> h
            "right" -> REF_WIDTH_DP - w
            "bottom" -> REF_HEIGHT_DP - h
            "top" -> 0.0
            "left" -> 0.0
            else -> 0.0
        }
    }

    private fun JsonElement.num(): Double? = (this as? JsonPrimitive)?.doubleOrNull
    private fun JsonElement.str(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
    private fun JsonElement.bool(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

    /** Código GLFW -> nombre de tecla del formato nativo */
    private val KEY_NAMES: Map<Int, String> = mapOf(
        32 to "GLFW_KEY_SPACE",
        39 to "GLFW_KEY_APOSTROPHE",
        44 to "GLFW_KEY_COMMA",
        45 to "GLFW_KEY_MINUS",
        46 to "GLFW_KEY_PERIOD",
        47 to "GLFW_KEY_SLASH",
        48 to "GLFW_KEY_0",
        49 to "GLFW_KEY_1",
        50 to "GLFW_KEY_2",
        51 to "GLFW_KEY_3",
        52 to "GLFW_KEY_4",
        53 to "GLFW_KEY_5",
        54 to "GLFW_KEY_6",
        55 to "GLFW_KEY_7",
        56 to "GLFW_KEY_8",
        57 to "GLFW_KEY_9",
        59 to "GLFW_KEY_SEMICOLON",
        61 to "GLFW_KEY_EQUAL",
        65 to "GLFW_KEY_A",
        66 to "GLFW_KEY_B",
        67 to "GLFW_KEY_C",
        68 to "GLFW_KEY_D",
        69 to "GLFW_KEY_E",
        70 to "GLFW_KEY_F",
        71 to "GLFW_KEY_G",
        72 to "GLFW_KEY_H",
        73 to "GLFW_KEY_I",
        74 to "GLFW_KEY_J",
        75 to "GLFW_KEY_K",
        76 to "GLFW_KEY_L",
        77 to "GLFW_KEY_M",
        78 to "GLFW_KEY_N",
        79 to "GLFW_KEY_O",
        80 to "GLFW_KEY_P",
        81 to "GLFW_KEY_Q",
        82 to "GLFW_KEY_R",
        83 to "GLFW_KEY_S",
        84 to "GLFW_KEY_T",
        85 to "GLFW_KEY_U",
        86 to "GLFW_KEY_V",
        87 to "GLFW_KEY_W",
        88 to "GLFW_KEY_X",
        89 to "GLFW_KEY_Y",
        90 to "GLFW_KEY_Z",
        91 to "GLFW_KEY_LEFT_BRACKET",
        92 to "GLFW_KEY_BACKSLASH",
        93 to "GLFW_KEY_RIGHT_BRACKET",
        96 to "GLFW_KEY_GRAVE_ACCENT",
        161 to "GLFW_KEY_WORLD_1",
        162 to "GLFW_KEY_WORLD_2",
        256 to "GLFW_KEY_ESCAPE",
        257 to "GLFW_KEY_ENTER",
        258 to "GLFW_KEY_TAB",
        259 to "GLFW_KEY_BACKSPACE",
        260 to "GLFW_KEY_INSERT",
        261 to "GLFW_KEY_DELETE",
        262 to "GLFW_KEY_RIGHT",
        263 to "GLFW_KEY_LEFT",
        264 to "GLFW_KEY_DOWN",
        265 to "GLFW_KEY_UP",
        266 to "GLFW_KEY_PAGE_UP",
        267 to "GLFW_KEY_PAGE_DOWN",
        268 to "GLFW_KEY_HOME",
        269 to "GLFW_KEY_END",
        280 to "GLFW_KEY_CAPS_LOCK",
        281 to "GLFW_KEY_SCROLL_LOCK",
        282 to "GLFW_KEY_NUM_LOCK",
        283 to "GLFW_KEY_PRINT_SCREEN",
        284 to "GLFW_KEY_PAUSE",
        290 to "GLFW_KEY_F1",
        291 to "GLFW_KEY_F2",
        292 to "GLFW_KEY_F3",
        293 to "GLFW_KEY_F4",
        294 to "GLFW_KEY_F5",
        295 to "GLFW_KEY_F6",
        296 to "GLFW_KEY_F7",
        297 to "GLFW_KEY_F8",
        298 to "GLFW_KEY_F9",
        299 to "GLFW_KEY_F10",
        300 to "GLFW_KEY_F11",
        301 to "GLFW_KEY_F12",
        302 to "GLFW_KEY_F13",
        303 to "GLFW_KEY_F14",
        304 to "GLFW_KEY_F15",
        305 to "GLFW_KEY_F16",
        306 to "GLFW_KEY_F17",
        307 to "GLFW_KEY_F18",
        308 to "GLFW_KEY_F19",
        309 to "GLFW_KEY_F20",
        310 to "GLFW_KEY_F21",
        311 to "GLFW_KEY_F22",
        312 to "GLFW_KEY_F23",
        313 to "GLFW_KEY_F24",
        314 to "GLFW_KEY_F25",
        320 to "GLFW_KEY_KP_0",
        321 to "GLFW_KEY_KP_1",
        322 to "GLFW_KEY_KP_2",
        323 to "GLFW_KEY_KP_3",
        324 to "GLFW_KEY_KP_4",
        325 to "GLFW_KEY_KP_5",
        326 to "GLFW_KEY_KP_6",
        327 to "GLFW_KEY_KP_7",
        328 to "GLFW_KEY_KP_8",
        329 to "GLFW_KEY_KP_9",
        330 to "GLFW_KEY_KP_DECIMAL",
        331 to "GLFW_KEY_KP_DIVIDE",
        332 to "GLFW_KEY_KP_MULTIPLY",
        333 to "GLFW_KEY_KP_SUBTRACT",
        334 to "GLFW_KEY_KP_ADD",
        335 to "GLFW_KEY_KP_ENTER",
        336 to "GLFW_KEY_KP_EQUAL",
        340 to "GLFW_KEY_LEFT_SHIFT",
        341 to "GLFW_KEY_LEFT_CONTROL",
        342 to "GLFW_KEY_LEFT_ALT",
        343 to "GLFW_KEY_LEFT_SUPER",
        344 to "GLFW_KEY_RIGHT_SHIFT",
        345 to "GLFW_KEY_RIGHT_CONTROL",
        346 to "GLFW_KEY_RIGHT_ALT",
        347 to "GLFW_KEY_RIGHT_SUPER",
        348 to "GLFW_KEY_MENU"
    )
}
