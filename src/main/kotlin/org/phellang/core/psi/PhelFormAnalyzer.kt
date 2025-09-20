package org.phellang.core.psi

import org.phellang.core.utils.PhelErrorHandler
import org.phellang.language.psi.*

/**
 * Specialized analyzer for Phel forms and their representations.
 * Handles form type detection, string representation, and structural analysis.
 */
object PhelFormAnalyzer {

    /**
     * Get string representation for forms
     */
    @JvmStatic
    fun toString(form: PhelForm): String {
        return PhelErrorHandler.safeOperation({
            "PhelForm(${form.text})"
        }) ?: "PhelForm(invalid)"
    }

    /**
     * Get string representation for metadata
     */
    @JvmStatic
    fun toString(metadata: PhelMetadata): String {
        return PhelErrorHandler.safeOperation({
            "PhelMetadata(${metadata.text})"
        }) ?: "PhelMetadata(invalid)"
    }

    /**
     * Get string representation for reader macros
     */
    @JvmStatic
    fun toString(readerMacro: PhelReaderMacro): String {
        return PhelErrorHandler.safeOperation({
            "PhelReaderMacro(${readerMacro.text})"
        }) ?: "PhelReaderMacro(invalid)"
    }

    /**
     * Check if form is a list form
     */
    @JvmStatic
    fun isListForm(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            PhelPsiUtils.findChildOfType<PhelList>(form) != null
        }) ?: false
    }

    /**
     * Check if form is a vector form
     */
    @JvmStatic
    fun isVectorForm(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            PhelPsiUtils.findChildOfType<PhelVec>(form) != null
        }) ?: false
    }

    /**
     * Check if form is a map form
     */
    @JvmStatic
    fun isMapForm(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            PhelPsiUtils.findChildOfType<PhelMap>(form) != null
        }) ?: false
    }

    /**
     * Check if form is a symbol form
     */
    @JvmStatic
    fun isSymbolForm(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            PhelPsiUtils.findChildOfType<PhelSymbol>(form) != null
        }) ?: false
    }

    /**
     * Check if form is a literal form
     */
    @JvmStatic
    fun isLiteralForm(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            PhelPsiUtils.findChildOfType<PhelLiteral>(form) != null
        }) ?: false
    }

    /**
     * Check if form is a keyword form
     */
    @JvmStatic
    fun isKeywordForm(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            PhelPsiUtils.findChildOfType<PhelKeyword>(form) != null
        }) ?: false
    }

    /**
     * Get the primary element type within a form
     */
    @JvmStatic
    fun getFormType(form: PhelForm): String {
        return PhelErrorHandler.safeOperation({
            when {
                isListForm(form) -> "list"
                isVectorForm(form) -> "vector"
                isMapForm(form) -> "map"
                isSymbolForm(form) -> "symbol"
                isLiteralForm(form) -> "literal"
                isKeywordForm(form) -> "keyword"
                else -> "unknown"
            }
        }) ?: "unknown"
    }

    /**
     * Get the symbol from a form if it contains one
     */
    @JvmStatic
    fun getSymbol(form: PhelForm): PhelSymbol? {
        return PhelPsiUtils.findChildOfType<PhelSymbol>(form)
    }

    /**
     * Get the literal from a form if it contains one
     */
    @JvmStatic
    fun getLiteral(form: PhelForm): PhelLiteral? {
        return PhelPsiUtils.findChildOfType<PhelLiteral>(form)
    }

    /**
     * Get the keyword from a form if it contains one
     */
    @JvmStatic
    fun getKeyword(form: PhelForm): PhelKeyword? {
        return PhelPsiUtils.findChildOfType<PhelKeyword>(form)
    }

    /**
     * Get the list from a form if it contains one
     */
    @JvmStatic
    fun getList(form: PhelForm): PhelList? {
        return PhelPsiUtils.findChildOfType<PhelList>(form)
    }

    /**
     * Get the vector from a form if it contains one
     */
    @JvmStatic
    fun getVector(form: PhelForm): PhelVec? {
        return PhelPsiUtils.findChildOfType<PhelVec>(form)
    }

    /**
     * Get the map from a form if it contains one
     */
    @JvmStatic
    fun getMap(form: PhelForm): PhelMap? {
        return PhelPsiUtils.findChildOfType<PhelMap>(form)
    }

    /**
     * Check if form is empty (contains no meaningful content)
     */
    @JvmStatic
    fun isEmpty(form: PhelForm): Boolean {
        return PhelErrorHandler.safeOperation({
            val text = form.text?.trim()
            text.isNullOrEmpty()
        }) ?: true
    }

    /**
     * Get the depth of nested forms
     */
    @JvmStatic
    fun getFormDepth(form: PhelForm): Int {
        return PhelErrorHandler.safeOperation({
            fun calculateDepth(element: PhelForm, currentDepth: Int): Int {
                var depth = currentDepth
                
                // Check for nested lists
                PhelPsiUtils.getChildrenOfType<PhelList>(element).forEach { list ->
                    PhelPsiUtils.getChildrenOfType<PhelForm>(list).forEach { childForm ->
                        depth = maxOf(depth, calculateDepth(childForm, currentDepth + 1))
                    }
                }
                
                // Check for nested vectors
                PhelPsiUtils.getChildrenOfType<PhelVec>(element).forEach { vec ->
                    PhelPsiUtils.getChildrenOfType<PhelForm>(vec).forEach { childForm ->
                        depth = maxOf(depth, calculateDepth(childForm, currentDepth + 1))
                    }
                }
                
                // Check for nested maps
                PhelPsiUtils.getChildrenOfType<PhelMap>(element).forEach { map ->
                    PhelPsiUtils.getChildrenOfType<PhelForm>(map).forEach { childForm ->
                        depth = maxOf(depth, calculateDepth(childForm, currentDepth + 1))
                    }
                }
                
                return depth
            }
            
            calculateDepth(form, 1)
        }) ?: 0
    }
}
