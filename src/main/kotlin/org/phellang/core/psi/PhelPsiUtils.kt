package org.phellang.core.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import org.phellang.core.utils.PhelErrorHandler
import org.phellang.language.psi.*

/**
 * Core PSI utility methods for working with Phel PSI elements.
 * This class provides essential helper functions for analyzing Phel code structures.
 */
object PhelPsiUtils {
    
    /**
     * Get the name of a Phel symbol.
     * For qualified symbols like "ns/name", returns just "name".
     * For simple symbols like "name", returns "name".
     */
    @JvmStatic
    fun getName(symbol: PhelSymbol): String? {
        return PhelErrorHandler.safeOperation({
            val text = symbol.text ?: return@safeOperation null

            // Handle qualified symbols (namespace/name)
            val slashIndex = text.lastIndexOf('/')
            if (slashIndex > 0 && slashIndex < text.length - 1) {
                text.substring(slashIndex + 1)
            } else {
                text
            }
        })
    }

    /**
     * Get the namespace qualifier of a symbol.
     * For "ns/name" returns "ns", for "name" returns null.
     */
    @JvmStatic
    fun getQualifier(symbol: PhelSymbol): String? {
        return PhelErrorHandler.safeOperation({
            val text = symbol.text ?: return@safeOperation null

            val slashIndex = text.lastIndexOf('/')
            if (slashIndex > 0) {
                text.take(slashIndex)
            } else {
                null
            }
        })
    }

    /**
     * Get the text offset of the symbol name part (excluding qualifier).
     * Used for proper positioning in rename refactoring.
     */
    @JvmStatic
    fun getNameTextOffset(symbol: PhelSymbol): Int {
        return PhelErrorHandler.safeOperation({
            val text = symbol.text ?: return@safeOperation symbol.textRange.startOffset

            val slashIndex = text.lastIndexOf('/')
            if (slashIndex > 0 && slashIndex < text.length - 1) {
                symbol.textRange.startOffset + slashIndex + 1
            } else {
                symbol.textRange.startOffset
            }
        }) ?: 0
    }

    /**
     * Set the name of a symbol (for rename refactoring)
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun setName(symbol: PhelSymbol, newName: String): PsiElement {
        // For now, return the element unchanged
        // Full implementation would create a new element with the new name
        return symbol
    }

    /**
     * Get the name identifier element for a symbol
     */
    @JvmStatic
    fun getNameIdentifier(symbol: PhelSymbol): PsiElement? {
        // The symbol itself is the name identifier
        return symbol
    }

    /**
     * Get the text offset of a symbol
     */
    @JvmStatic
    fun getTextOffset(symbol: PhelSymbol): Int {
        return symbol.textOffset
    }

    /**
     * Get the text offset of a list
     */
    @JvmStatic
    fun getTextOffset(list: PhelList): Int {
        return list.textOffset
    }

    /**
     * Get the reference for a symbol (for go-to-definition)
     */
    @JvmStatic
    fun getReference(symbol: PhelSymbol): PsiReference? {
        return PhelErrorHandler.safeOperation({
            symbol.reference
        })
    }

    /**
     * Get the first form in a list
     */
    @JvmStatic
    fun getFirst(list: PhelList): PhelForm? {
        return PhelErrorHandler.safeOperation({
            val forms = PsiTreeUtil.getChildrenOfType(list, PhelForm::class.java)
            if (forms != null && forms.isNotEmpty()) forms[0] else null
        })
    }

    /**
     * Get all forms in a list safely
     */
    @JvmStatic
    fun getForms(list: PhelList): Array<PhelForm> {
        return PhelErrorHandler.safeOperation({
            PsiTreeUtil.getChildrenOfType(list, PhelForm::class.java) ?: emptyArray()
        }) ?: emptyArray()
    }

    /**
     * Find child of specific type safely
     */
    @JvmStatic
    inline fun <reified T : PsiElement> findChildOfType(element: PsiElement): T? {
        return PhelErrorHandler.safeOperation({
            PsiTreeUtil.findChildOfType(element, T::class.java)
        })
    }

    /**
     * Find parent of specific type safely
     */
    @JvmStatic
    inline fun <reified T : PsiElement> findParentOfType(element: PsiElement): T? {
        return PhelErrorHandler.safeOperation({
            PsiTreeUtil.getParentOfType(element, T::class.java)
        })
    }

    /**
     * Get children of specific type safely
     */
    @JvmStatic
    inline fun <reified T : PsiElement> getChildrenOfType(element: PsiElement): Array<T> {
        return PhelErrorHandler.safeOperation({
            PsiTreeUtil.getChildrenOfType(element, T::class.java) ?: emptyArray()
        }) ?: emptyArray()
    }

    /**
     * Check if element is valid for operations
     */
    @JvmStatic
    fun isValidElement(element: PsiElement?): Boolean {
        return PhelErrorHandler.isValidPsiElement(element)
    }
}
