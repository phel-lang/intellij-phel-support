package org.phellang.language.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.phellang.core.psi.PhelPsiUtils
import org.phellang.core.psi.PhelSymbolAnalyzer
import org.phellang.language.psi.impl.PhelSFormImpl

/**
 * Base implementation for named Phel elements that can be referenced and renamed.
 * This class implements PsiNameIdentifierOwner to enable proper 'Go to Definition' functionality.
 */
abstract class PhelNamedElementImpl(node: ASTNode) : PhelSFormImpl(node), PsiNameIdentifierOwner {
    override fun getName(): String? {
        if (this is PhelSymbol) {
            return PhelPsiUtils.getName(this as PhelSymbol)
        }
        return null
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(name: @NonNls String): PsiElement {
        // For now, return this unchanged. In a full implementation, 
        // this would create a new PSI element with the new name.
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        // For symbols, the entire element is the name identifier
        return this
    }

    override fun getReference(): PsiReference? {
        if (this is PhelSymbol) {
            val symbol = this as PhelSymbol
            val isDefinition = PhelSymbolAnalyzer.isDefinition(symbol)

            return if (isDefinition) {
                PhelReference(symbol, true) // findUsages = true
            } else {
                PhelReference(symbol, false) // findUsages = false
            }
        }
        return null
    }

    override fun getTextOffset(): Int {
        return PhelPsiUtils.getNameTextOffset(this as PhelSymbol)
    }

    override fun getPresentation(): ItemPresentation? {
        if (this is PhelSymbol) {
            return PhelItemPresentation(this as PhelSymbol)
        }
        return super.getPresentation()
    }
}
