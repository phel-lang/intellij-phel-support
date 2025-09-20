package org.phellang.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import org.phellang.PhelIcons
import javax.swing.Icon

/**
 * Centralized icon management for consistent iconography across the plugin.
 * Provides semantic icon access rather than scattered icon references.
 */
object PhelIconManager {
    
    // Language-specific icons
    val PHEL_FILE: Icon = PhelIcons.FILE
    
    // Completion category icons
    val FUNCTION: Icon = AllIcons.Nodes.Function
    val MACRO: Icon = AllIcons.Nodes.AbstractMethod
    val SPECIAL_FORM: Icon = AllIcons.Nodes.Method
    val PARAMETER: Icon = AllIcons.Nodes.Parameter
    val LOCAL_VARIABLE: Icon = AllIcons.Nodes.Variable
    val CONSTANT: Icon = AllIcons.Nodes.Constant
    val KEYWORD: Icon = AllIcons.Nodes.PropertyReadWrite
    
    // PHP interop icons
    val PHP_INTEROP: Icon = AllIcons.Nodes.Static
    val PHP_CLASS: Icon = AllIcons.Nodes.Class
    val PHP_VARIABLE: Icon = AllIcons.Nodes.Field
    
    // Namespace and module icons
    val NAMESPACE: Icon = AllIcons.Nodes.Package
    val MODULE: Icon = AllIcons.Nodes.Module
    
    // Context-specific icons
    val PREDICATE: Icon = AllIcons.Nodes.Method
    val COLLECTION_FUNCTION: Icon = AllIcons.Nodes.Function
    val STRING_FUNCTION: Icon = AllIcons.Nodes.Function
    val ARITHMETIC_FUNCTION: Icon = AllIcons.Nodes.Function
    
    // Template and structural icons
    val TEMPLATE: Icon = AllIcons.Nodes.Template
    val STRUCTURE: Icon = AllIcons.Nodes.Artifact
    val EXPRESSION: Icon = AllIcons.General.Add
    
    // Documentation and information icons
    val DOCUMENTATION: Icon = AllIcons.General.Information
    val WARNING: Icon = AllIcons.General.Warning
    val ERROR: Icon = AllIcons.General.Error
    
    /**
     * Get icon based on completion context and symbol type.
     */
    fun getContextualIcon(symbolType: SymbolType, context: CompletionContext = CompletionContext.GENERAL): Icon {
        return when (symbolType) {
            SymbolType.FUNCTION -> when (context) {
                CompletionContext.PREDICATE -> PREDICATE
                CompletionContext.COLLECTION -> COLLECTION_FUNCTION  
                CompletionContext.STRING -> STRING_FUNCTION
                CompletionContext.ARITHMETIC -> ARITHMETIC_FUNCTION
                else -> FUNCTION
            }
            SymbolType.MACRO -> MACRO
            SymbolType.SPECIAL_FORM -> SPECIAL_FORM
            SymbolType.PARAMETER -> PARAMETER
            SymbolType.LOCAL_VARIABLE -> LOCAL_VARIABLE
            SymbolType.CONSTANT -> CONSTANT
            SymbolType.KEYWORD -> KEYWORD
            SymbolType.PHP_INTEROP -> PHP_INTEROP
            SymbolType.PHP_CLASS -> PHP_CLASS
            SymbolType.PHP_VARIABLE -> PHP_VARIABLE
            SymbolType.NAMESPACE -> NAMESPACE
            SymbolType.TEMPLATE -> TEMPLATE
            SymbolType.STRUCTURE -> STRUCTURE
        }
    }
    
    enum class SymbolType {
        FUNCTION,
        MACRO,
        SPECIAL_FORM,
        PARAMETER,
        LOCAL_VARIABLE,
        CONSTANT,
        KEYWORD,
        PHP_INTEROP,
        PHP_CLASS,
        PHP_VARIABLE,
        NAMESPACE,
        TEMPLATE,
        STRUCTURE
    }
    
    enum class CompletionContext {
        GENERAL,
        PREDICATE,
        COLLECTION,
        STRING,
        ARITHMETIC,
        PHP_INTEROP
    }
}
