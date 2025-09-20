package org.phellang.core.psi

import org.phellang.core.utils.PhelErrorHandler
import org.phellang.language.psi.*

/**
 * Specialized analyzer for Phel literals and their types.
 * Handles literal type detection, validation, and content extraction.
 */
object PhelLiteralAnalyzer {

    /**
     * Get the literal type of a literal element
     */
    @JvmStatic
    fun getLiteralType(literal: PhelLiteral): String? {
        return PhelErrorHandler.safeOperation({
            val text = literal.text ?: return@safeOperation null
            
            when {
                text.matches(Regex("[+-]?\\d+(\\.\\d*)?([eE][+-]?\\d+)?")) -> "number"
                text.matches(Regex("[+-]?0x[\\da-fA-F_]+")) -> "hexnum"
                text.matches(Regex("[+-]?0b[01_]+")) -> "binnum"
                text.matches(Regex("[+-]?0o[0-7_]+")) -> "octnum"
                text.startsWith("\"") && text.endsWith("\"") -> "string"
                text == "true" || text == "false" -> "boolean"
                text == "nil" -> "nil"
                text == "NAN" -> "nan"
                text.startsWith("\\") -> "char"
                else -> "unknown"
            }
        })
    }

    /**
     * Get the literal text content (without quotes for strings, etc.)
     */
    @JvmStatic
    fun getLiteralText(literal: PhelLiteral): String? {
        return PhelErrorHandler.safeOperation({
            val text = literal.text ?: return@safeOperation null
            val type = getLiteralType(literal)
            
            when (type) {
                "string" -> if (text.length >= 2) text.substring(1, text.length - 1) else text
                "char" -> if (text.length > 1) text.substring(1) else text
                else -> text
            }
        })
    }

    /**
     * Check if literal is a numeric type
     */
    @JvmStatic
    fun isNumericLiteral(literal: PhelLiteral): Boolean {
        return PhelErrorHandler.safeOperation({
            val type = getLiteralType(literal)
            type in setOf("number", "hexnum", "binnum", "octnum")
        }) ?: false
    }

    /**
     * Check if literal is a string type
     */
    @JvmStatic
    fun isStringLiteral(literal: PhelLiteral): Boolean {
        return PhelErrorHandler.safeOperation({
            getLiteralType(literal) == "string"
        }) ?: false
    }

    /**
     * Check if literal is a boolean type
     */
    @JvmStatic
    fun isBooleanLiteral(literal: PhelLiteral): Boolean {
        return PhelErrorHandler.safeOperation({
            getLiteralType(literal) == "boolean"
        }) ?: false
    }

    /**
     * Check if literal is nil
     */
    @JvmStatic
    fun isNilLiteral(literal: PhelLiteral): Boolean {
        return PhelErrorHandler.safeOperation({
            getLiteralType(literal) == "nil"
        }) ?: false
    }

    /**
     * Check if literal is a character
     */
    @JvmStatic
    fun isCharLiteral(literal: PhelLiteral): Boolean {
        return PhelErrorHandler.safeOperation({
            getLiteralType(literal) == "char"
        }) ?: false
    }

    /**
     * Validate string literal syntax
     */
    @JvmStatic
    fun isValidStringLiteral(text: String): Boolean {
        return PhelErrorHandler.safeOperation({
            if (text.length < 2) return@safeOperation false
            if (!text.startsWith("\"") || !text.endsWith("\"")) return@safeOperation false
            
            // Check for proper escaping
            var i = 1
            while (i < text.length - 1) {
                if (text[i] == '\\') {
                    i += 2 // Skip escaped character
                } else {
                    i++
                }
            }
            true
        }) ?: false
    }

    /**
     * Validate numeric literal syntax
     */
    @JvmStatic
    fun isValidNumericLiteral(text: String): Boolean {
        return PhelErrorHandler.safeOperation({
            text.matches(Regex("[+-]?\\d+(\\.\\d*)?([eE][+-]?\\d+)?")) ||
            text.matches(Regex("[+-]?0x[\\da-fA-F_]+")) ||
            text.matches(Regex("[+-]?0b[01_]+")) ||
            text.matches(Regex("[+-]?0o[0-7_]+"))
        }) ?: false
    }

    /**
     * Get numeric value from literal if possible
     */
    @JvmStatic
    fun getNumericValue(literal: PhelLiteral): Number? {
        return PhelErrorHandler.safeOperation({
            val text = literal.text ?: return@safeOperation null
            val type = getLiteralType(literal)
            
            when (type) {
                "number" -> {
                    if (text.contains('.')) {
                        text.toDoubleOrNull()
                    } else {
                        text.toLongOrNull()
                    }
                }
                "hexnum" -> {
                    val hexPart = text.removePrefix("+").removePrefix("-")
                        .removePrefix("0x").replace("_", "")
                    val value = hexPart.toLongOrNull(16)
                    if (text.startsWith("-")) value?.unaryMinus() else value
                }
                "binnum" -> {
                    val binPart = text.removePrefix("+").removePrefix("-")
                        .removePrefix("0b").replace("_", "")
                    val value = binPart.toLongOrNull(2)
                    if (text.startsWith("-")) value?.unaryMinus() else value
                }
                "octnum" -> {
                    val octPart = text.removePrefix("+").removePrefix("-")
                        .removePrefix("0o").replace("_", "")
                    val value = octPart.toLongOrNull(8)
                    if (text.startsWith("-")) value?.unaryMinus() else value
                }
                else -> null
            }
        })
    }

    /**
     * Get boolean value from literal if possible
     */
    @JvmStatic
    fun getBooleanValue(literal: PhelLiteral): Boolean? {
        return PhelErrorHandler.safeOperation({
            val text = literal.text ?: return@safeOperation null
            when (text) {
                "true" -> true
                "false" -> false
                else -> null
            }
        })
    }
}
