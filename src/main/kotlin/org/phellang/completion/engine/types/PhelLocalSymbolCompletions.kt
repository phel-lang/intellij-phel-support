package org.phellang.completion.engine.types

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.phellang.PhelFileType
import org.phellang.completion.infrastructure.PhelCompletionErrorHandler
import org.phellang.completion.infrastructure.PhelCompletionUtils
import org.phellang.core.utils.PhelPerformanceUtils
import org.phellang.language.psi.PhelFile
import org.phellang.language.psi.PhelList
import org.phellang.language.psi.PhelSymbol
import org.phellang.language.psi.PhelVec
import org.phellang.language.psi.impl.PhelAccessImpl
import javax.swing.Icon

/**
 * Provides completions for parameters, let bindings, and other local variables in the current scope
 */
object PhelLocalSymbolCompletions {
    val PARAMETER_ICON = AllIcons.Nodes.Parameter
    val METHOD_ICON = AllIcons.Nodes.Method
    val VARIABLE_ICON = AllIcons.Nodes.Variable

    @JvmStatic
    fun addLocalSymbols(result: CompletionResultSet, position: PsiElement) {
        // Comprehensive error handling for local symbol completion
        PhelCompletionErrorHandler.withErrorHandling(PhelCompletionErrorHandler.withResultSet {
            addLocalSymbolsWithValidation(result, position)
        }, "local symbol completion") {
            // Fallback: add basic local symbols if detailed analysis fails
            addBasicLocalFallback(result, position)
        }
    }

    @Throws(Exception::class)
    private fun addLocalSymbolsWithValidation(result: CompletionResultSet, position: PsiElement) {
        // Performance check - skip expensive operations if needed
        if (PhelPerformanceUtils.shouldSkipExpensiveOperations(position)) {
            return
        }

        // Validate element before processing
        check(PhelCompletionErrorHandler.isCompletionContextValid(position)) { "Invalid PSI element context for local symbol completion" }

        val addedSymbols: MutableSet<String?> = HashSet()

        // First priority: Add function parameters from current scope
        addCurrentFunctionParameters(result, position, addedSymbols)
        
        // Second priority: Add let bindings from current scope
        addCurrentLetBindings(result, position, addedSymbols)
        
        // Third priority: Add local definitions
        addLocalDefinitionSymbolsSimple(result, position, addedSymbols)
        
        // Lower priority: Add global definitions from other files
        addProjectGlobalDefinitions(result, position, addedSymbols)
    }

    private fun addSymbolCompletion(
        result: CompletionResultSet,
        symbolName: String,
        typeText: String,
        icon: Icon?,
        addedSymbols: MutableSet<String?>
    ) {
        if (!addedSymbols.contains(symbolName) && !symbolName.trim { it <= ' ' }.isEmpty()) {
            addedSymbols.add(symbolName)

            PhelCompletionUtils.addLocalSymbolCompletion(result, symbolName, typeText, icon)
        }
    }

    /**
     * Add parameters from the current function scope
     */
    private fun addCurrentFunctionParameters(result: CompletionResultSet, position: PsiElement, addedSymbols: MutableSet<String?>) {
        var current = position.parent
        var depth = 0
        
        // Walk up the PSI tree to find the containing function definition
        while (current != null && depth < 10) {
            if (current is PhelList) {
                val children = current.children
                if (children.isNotEmpty()) {
                    val firstChild = children[0]
                    if (firstChild is PhelSymbol || firstChild is PhelAccessImpl) {
                        val functionType = firstChild.text
                        
                        // Check if this is a function definition
                        if (functionType == "defn" || functionType == "defn-" || functionType == "defmacro" || functionType == "defmacro-" || functionType == "fn") {
                            // Find the parameter vector
                            val paramVectorIndex = if (functionType == "fn") 1 else 2
                            
                            if (children.size > paramVectorIndex) {
                                val paramElement = children[paramVectorIndex]
                                if (paramElement is PhelVec) {
                                    // Extract parameters from the vector
                                    val paramChildren = paramElement.children
                                    for (paramChild in paramChildren) {
                                        if (paramChild is PhelSymbol || paramChild is PhelAccessImpl) {
                                            val paramName = paramChild.text
                                            if (paramName != null && paramName.isNotEmpty()) {
                                                addSymbolCompletion(result, paramName, "Function Parameter", PARAMETER_ICON, addedSymbols)
                                            }
                                        }
                                    }
                                }
                            }
                            break // Found the function, stop looking
                        }
                    }
                }
            }
            current = current.parent
            depth++
        }
    }

