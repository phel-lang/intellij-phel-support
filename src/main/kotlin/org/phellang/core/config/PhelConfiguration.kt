package org.phellang.core.config

/**
 * Core configuration management for Phel plugin.
 * Centralized configuration to avoid scattered settings throughout the codebase.
 * This replaces the old PhelSettings with better organization.
 */
object PhelConfiguration {

    // Performance settings
    const val MAX_COMPLETION_RESULTS = 50
    const val MAX_PROJECT_SEARCH_DEPTH = 100
    const val COMPLETION_TIMEOUT_MS = 1000L
    const val MAX_PSI_TREE_DEPTH = 50
    const val MAX_FILE_SIZE_KB = 100

    // Completion behavior settings
    const val ENABLE_CONTEXT_AWARE_COMPLETION = true
    const val ENABLE_PERFORMANCE_OPTIMIZATION = true
    const val ENABLE_LOCAL_SYMBOL_PRIORITY = true
    const val ENABLE_PHP_INTEROP_COMPLETION = true
    const val ENABLE_TEMPLATE_COMPLETION = true

    // UI settings
    const val SHOW_PARAMETER_HINTS = true
    const val SHOW_DOCUMENTATION_POPUP = true

    // Folding settings
    const val MIN_FOLDING_LENGTH = 15
    const val AUTO_COLLAPSE_NS_FORMS = true
    const val ENABLE_HYBRID_FOLDING = true

    // Debug and logging
    const val ENABLE_COMPLETION_LOGGING = false
    const val ENABLE_PERFORMANCE_MONITORING = false
    
    // Language feature settings
    const val ENABLE_FORM_COMMENT_HIGHLIGHTING = true
    const val ENABLE_SEMANTIC_HIGHLIGHTING = true
    const val ENABLE_SYNTAX_VALIDATION = true
    
    // Completion priorities (lower number = higher priority)
    const val PRIORITY_PARENTHESES = 10
    const val PRIORITY_TEMPLATES = 20
    const val PRIORITY_LOCAL_SYMBOLS = 30
    const val PRIORITY_NAMESPACE_FUNCTIONS = 40
    const val PRIORITY_CORE_FUNCTIONS = 50
    const val PRIORITY_PHP_INTEROP = 60
    
    // Completion categories
    object Categories {
        const val TEMPLATE = "template"
        const val LOCAL_SYMBOL = "local_symbol"
        const val NAMESPACE_FUNCTION = "namespace_function"
        const val CORE_FUNCTION = "core_function"
        const val PHP_INTEROP = "php_interop"
        const val KEYWORD = "keyword"
        const val LITERAL = "literal"
    }
    
    // Common Phel language constructs
    object Language {
        val DEFINING_FORMS = setOf(
            "def", "defn", "defmacro", "defstruct", "definterface", 
            "defexception", "declare", "def-", "defn-", "defmacro-"
        )
        
        val BINDING_FORMS = setOf(
            "let", "for", "binding", "loop", "foreach", "dofor"
        )
        
        val FUNCTION_FORMS = setOf(
            "defn", "defn-", "defmacro", "defmacro-", "fn"
        )
        
        val SPECIAL_FORMS = setOf(
            "def", "defn", "let", "if", "when", "fn", "do", "quote", 
            "var", "throw", "try", "catch", "finally"
        )
    }
    
    // File extensions and patterns
    object Files {
        const val PHEL_EXTENSION = "phel"
        val PHEL_FILE_PATTERN = "*.phel"
        val IGNORED_DIRECTORIES = setOf(".git", ".idea", "node_modules", "vendor")
    }
}
