package org.phellang.completion.infrastructure

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import org.phellang.completion.handlers.NamespacedInsertHandler
import org.phellang.core.utils.PhelErrorHandler
import javax.swing.Icon

object PhelCompletionUtils {

    @JvmStatic
    fun addLocalSymbolCompletion(
        result: CompletionResultSet, name: String, type: String, icon: Icon?
    ) {
        PhelErrorHandler.safeOperation {
            val priority = when (type) {
                "Parameter", "Function Parameter" -> PhelCompletionPriority.CURRENT_SCOPE_LOCALS
                "Let Binding", "Local Variable", "Loop Binding" -> PhelCompletionPriority.CURRENT_SCOPE_LOCALS
                "Function", "Function (recursive)", "Global Variable" -> PhelCompletionPriority.RECENT_DEFINITIONS
                else -> PhelCompletionPriority.PROJECT_SYMBOLS
            }

            val element = createLookupElement(name, icon, null, type, bold = true)
            result.addElement(PrioritizedLookupElement.withPriority(element, priority.value))
        }
    }

    @JvmStatic
    fun addRankedCompletion(
        result: CompletionResultSet,
        name: String,
        signature: String,
        description: String,
        priority: PhelCompletionPriority
    ) {
        PhelErrorHandler.safeOperation {
            val element = createLookupElement(name, AllIcons.Nodes.Method, signature, description)
            result.addElement(PrioritizedLookupElement.withPriority(element, priority.value))
        }
    }

    @JvmStatic
    private fun createLookupElement(
        name: String, icon: Icon?, signature: String?, description: String?, bold: Boolean = false
    ): LookupElement {
        return PhelErrorHandler.safeOperation {
            var builder = LookupElementBuilder.create(name).withIcon(icon)

            if (signature != null) {
                builder = builder.withTypeText(signature)
            }

            if (description != null) {
                builder = builder.withTailText(" $description", true)
            }

            // Add boldness if requested (for local symbols)
            if (bold) {
                builder = builder.withBoldness(true)
            }

            // Use namespaced insert handler if the name contains '/'
            if (name.contains('/')) {
                builder = builder.withInsertHandler(NamespacedInsertHandler())
            }

            builder
        } ?: LookupElementBuilder.create(name)
    }
}