    private fun addCurrentLetBindings(result: CompletionResultSet, position: PsiElement, addedSymbols: MutableSet<String?>) {
        var current = position.parent
        var depth = 0
        
        // Walk up the PSI tree to find let bindings
        while (current != null && depth < 10) {
            if (current is PhelList) {
                val children = current.children
                if (children.isNotEmpty()) {
                    val firstChild = children[0]
                    if (firstChild is PhelSymbol || firstChild is PhelAccessImpl) {
                        val bindingType = firstChild.text
                        
                        // Check if this is a binding form
                        if (bindingType == "let" || bindingType == "for" || bindingType == "loop" || bindingType == "binding") {
                            // Find the binding vector (second element)
                            if (children.size > 1) {
                                val bindingElement = children[1]
                                if (bindingElement is PhelVec) {
                                    // Extract bindings from the vector (every other element is a binding name)
                                    val bindingChildren = bindingElement.children
                                    for (i in bindingChildren.indices step 2) {
                                        val bindingChild = bindingChildren[i]
                                        if (bindingChild is PhelSymbol || bindingChild is PhelAccessImpl) {
                                            val bindingName = bindingChild.text
                                            if (bindingName != null && bindingName.isNotEmpty()) {
                                                val typeText = when (bindingType) {
                                                    "let" -> "Let Binding"
                                                    "loop" -> "Loop Binding"
                                                    else -> "Local Variable"
                                                }
                                                addSymbolCompletion(result, bindingName, typeText, VARIABLE_ICON, addedSymbols)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            current = current.parent
            depth++
        }
    }

    /**
     * Basic fallback for local symbol completion when detailed analysis fails
     */
    private fun addBasicLocalFallback(result: CompletionResultSet, position: PsiElement) {
        try {
            // Try to add at least some basic local symbols using simple PSI traversal
            val currentFunctionName = getCurrentFunctionName(position)
            if (currentFunctionName != null && !currentFunctionName.isEmpty()) {
                result.addElement(
                    LookupElementBuilder.create(currentFunctionName).withIcon(METHOD_ICON)
                        .withTailText(" (current function)", true)
                )
            }
        } catch (_: Exception) {
            // Even fallback failed - provide minimal completions
        }
    }

    /**
     * Add global definitions from other Phel files in the project
     */
    private fun addProjectGlobalDefinitions(
        result: CompletionResultSet, position: PsiElement, addedSymbols: MutableSet<String?>
    ) {
        val project = position.project

        // Skip in large projects for performance
        if (PhelPerformanceUtils.shouldSkipExpensiveOperations(position)) {
            return
        }

        try {
            // Iterate through all project files and filter by type
            val phelFiles: MutableCollection<VirtualFile> = ArrayList()
            ProjectRootManager.getInstance(project).fileIndex.iterateContent { virtualFile: VirtualFile? ->
                    if (phelFiles.size < 20 &&  // Limit for performance
                        virtualFile!!.fileType == PhelFileType.INSTANCE
                    ) {
                        phelFiles.add(virtualFile)
                    }
                    true
                }

            val psiManager = PsiManager.getInstance(project)
            val currentFile = position.containingFile

            var fileCount = 0
            val maxFilesToScan = 20 // Performance limit

            for (virtualFile in phelFiles) {
                if (fileCount >= maxFilesToScan) break

                // Skip current file - already processed
                if (virtualFile == currentFile.virtualFile) continue

                val psiFile = psiManager.findFile(virtualFile)
                if (psiFile is PhelFile) {
                    extractGlobalDefinitionsFromFile(psiFile, result, addedSymbols)
                    fileCount++
                }
            }
        } catch (_: Exception) {
            // Silently ignore errors to avoid disrupting completion
        }
    }

    /**
     * Extract global definitions from a specific Phel file
     */
    private fun extractGlobalDefinitionsFromFile(
        file: PhelFile, result: CompletionResultSet, addedSymbols: MutableSet<String?>
    ) {
        var definitionCount = 0
        val maxDefinitionsPerFile = 10

        for (child in file.children) {
            if (definitionCount >= maxDefinitionsPerFile) break

            if (child is PhelList) {
                val children: Array<PsiElement> = child.children
                if (children.size >= 2) {
                    val firstElement = children[0]

                    if (firstElement is PhelSymbol) {
                        val defType = firstElement.text

                        // Only include publicly accessible definitions
                        if (defType == "defn" || defType == "def" || defType == "defmacro" || defType == "defstruct") {
                            val nameElement = children[1]
                            if (nameElement is PhelSymbol) {
                                val symbolName = nameElement.text
                                val displayType = when (defType) {
                                    "def" -> "Global Variable"
                                    "defn" -> "Public Function"
                                    "defmacro" -> "Public Macro"
                                    else -> "Public Definition"
                                }

                                // Add file context
                                val fileName = file.name.replace(".phel", "")
                                val typeWithContext = "$displayType ($fileName)"

                                addSymbolCompletion(result, symbolName, typeWithContext, METHOD_ICON, addedSymbols)
                                definitionCount++
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Simple version for scanning all local definitions in current file
     */
    private fun addLocalDefinitionSymbolsSimple(
        result: CompletionResultSet, position: PsiElement, addedSymbols: MutableSet<String?>
    ) {
        val file = position.containingFile as PhelFile? ?: return

        // Just iterate through all children without any limits
        for (child in file.children) {
            if (child is PhelList) {
                // Get children array and look for def + variable pattern  
                val children: Array<PsiElement> = child.children

                // Look for (def symbol-name ...) pattern 
                for (i in 0..<children.size - 1) {
                    val defChild = children[i]
                    if ((defChild is PhelSymbol || defChild is PhelAccessImpl) && "def" == defChild.text) {
                        // Check next element for the variable name
                        if (i + 1 < children.size) {
                            val nameChild = children[i + 1]

                            if (nameChild is PhelSymbol || nameChild is PhelAccessImpl) {
                                val symbolName = nameChild.text
                                addSymbolCompletion(
                                    result, symbolName, "Global Variable", METHOD_ICON, addedSymbols
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the name of the function currently being defined at the given position
     * This is used to mark recursive function calls with a special indicator
     */
    private fun getCurrentFunctionName(position: PsiElement): String? {
        var current: PsiElement? = position

        // Walk up the PSI tree to find the containing function definition
        while (current != null) {
            if (current is PhelList) {
                val list = current
                val children: Array<PsiElement> = list.children

                if (children.size >= 2) {
                    val firstElement = children[0]

                    // Check if this is a function definition
                    if (firstElement is PhelSymbol || firstElement is PhelAccessImpl) {
                        val defType = firstElement.text
                        if (defType == "defn" || defType == "defn-" || defType == "defmacro" || defType == "defmacro-") {
                            // Get the function name (second element)
                            val nameElement = children[1]
                            if (nameElement is PhelSymbol || nameElement is PhelAccessImpl) {
                                return nameElement.text
                            }
                        }
                    }
                }
            }
            current = current.parent
        }

        return null // Not inside a function definition
    }
}
