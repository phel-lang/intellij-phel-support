package org.phellang.completion.engine

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import org.phellang.core.utils.PhelErrorHandler
import org.phellang.language.psi.*

/**
 * Completion context analyzer with only the methods that are actually used.
 * Analyzes completion context to determine what type of completions should be provided.
 */
class PhelCompletionContext(
    parameters: CompletionParameters
) {
    val element: PsiElement = parameters.position

    /**
     * Determine if we should suggest a new form (parentheses, brackets, templates)
     * This should be true when we're at the top level (nothing written yet)
     * and false when we're inside parentheses where we want function completions
     */
    fun shouldSuggestNewForm(): Boolean {
        return PhelErrorHandler.safeOperation {
            // Check if we're at file level - walk up the PSI tree
            isAtFileLevel()
        } ?: false
    }

    private fun isAtFileLevel(): Boolean {
        var current = element
        var depth = 0

        while (depth < 10) { // Prevent infinite loops
            if (current is PhelFile) {
                return true
            }

            // If we find a PhelList, we're inside a form, not at file level
            if (current is PhelList) {
                return false
            }

            current = current.parent
            depth++
        }

        return false
    }

    private fun isAfterOpeningParen(): Boolean {
        return PhelErrorHandler.safeOperation {
            val text = element.containingFile.text
            val offset = element.textOffset

            if (offset > 0) {
                val charBefore = text[offset - 1]
                charBefore == '(' || charBefore == '['
            } else {
                false
            }
        } ?: false
    }

    fun isInsideParentheses(): Boolean {
        return PhelErrorHandler.safeOperation {
            // We're inside parentheses if we're after an opening paren
            // or if our parent is a list and we're not at the file level
            val parent = element.parent

            isAfterOpeningParen() || (parent is PhelList && parent.parent !is PhelFile)
        } ?: false
    }
}
