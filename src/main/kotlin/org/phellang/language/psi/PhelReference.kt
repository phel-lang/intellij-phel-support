package org.phellang.language.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.phellang.core.psi.PhelPsiUtils
import org.phellang.core.psi.PhelSymbolAnalyzer

/**
 * Reference implementation for Phel symbols that supports resolving to multiple targets.
 * This handles cases where a symbol might refer to multiple definitions, such as:
 * - Function overloads
 * - Macro vs function with same name
 * - Forward declarations + definitions
 * - Redefinitions
 */
class PhelReference @JvmOverloads constructor(
    element: PhelSymbol, // If true, find usages; if false, find definitions
    private val findUsages: Boolean = PhelSymbolAnalyzer.isDefinition(element)
) : PsiReferenceBase<PhelSymbol?>(element, calculateRangeInElement(element)), PsiPolyVariantReference {
    private val symbolName: String? = PhelPsiUtils.getName(element)

    /**
     * Resolve to multiple targets. This is the key method for polyvariant references.
     * Returns all possible targets that this symbol could refer to.
     */
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (symbolName == null || symbolName.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY
        }

        val results: MutableList<ResolveResult> = ArrayList()

        if (findUsages) {
            // Find all usages of this symbol (for definitions)
            val usages = findAllUsages()
            for (usage in usages) {
                results.add(PsiElementResolveResult(usage))
            }
        } else {
            // Find definitions (original behavior)
            findDefinitions(results)
        }

        return results.toTypedArray<ResolveResult>()
    }

    /**
     * Find all definitions for a symbol usage (original multiResolve logic).
     */
    private fun findDefinitions(results: MutableList<ResolveResult>) {
        // 1. Check local scope first - but collect ALL definitions for polyvariant resolution
        val localDefinition = resolveInLocalScope()
        if (localDefinition != null) {
            results.add(PsiElementResolveResult(localDefinition))
            // Don't return immediately - collect ALL definitions for polyvariant resolution
        }

        // 2. Current file definitions (def, defn, defmacro, etc.)
        val fileDefinitions = resolveInCurrentFile()
        for (def in fileDefinitions) {
            // Avoid duplicates
            var alreadyAdded = false
            for (existing in results) {
                if (existing.element === def) {
                    alreadyAdded = true
                    break
                }
            }

            if (!alreadyAdded) {
                results.add(PsiElementResolveResult(def))
            }
        }

        val parameterDefinitions = findAllFunctionParameters()
        for (param in parameterDefinitions) {
            // Avoid duplicates - check if we already added this element
            var alreadyAdded = false
            for (existing in results) {
                if (existing.element === param) {
                    alreadyAdded = true
                    break
                }
            }

            if (!alreadyAdded) {
                results.add(PsiElementResolveResult(param))
            }
        }

        // 3. Project-wide definitions (other files)
        val projectDefinitions = resolveInProject()
        for (def in projectDefinitions) {
            // Avoid duplicates
            var alreadyAdded = false
            for (existing in results) {
                if (existing.element === def) {
                    alreadyAdded = true
                    break
                }
            }

            if (!alreadyAdded) {
                results.add(PsiElementResolveResult(def))
            }
        }
    }

    /**
     * Find all usages of this symbol (for when clicking on definitions).
     * This includes both actual usages AND other definitions (not the one being clicked).
     */
    private fun findAllUsages(): MutableCollection<PsiElement> {
        val usages: MutableList<PsiElement> = ArrayList()

        // For function parameters, let bindings, etc., only search within the local scope
        if (PhelSymbolAnalyzer.isDefinition(myElement!!) && this.isLocalBinding) {
            // For local bindings, search only within the containing form (function/let block)
            val localUsages = findUsagesInLocalScope()
            if (!localUsages.isEmpty()) {
                usages.addAll(localUsages)
                return usages // Return immediately for local bindings - no project-wide search needed
            }
        }

        // Search current file for usages and other definitions
        val containingFile = myElement!!.containingFile
        if (containingFile is PhelFile) {
            val allSymbols = PsiTreeUtil.findChildrenOfType(containingFile, PhelSymbol::class.java)

            for (symbol in allSymbols) {
                if (symbol !== null) {
                    val name = PhelPsiUtils.getName(symbol)
                    if (symbolName == name && symbol !== myElement) {
                        // Include both usages AND other definitions (but not the element we clicked on)
                        usages.add(symbol)
                    }
                }
            }
        }

        // Search project-wide ONLY for top-level definitions (def, defn, etc.)
        // Skip expensive project-wide search for local bindings
        if (!this.isLocalBinding) {
            val project = myElement!!.project
            val projectScope = GlobalSearchScope.projectScope(project)

            val phelFiles = FilenameIndex.getAllFilesByExt(project, "phel", projectScope)
            for (virtualFile in phelFiles) {
                if (virtualFile == containingFile.virtualFile) {
                    continue  // Skip current file (already searched above)
                }

                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                if (psiFile !is PhelFile) continue

                val allSymbols = PsiTreeUtil.findChildrenOfType(psiFile, PhelSymbol::class.java)

                for (symbol in allSymbols) {
                    val name = PhelPsiUtils.getName(symbol)
                    if (symbolName == name) {
                        // Include both usages AND definitions from other files
                        usages.add(symbol)
                    }
                }
            }
        }

        return usages
    }

    private val isLocalBinding: Boolean
        /**
         * Check if this symbol is a local binding (parameter, let binding, etc.)
         * This checks if it's NOT a top-level definition (def, defn, etc.)
         */
        get() {
            if (!PhelSymbolAnalyzer.isDefinition(myElement!!)) {
                return false // Not a definition at all
            }

            // Check if it's inside a parameter vector or binding vector
            PsiTreeUtil.getParentOfType(myElement, PhelVec::class.java)
                ?: return false // Not in a vector, so likely a top-level definition

            // If it's in a vector, it's likely a parameter or binding (local binding)
            return true
        }

    /**
     * Find usages within local scope only (for performance optimization)
     */
    private fun findUsagesInLocalScope(): MutableCollection<PsiElement> {
        val usages: MutableList<PsiElement> = ArrayList()

        // Find the containing function or let form
        val containingForm = findContainingForm() ?: return usages

        // Search only within this form
        val localSymbols = PsiTreeUtil.findChildrenOfType(containingForm, PhelSymbol::class.java)

        for (symbol in localSymbols) {
            val name = PhelPsiUtils.getName(symbol)
            if (symbolName == name && symbol !== myElement && !PhelSymbolAnalyzer.isDefinition(symbol)) {
                // Include only usages (not other definitions) within local scope
                usages.add(symbol)
            }
        }

        return usages
    }

    /**
     * Find the containing form (defn, fn, let, etc.) for scoped search
     */
    private fun findContainingForm(): PhelList? {
        var current = myElement!!.parent

        while (current != null) {
            if (current is PhelList) {
                val list = current

                // Check if this is a defining form
                val firstForm = PsiTreeUtil.findChildOfType(list, PhelForm::class.java)
                if (firstForm != null) {
                    val firstSymbol = PsiTreeUtil.findChildOfType(firstForm, PhelSymbol::class.java)
                    if (firstSymbol != null) {
                        val keyword = firstSymbol.text
                        if (keyword == "defn" || keyword == "defn-" || keyword == "defmacro" || keyword == "defmacro-" || keyword == "fn" || keyword == "let" || keyword == "if-let" || keyword == "for" || keyword == "binding" || keyword == "loop" || keyword == "foreach" || keyword == "dofor" || keyword == "try" || keyword == "catch") {
                            return list
                        }
                    }
                }
            }
            current = current.parent
        }

        return null
    }

    /**
     * Single target resolution - returns null for multiple targets to force modal display.
     * This ensures IntelliJ shows a selection modal when multiple targets exist.
     */
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)

        // For definitions finding usages, return null if multiple usages exist
        // This forces IntelliJ to show the polyvariant selection modal
        if (findUsages && results.size > 1) {
            return null // Force modal for multiple usages
        }

        // For single targets or usage-to-definition, return the first result
        return if (results.isNotEmpty()) results[0].element else null
    }

    /**
     * Check if this reference points to the given element.
     */
    override fun isReferenceTo(element: PsiElement): Boolean {
        val results = multiResolve(false)
        for (result in results) {
            if (element == result.element) {
                return true
            }
        }
        return false
    }

    /**
     * Provide completion variants for this reference.
     * Returns symbols that are valid in this context.
     */
    override fun getVariants(): Array<Any?> {
        val variants: MutableList<Any?> = ArrayList()

        // Add local scope definitions (let bindings, parameters)
        val localDef = resolveInLocalScope()
        if (localDef != null) {
            variants.add(localDef)
        }

        // Add file-level definitions
        val fileDefinitions = resolveInCurrentFile()
        variants.addAll(fileDefinitions)

        // Add project-wide definitions (limited for performance)
        if (variants.size < 20) { // Limit to prevent performance issues
            val projectDefs = resolveInProject()
            variants.addAll(projectDefs)
        }

        return variants.toTypedArray()
    }

    /**
     * Handle renaming of the referenced element.
     * Updates the symbol text to the new name.
     */
    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement? {
        if (myElement is PhelSymbol) {
            val symbol = myElement as PhelSymbol

            // For qualified symbols like "ns/symbol", only rename the symbol part
            val currentText = symbol.text
            if (currentText != null && currentText.contains("/")) {
                val slashIndex = currentText.lastIndexOf('/')
                val qualifier = currentText.take(slashIndex + 1)
                val newText = qualifier + newElementName
                return symbol.setName(newText)
            } else {
                // For unqualified symbols, replace entire text
                return symbol.setName(newElementName)
            }
        }
        return null
    }

    /**
     * Bind this reference to a different element.
     */
    @Throws(IncorrectOperationException::class)
    override fun bindToElement(element: PsiElement): PsiElement? {
        return myElement
    }

    // === Resolution Methods ===
    /**
     * Resolve in local scope: let bindings, function parameters, etc.
     * Local bindings have highest priority and shadow everything else.
     */
    private fun resolveInLocalScope(): PsiElement? {
        var current: PsiElement? = myElement

        // Walk up the PSI tree looking for binding contexts
        while (current != null) {
            // Check for let bindings
            val letForm = PsiTreeUtil.getParentOfType(current, PhelList::class.java)
            if (letForm != null) {
                val binding = findInLetBindings(letForm)
                if (binding != null) {
                    return binding
                }
            }

            // Check for function parameters
            val fnParam = findInFunctionParameters(current)
            if (fnParam != null) {
                return fnParam
            }

            current = current.parent
        }

        return null
    }

    /**
     * Resolve in current file: def, defn, defmacro, etc.
     */
    private fun resolveInCurrentFile(): MutableCollection<PsiElement> {
        val results: MutableList<PsiElement> = ArrayList()
        val file = myElement!!.containingFile

        if (file != null) {
            val lists = PsiTreeUtil.findChildrenOfType(file, PhelList::class.java)
            for (list in lists) {
                val definition = findDefinitionInList(list)
                if (definition != null) {
                    results.add(definition)
                }
            }
        }

        return results
    }

    /**
     * Resolve in entire project: search all .phel files.
     */
    private fun resolveInProject(): MutableCollection<PsiElement> {
        val results: MutableList<PsiElement> = ArrayList()
        val project = myElement!!.project

        // Find all .phel files in the project
        val phelFiles = FilenameIndex.getAllFilesByExt(
            project, "phel", GlobalSearchScope.projectScope(project)
        )

        val psiManager = PsiManager.getInstance(project)

        for (file in phelFiles) {
            val psiFile = psiManager.findFile(file)
            if (psiFile != null && psiFile != myElement!!.containingFile) {
                // Skip current file (already handled above)
                val lists = PsiTreeUtil.findChildrenOfType(psiFile, PhelList::class.java)
                for (list in lists) {
                    val definition = findDefinitionInList(list)
                    if (definition != null) {
                        results.add(definition)
                    }
                }
            }
        }

        return results
    }

    // === Helper Methods ===
    /**
     * Find symbol in let bindings: (let [symbol value] ...)
     */
    private fun findInLetBindings(letForm: PhelList): PsiElement? {
        val forms = PsiTreeUtil.getChildrenOfType(letForm, PhelForm::class.java)
        if (forms == null || forms.size < 2) {
            return null
        }

        // Check if this is a binding form (let, for, binding, loop, foreach, dofor)
        val firstSymbol = PsiTreeUtil.findChildOfType(forms[0], PhelSymbol::class.java) ?: return null
        val formType = firstSymbol.text
        if (("let" != formType) && ("if-let" != formType) && ("for" != formType) && ("binding" != formType) && ("loop" != formType) && ("foreach" != formType) && ("dofor" != formType) && ("catch" != formType)) {
            return null
        }

        // Look at binding vector - forms[1] should BE the vector directly
        val bindingsVec = if (forms[1] is PhelVec) {
            forms[1] as PhelVec
        } else {
            // Fallback: look inside forms[1] for a vector
            PsiTreeUtil.findChildOfType(forms[1], PhelVec::class.java)
        }

        if (bindingsVec == null) {
            return null
        }

        val bindings = PsiTreeUtil.getChildrenOfType(bindingsVec, PhelForm::class.java) ?: return null

        // Bindings are pairs: [symbol1 value1 symbol2 value2 ...]
        var i = 0
        while (i < bindings.size) {
            val bindingSymbol = PsiTreeUtil.findChildOfType(bindings[i], PhelSymbol::class.java)
            if (bindingSymbol != null && symbolName == bindingSymbol.text) {
                return bindingSymbol
            }
            i += 2
        }

        return null
    }

    /**
     * Find symbol in function parameters: (defn func-name [param1 param2] ...)
     * This searches for ALL function parameters with matching names, not just in the current function.
     */
    private fun findInFunctionParameters(context: PsiElement): PsiElement? {
        // Walk up the PSI tree to find ALL parent PhelList elements
        // We need to check each one to see if it's a function definition
        var current: PsiElement? = context

        while (current != null) {
            // Find the next parent PhelList
            val fnForm = PsiTreeUtil.getParentOfType(current, PhelList::class.java) ?: break

            val forms = PsiTreeUtil.getChildrenOfType(fnForm, PhelForm::class.java)
            if (forms != null && forms.size >= 2) {
                // Check if this is a function definition
                val fnKeyword = PsiTreeUtil.findChildOfType(forms[0], PhelSymbol::class.java)

                if (fnKeyword != null) {
                    val keyword = fnKeyword.text
                    var paramVec: PhelVec? = null

                    // Handle different function types:
                    // (defn name [params] body) - parameter vector at forms[2]
                    // (fn [params] body) - parameter vector at forms[1] 
                    if (keyword == "defn" || keyword == "defn-" || keyword == "defmacro" || keyword == "defmacro-") {
                        if (forms.size >= 3) {
                            paramVec = if (forms[2] is PhelVec) {
                                forms[2] as PhelVec
                            } else {
                                PsiTreeUtil.findChildOfType(forms[2], PhelVec::class.java)
                            }
                        }
                    } else if (keyword == "fn") {
                        paramVec = if (forms[1] is PhelVec) {
                            forms[1] as PhelVec
                        } else {
                            PsiTreeUtil.findChildOfType(forms[1], PhelVec::class.java)
                        }
                    }

                    if (paramVec != null) {
                        val params = PsiTreeUtil.getChildrenOfType(paramVec, PhelForm::class.java)
                        if (params != null) {
                            for (param in params) {
                                val paramSymbol = PsiTreeUtil.findChildOfType(param, PhelSymbol::class.java)
                                if (paramSymbol != null && symbolName == paramSymbol.text) {
                                    return paramSymbol
                                }
                            }
                        }
                    }
                }
            }

            // Continue searching up the tree
            current = fnForm.parent
        }

        return null
    }

    /**
     * Find ALL function parameters with matching names in the current file.
     * This is for polyvariant resolution - show all parameter definitions.
     */
    private fun findAllFunctionParameters(): MutableCollection<PsiElement> {
        val results: MutableList<PsiElement> = ArrayList()
        val file = myElement!!.containingFile

        if (file != null) {
            val lists = PsiTreeUtil.findChildrenOfType(file, PhelList::class.java)
            for (list in lists) {
                val forms = PsiTreeUtil.getChildrenOfType(list, PhelForm::class.java)
                if (forms == null || forms.size < 2) continue

                // Check if this is a function definition
                val fnKeyword = PsiTreeUtil.findChildOfType(forms[0], PhelSymbol::class.java) ?: continue

                val keyword = fnKeyword.text
                var paramVec: PhelVec? = null

                // Handle different function types
                if (keyword == "defn" || keyword == "defn-" || keyword == "defmacro" || keyword == "defmacro-") {
                    if (forms.size >= 3) {
                        paramVec = PsiTreeUtil.findChildOfType(forms[2], PhelVec::class.java)
                    }
                } else if (keyword == "fn") {
                    paramVec = PsiTreeUtil.findChildOfType(forms[1], PhelVec::class.java)
                } else {
                    continue  // Not a function we recognize
                }

                if (paramVec == null) continue

                val params = PsiTreeUtil.getChildrenOfType(paramVec, PhelForm::class.java) ?: continue

                for (param in params) {
                    val paramSymbol = PsiTreeUtil.findChildOfType(param, PhelSymbol::class.java)
                    if (paramSymbol != null && symbolName == paramSymbol.text) {
                        results.add(paramSymbol)
                    }
                }
            }
        }

        return results
    }

    /**
     * Find definition in a list form: (def name value), (defn name [...] ...), etc.
     */
    private fun findDefinitionInList(list: PhelList): PsiElement? {
        val forms = PsiTreeUtil.getChildrenOfType(list, PhelForm::class.java)

        if (forms == null || forms.size < 2) {
            return null
        }

        // Check if this is a defining form
        val defKeyword = PsiTreeUtil.findChildOfType(forms[0], PhelSymbol::class.java)
        val keywordText = defKeyword?.text

        if (defKeyword == null || !isDefiningKeyword(keywordText)) {
            return null
        }

        // Get the defined name (second element)
        val definedName = PsiTreeUtil.findChildOfType(forms[1], PhelSymbol::class.java)

        if (definedName != null && symbolName == definedName.text) {
            return definedName
        }

        return null
    }

    /**
     * Check if a keyword is a defining form.
     */
    private fun isDefiningKeyword(keyword: String?): Boolean {
        if (keyword == null) return false

        return keyword == "def" || keyword == "defn" || keyword == "defmacro" || keyword == "defstruct" || keyword == "declare" || keyword == "def-" || keyword == "defn-" || keyword == "defmacro-"
    }

    companion object {
        /**
         * Calculate the text range within the symbol that represents the reference.
         * For qualified symbols like "ns/symbol", we only reference the symbol part.
         */
        private fun calculateRangeInElement(element: PhelSymbol): TextRange {
            val text = element.text ?: return TextRange.EMPTY_RANGE

            // For qualified symbols, reference only the name part after '/'
            val slashIndex = text.lastIndexOf('/')
            if (slashIndex >= 0 && slashIndex < text.length - 1) {
                return TextRange.from(slashIndex + 1, text.length - slashIndex - 1)
            }

            // For unqualified symbols, reference the entire text
            return TextRange.from(0, text.length)
        }
    }
}
