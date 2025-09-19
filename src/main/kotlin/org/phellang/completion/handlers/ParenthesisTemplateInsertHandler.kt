package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

class ParenthesisTemplateInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document

            val template = "()"
            document.replaceString(context.startOffset, context.tailOffset, template)

            val nameStart = context.startOffset + 1 // Position after "("
            editor.caretModel.moveToOffset(nameStart)
        }
    }
}
