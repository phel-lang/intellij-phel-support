package org.phellang.core.psi

import com.intellij.psi.util.PsiTreeUtil
import org.phellang.core.config.PhelConfiguration
import org.phellang.core.utils.PhelErrorHandler
import org.phellang.language.psi.*

/**
 * Specialized analyzer for Phel symbols and their contexts.
 * Handles definition detection, binding analysis, and symbol classification.
 */
object PhelSymbolAnalyzer {

    /**
     * Check if this symbol is in a definition position (being defined rather than referenced).
     * This helps distinguish between definitions and usages for proper highlighting.
     */
    @JvmStatic
    fun isDefinition(symbol: PhelSymbol): Boolean {
        return PhelErrorHandler.safeOperation({
            // Check if this symbol is a function parameter
            if (isFunctionParameter(symbol)) {
                return@safeOperation true
            }

            // Check if this symbol is a let binding
            if (isLetBinding(symbol)) {
                return@safeOperation true
            }

            // Check if this symbol is the first element in a list starting with def, defn, etc.
            val containingList = PsiTreeUtil.getParentOfType(symbol, PhelList::class.java)
            if (containingList != null) {
                val firstForm = PsiTreeUtil.findChildOfType(containingList, PhelForm::class.java)
                if (firstForm != null) {
                    val firstSymbol = PsiTreeUtil.findChildOfType(firstForm, PhelSymbol::class.java)
                    if (firstSymbol != null) {
                        val firstSymbolText = firstSymbol.text
                        if (isDefiningForm(firstSymbolText)) {
                            // Check if this symbol is the second element (the name being defined)
                            val forms = PsiTreeUtil.getChildrenOfType(containingList, PhelForm::class.java)
                            if (forms != null && forms.size > 1) {
                                val definedName = PsiTreeUtil.findChildOfType(forms[1], PhelSymbol::class.java)
                                return@safeOperation symbol === definedName
                            }
                        }
                    }
                }
            }

            false
        }) ?: false
    }

    /**
     * Check if a symbol represents a defining form (def, defn, defmacro, etc.).
     */
    @JvmStatic
    fun isDefiningForm(symbolText: String?): Boolean {
        if (symbolText == null) return false
        return symbolText in PhelConfiguration.Language.DEFINING_FORMS
    }

    /**
     * Check if a symbol represents a binding form (let, for, etc.).
     */
    @JvmStatic
    fun isBindingForm(symbolText: String?): Boolean {
        if (symbolText == null) return false
        return symbolText in PhelConfiguration.Language.BINDING_FORMS
    }

    /**
     * Check if a symbol represents a function form (defn, fn, etc.).
     */
    @JvmStatic
    fun isFunctionForm(symbolText: String?): Boolean {
        if (symbolText == null) return false
        return symbolText in PhelConfiguration.Language.FUNCTION_FORMS
    }

    /**
     * Check if a symbol represents a special form.
     */
    @JvmStatic
    fun isSpecialForm(symbolText: String?): Boolean {
        if (symbolText == null) return false
        return symbolText in PhelConfiguration.Language.SPECIAL_FORMS
    }

    /**
     * Check if this symbol is a function parameter.
     * Parameters appear in parameter vectors: (defn name [param1 param2] ...) or (fn [param1 param2] ...)
     */
    @JvmStatic
    fun isFunctionParameter(symbol: PhelSymbol): Boolean {
        return PhelErrorHandler.safeOperation({
            // Check if symbol is inside a parameter vector
            val paramVec = PsiTreeUtil.getParentOfType(symbol, PhelVec::class.java) ?: return@safeOperation false

            // Check if the parameter vector is part of a function definition
            val containingList = PsiTreeUtil.getParentOfType(paramVec, PhelList::class.java) ?: return@safeOperation false

            val forms = PsiTreeUtil.getChildrenOfType(containingList, PhelForm::class.java)
            if (forms == null || forms.size < 2) return@safeOperation false

            // Check first symbol to see if it's a function-defining form
            val firstSymbol = PsiTreeUtil.findChildOfType(forms[0], PhelSymbol::class.java) ?: return@safeOperation false

            val keyword = firstSymbol.text

            if (keyword == "defn" || keyword == "defn-" || keyword == "defmacro" || keyword == "defmacro-") {
                // For (defn name [params] ...) and (defn- name [params] ...), parameter vector is at forms[2]
                return@safeOperation forms.size >= 3 && forms[2] === paramVec
            } else if (keyword == "fn") {
                // For (fn [params] ...), parameter vector is at forms[1]
                return@safeOperation forms[1] === paramVec
            }

            false
        }) ?: false
    }

    /**
     * Check if this symbol is a let binding.
     * Let bindings appear in binding vectors: (let [symbol value symbol2 value2] ...)
     */
    @JvmStatic
    fun isLetBinding(symbol: PhelSymbol): Boolean {
        return PhelErrorHandler.safeOperation({
            // Check if symbol is inside a binding vector
            val bindingVec = PsiTreeUtil.getParentOfType(symbol, PhelVec::class.java) ?: return@safeOperation false

            // Check if the binding vector is part of a let form
            val containingList = PsiTreeUtil.getParentOfType(bindingVec, PhelList::class.java) ?: return@safeOperation false

            val forms = PsiTreeUtil.getChildrenOfType(containingList, PhelForm::class.java)
            if (forms == null || forms.size < 2) return@safeOperation false

            // Check if first symbol is a binding form
            val firstSymbol = PsiTreeUtil.findChildOfType(forms[0], PhelSymbol::class.java) ?: return@safeOperation false
            val formType = firstSymbol.text
            if (!isBindingForm(formType)) return@safeOperation false

            // Check if binding vector is at forms[1]
            if (forms[1] !== bindingVec) return@safeOperation false

            // Check if symbol is in an even position (binding symbols are at even indices)
            val bindings = PsiTreeUtil.getChildrenOfType(bindingVec, PhelForm::class.java) ?: return@safeOperation false

            var i = 0
            while (i < bindings.size) {
                val bindingSymbol = PsiTreeUtil.findChildOfType(bindings[i], PhelSymbol::class.java)
                if (bindingSymbol === symbol) {
                    return@safeOperation true // Found symbol at even index (binding position)
                }
                i += 2
            }

            false
        }) ?: false
    }

    /**
     * Get the symbol type based on its context.
     */
    @JvmStatic
    fun getSymbolType(symbol: PhelSymbol): String {
        return when {
            isDefinition(symbol) -> "definition"
            isFunctionParameter(symbol) -> "parameter"
            isLetBinding(symbol) -> "binding"
            else -> "reference"
        }
    }

    /**
     * Check if symbol is qualified (contains namespace).
     */
    @JvmStatic
    fun isQualifiedSymbol(symbol: PhelSymbol): Boolean {
        return PhelErrorHandler.safeOperation({
            val text = symbol.text ?: return@safeOperation false
            text.contains('/')
        }) ?: false
    }

    /**
     * Get the binding scope of a symbol (function, let, loop, etc.).
     */
    @JvmStatic
    fun getBindingScope(symbol: PhelSymbol): String? {
        return PhelErrorHandler.safeOperation({
            when {
                isFunctionParameter(symbol) -> "function"
                isLetBinding(symbol) -> "let"
                isDefinition(symbol) -> "global"
                else -> null
            }
        })
    }
}
