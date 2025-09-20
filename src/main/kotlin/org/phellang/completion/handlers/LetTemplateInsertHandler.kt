package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

class LetTemplateInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document

            val template = "(let [bindings]\n  body)"
            document.replaceString(context.startOffset, context.tailOffset, template)

            val bindingsStart = context.startOffset + 6 // Position after "(let ["
            editor.caretModel.moveToOffset(bindingsStart)
            editor.selectionModel.setSelection(bindingsStart, bindingsStart + 8) // Select "bindings"
        }
    }
}
