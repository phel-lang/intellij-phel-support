package org.phellang.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.phellang.core.utils.PhelErrorHandler

class DefnTemplateInsertHandler : InsertHandler<LookupElement?> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        PhelErrorHandler.safeOperation {
            val editor = context.editor
            val document = editor.document

            val template = "(defn name [args]\n  body)"
            document.replaceString(context.startOffset, context.tailOffset, template)

            val nameStart = context.startOffset + 6 // Position after "(defn "
            editor.caretModel.moveToOffset(nameStart)
            editor.selectionModel.setSelection(nameStart, nameStart + 4) // Select "name"
        }
    }
}
