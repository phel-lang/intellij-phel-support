package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

class DefTemplateInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document

            val template = "(def name value)"
            document.replaceString(context.startOffset, context.tailOffset, template)

            val nameStart = context.startOffset + 5 // Position after "(def "
            editor.caretModel.moveToOffset(nameStart)
            editor.selectionModel.setSelection(nameStart, nameStart + 4) // Select "name"
        }
    }
}
