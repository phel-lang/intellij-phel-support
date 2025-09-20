package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

class IfTemplateInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document

            val template = "(if condition then else)"
            document.replaceString(context.startOffset, context.tailOffset, template)

            val conditionStart = context.startOffset + 4 // Position after "(if "
            editor.caretModel.moveToOffset(conditionStart)
            editor.selectionModel.setSelection(conditionStart, conditionStart + 9) // Select "condition"
        }
    }
}
