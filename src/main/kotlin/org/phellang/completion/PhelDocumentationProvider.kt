package org.phellang.completion

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.phellang.completion.data.PhelFunctionRegistry
import org.phellang.completion.documentation.PhelFunctionDocumentation
import org.phellang.core.psi.PhelSymbolAnalyzer
import org.phellang.language.psi.*

class PhelDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val elementToClassify = originalElement as? PhelSymbol ?: if (originalElement != null) {
            val parentSymbol = PsiTreeUtil.getParentOfType(originalElement, PhelSymbol::class.java)
            parentSymbol ?: element
        } else {
            element
        }

        if (elementToClassify is PhelSymbol) {
            val symbolName = elementToClassify.text
            if (symbolName != null && symbolName.isNotEmpty()) {
                val doc = PhelFunctionDocumentation.FUNCTION_DOCS[symbolName]
                if (doc != null) {
                    return wrapInHtml(doc)
                }

                // Generate basic documentation for unknown symbols
                return generateBasicDocForElement(elementToClassify, symbolName)
            }
        }

        return super.generateDoc(element, originalElement)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element is PhelSymbol) {
            val symbolName = element.text
            if (symbolName != null && symbolName.isNotEmpty()) {
                val signature = getSignature(symbolName)
                if (signature != null) {
                    return "$symbolName $signature"
                }
            }
        }
        return super.getQuickNavigateInfo(element, originalElement)
    }

    /**
     * Wrap documentation content in proper HTML structure
     */
    private fun wrapInHtml(content: String): String {
        return "<html><body>$content</body></html>"
    }

    private fun generateBasicDocForElement(element: PhelSymbol, symbolName: String): String {
        val category = categorizeSymbol(element, symbolName)
        val signature = getSignature(symbolName) ?: symbolName

        return wrapInHtml(
            "<h3>$symbolName</h3><p><b>Type:</b> $category</p><p><b>Signature:</b> <code>$signature</code></p><p>Documentation not available for this symbol.</p>"
        )
    }

    private fun categorizeSymbol(element: PsiElement?, symbolName: String): String {
        // First check if this is a definition and determine its type
        if (element is PhelSymbol) {
            if (PhelSymbolAnalyzer.isDefinition(element)) {
                // Check if it's a function parameter or let binding first
                if (isInParameterVector(element)) {
                    return "Function Parameter"
                } else if (isInLetBinding(element)) {
                    return "Let Binding"
                }

                // Check the defining form to determine type
                val definingForm = getDefiningForm(element)
                return when (definingForm) {
                    "defn", "defn-" -> "Function Definition"
                    "defmacro", "defmacro-" -> "Macro Definition"
                    "def" -> "Variable Definition"
                    "defstruct" -> "Struct Definition"
                    else -> "Definition"
                }
            }
        }

        // Check if it's a known API function
        val functionInfo = PhelFunctionRegistry.getFunction(symbolName)
        if (functionInfo != null) {
            return when {
                symbolName.endsWith("?") -> "Predicate Function"
                symbolName in setOf("+", "-", "*", "/", "%", "**", "inc", "dec") -> "Arithmetic Function"
                symbolName in setOf("=", "<", ">", "<=", ">=") -> "Comparison Function"
                symbolName in setOf("map", "filter", "reduce", "first", "rest") -> "Collection Function"
                else -> "Core Function"
            }
        }

        // Check if it's a special form
        if (symbolName in setOf("def", "defn", "let", "if", "when", "do", "fn", "quote", "var", "throw", "try")) {
            return "Special Form"
        }

        // Default classification
        return "Symbol"
    }

    /**
     * Get function signature if available
     */
    private fun getSignature(symbolName: String): String? {
        return PhelFunctionRegistry.getFunction(symbolName)?.signature
    }

    /**
     * Check if symbol is in a parameter vector
     */
    private fun isInParameterVector(symbol: PhelSymbol): Boolean {
        return PhelSymbolAnalyzer.isFunctionParameter(symbol)
    }

    /**
     * Check if symbol is in a let binding
     */
    private fun isInLetBinding(symbol: PhelSymbol): Boolean {
        return PhelSymbolAnalyzer.isLetBinding(symbol)
    }

    /**
     * Get the defining form for a symbol
     */
    private fun getDefiningForm(symbol: PhelSymbol): String? {
        val containingList = PsiTreeUtil.getParentOfType(symbol, PhelList::class.java)
        if (containingList != null) {
            val firstForm = PsiTreeUtil.findChildOfType(containingList, PhelForm::class.java)
            if (firstForm != null) {
                val firstSymbol = PsiTreeUtil.findChildOfType(firstForm, PhelSymbol::class.java)
                return firstSymbol?.text
            }
        }
        return null
    }
}
