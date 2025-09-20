package org.phellang.language.psi

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.util.PsiTreeUtil
import org.phellang.core.psi.PhelPsiUtils
import org.phellang.core.psi.PhelSymbolAnalyzer
import javax.swing.Icon
import kotlin.math.min

/**
 * Custom presentation for Phel PSI elements in navigation dialogs.
 * This controls how elements appear in "Choose Declaration" and similar modals.
 */
class PhelItemPresentation(private val myElement: PhelSymbol) : ItemPresentation {
    override fun getPresentableText(): String? {
        val name = PhelPsiUtils.getName(myElement) ?: return null

        // Add type information if this is a definition
        if (PhelSymbolAnalyzer.isDefinition(myElement)) {
            val definingKeyword = getDefiningKeyword(myElement)
            val type = getTypeFromDefiningKeyword(definingKeyword)
            if (type != null) {
                return "$name [$type]"
            }
        }

        return name
    }

    override fun getLocationString(): String? {
        val context = this.elementContext
        val location = this.locationInfo

        return if (context != null) {
            "$context $location"
        } else {
            location
        }
    }

    override fun getIcon(unused: Boolean): Icon? {
        // Could return different icons based on element type
        return null
    }

    private val elementContext: String?
        /**
         * Get contextual information about where/how the element is used.
         */
        get() {
            val containingList = PsiTreeUtil.getParentOfType(myElement, PhelList::class.java)
            if (containingList != null) {
                val listText = containingList.text
                if (listText != null) {
                    return if (listText.length > 60) {
                        // Truncate long expressions
                        "(" + listText.substring(1, min(50, listText.length - 1)) + "...)"
                    } else {
                        listText
                    }
                }
            }
            return null
        }

    private val locationInfo: String
        /**
         * Get location information (file name and line number).
         */
        get() {
            val location = StringBuilder()

            val containingFile = myElement.containingFile
            if (containingFile != null) {
                val fileName = containingFile.name
                location.append("(").append(fileName)

                val lineNumber = this.lineNumber
                if (lineNumber > 0) {
                    location.append(":").append(lineNumber)
                }

                location.append(")")
            }

            return location.toString()
        }

    private val lineNumber: Int
        /**
         * Calculate the line number for the element.
         */
        get() {
            val containingFile = myElement.containingFile
            if (containingFile != null) {
                val fileText = containingFile.text
                if (fileText != null) {
                    val offset = myElement.textOffset
                    var lineNumber = 1
                    var i = 0
                    while (i < offset && i < fileText.length) {
                        if (fileText[i] == '\n') {
                            lineNumber++
                        }
                        i++
                    }
                    return lineNumber
                }
            }
            return 0
        }

    /**
     * Get the defining keyword (def, defn, defmacro, etc.) for a symbol definition.
     */
    private fun getDefiningKeyword(symbol: PhelSymbol): String? {
        val containingList = PsiTreeUtil.getParentOfType(symbol, PhelList::class.java)
        if (containingList != null) {
            val firstForm = PsiTreeUtil.findChildOfType(containingList, PhelForm::class.java)
            if (firstForm != null) {
                val firstSymbol = PsiTreeUtil.findChildOfType(firstForm, PhelSymbol::class.java)
                if (firstSymbol != null) {
                    return firstSymbol.text
                }
            }
        }
        return null
    }

    /**
     * Convert defining keyword to readable type name.
     */
    private fun getTypeFromDefiningKeyword(keyword: String?): String? {
        if (keyword == null) return null

        return when (keyword) {
            "def", "def-" -> "variable"
            "defn", "defn-" -> "function"
            "defmacro", "defmacro-" -> "macro"
            "defstruct" -> "struct"
            "definterface" -> "interface"
            "defexception" -> "exception"
            "declare" -> "declaration"
            else -> null
        }
    }
}



