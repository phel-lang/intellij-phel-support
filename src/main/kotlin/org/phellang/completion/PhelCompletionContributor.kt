package org.phellang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import org.phellang.PhelLanguage
import org.phellang.completion.engine.PhelMainCompletionProvider
import org.phellang.language.psi.PhelTypes

/**
 * Main completion contributor for Phel language using the new modular architecture
 */
class PhelCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(PhelTypes.SYM).withLanguage(PhelLanguage.INSTANCE),
            PhelMainCompletionProvider()
        )
    }
}
