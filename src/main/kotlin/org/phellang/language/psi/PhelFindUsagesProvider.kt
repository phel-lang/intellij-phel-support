package org.phellang.language.psi

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.phellang.core.psi.PhelPsiUtils
import org.phellang.core.psi.PhelSymbolAnalyzer
import org.phellang.language.PhelLexerAdapter
import kotlin.math.min

/**
 * Provides information about how Phel elements should be displayed in find usages and navigation.
 * This determines whether elements are labeled as "variable", "function", "macro", etc.
 */
class PhelFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            PhelLexerAdapter(), PhelTokenSets.SYM,  // Use SYM tokens for identifiers
            PhelTokenSets.LINE_COMMENT,  // Use LINE_COMMENT for comments
            TokenSet.create(PhelTypes.STRING, PhelTypes.NUMBER, PhelTypes.BOOL, PhelTypes.NIL)
        ) // Literals
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is PsiNamedElement
    }

    override fun getHelpId(psiElement: PsiElement): String? {
        return null // No specific help ID needed
    }

    /**
     * Returns the type description for an element (e.g., "variable", "function", "macro").
     * This is what appears in the "Choose Declaration" modal.
     */
    override fun getType(element: PsiElement): String {
        if (element is PhelSymbol) {
            // Check if this is a definition and determine its type
            if (PhelSymbolAnalyzer.isDefinition(element)) {
                val definingKeyword = getDefiningKeyword(element)

                return when (definingKeyword) {
                    "def", "def-" -> "variable"
                    "defn", "defn-" -> "function"
                    "defmacro", "defmacro-" -> "macro"
                    "defstruct" -> "struct"
                    "definterface" -> "interface"
                    "defexception" -> "exception"
                    "declare" -> "declaration"
                    else -> "symbol"
                }
            }

            // For usages, try to determine type by looking at the definition
            return "symbol"
        }
        return "element"
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

    override fun getDescriptiveName(element: PsiElement): String {
        if (element is PhelSymbol) {
            val name = PhelPsiUtils.getName(element)
            if (name != null) {
                // Add type information to the descriptive name
                val type = getType(element)
                if (!"symbol".equals(type, ignoreCase = true)) {
                    return "$name [$type]"
                }
                return name
            }
            return "<unnamed>"
        }
        return element.toString()
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        if (element is PhelSymbol) {
            val name = PhelPsiUtils.getName(element)
            if (name != null) {
                // Add context information for better identification
                val context = getElementContext(element)
                val location = getLocationInfo(element)

                val result = StringBuilder()
                result.append(if (useFullName) getFullyQualifiedName(element, name) else name)

                if (context != null && !context.isEmpty()) {
                    result.append(" ").append(context)
                }

                if (!location.isEmpty()) {
                    result.append(" ").append(location)
                }

                return result.toString()
            }
        }
        return element.toString()
    }

    private fun getElementContext(symbol: PhelSymbol): String? {
        // Get the containing list to understand the context
        val containingList = PsiTreeUtil.getParentOfType(symbol, PhelList::class.java)
        if (containingList != null) {
            val listText = containingList.text
            if (listText != null && listText.length > 50) {
                // Truncate long expressions and show the beginning
                return "in (" + listText.substring(1, min(47, listText.length - 1)) + "...)"
            } else if (listText != null) {
                return "in $listText"
            }
        }
        return null
    }

    private fun getLocationInfo(symbol: PhelSymbol): String {
        val location = StringBuilder()

        // Add file name
        val containingFile = symbol.containingFile
        if (containingFile != null) {
            val fileName = containingFile.name
            location.append("(").append(fileName)

            // Add line number
            val lineNumber = getLineNumber(symbol)
            if (lineNumber > 0) {
                location.append(":").append(lineNumber)
            }

            location.append(")")
        }

        return location.toString()
    }

    private fun getLineNumber(element: PsiElement): Int {
        val containingFile = element.containingFile
        if (containingFile != null) {
            val fileText = containingFile.text
            if (fileText != null) {
                val offset = element.textOffset
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

    private fun getFullyQualifiedName(element: PsiElement, name: String): String {
        if (element is PhelSymbol) {
            val qualifier = PhelPsiUtils.getQualifier(element)
            if (qualifier != null) {
                return "$qualifier/$name"
            }
        }
        return name
    }
}
