package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

class FnTemplateInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document

            val template = "(fn [args] body)"
            document.replaceString(context.startOffset, context.tailOffset, template)

            val argsStart = context.startOffset + 4 // Position after "(fn ["
            editor.caretModel.moveToOffset(argsStart)
            editor.selectionModel.setSelection(argsStart, argsStart + 4) // Select "args"
        }
    }
}
