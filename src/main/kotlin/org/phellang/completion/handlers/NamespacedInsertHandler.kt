package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

/**
 * Custom insert handler for namespaced completions to prevent text duplication.
 * Handles completions containing '/' by properly replacing the typed prefix.
 */
class NamespacedInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document
            val caretOffset = context.startOffset

            // Find the start of the current symbol
            var symbolStart = caretOffset
            val text = document.charsSequence

            // Move back to find the beginning of the symbol
            while (symbolStart > 0) {
                val c = text[symbolStart - 1]
                // Stop at whitespace, opening parenthesis, or other delimiters
                if (Character.isWhitespace(c) || c == '(' || c == '[' || c == '{') {
                    break
                }
                symbolStart--
            }

            // Replace the entire symbol with the completion
            val symbolEnd = context.tailOffset
            document.replaceString(symbolStart, symbolEnd, item.lookupString)

            // Position cursor after the inserted text
            editor.caretModel.moveToOffset(symbolStart + item.lookupString.length)
        }
    }
}
