package org.phellang.completion.infrastructure

/**
 * Higher values appear first in completion lists.
 */
enum class PhelCompletionPriority(val value: Double) {
    // Local scope symbols
    CURRENT_SCOPE_LOCALS(100.0),
    RECENT_DEFINITIONS(90.0),

    // Essential language constructs
    SPECIAL_FORMS(80.0),
    CONTROL_FLOW(75.0),

    // Commonly used functions
    COMMON_BUILTINS(60.0),
    CORE_FUNCTIONS(55.0),
    STRING_FUNCTIONS(52.0),

    // General API functions
    COLLECTION_FUNCTIONS(38.0),
    PREDICATE_FUNCTIONS(35.0),
    ARITHMETIC_FUNCTIONS(32.0),

    // Namespace-specific functions
    HTML_FUNCTIONS(24.0),
    HTTP_FUNCTIONS(23.0),
    JSON_FUNCTIONS(22.0),
    TEST_FUNCTIONS(21.0),

    // Specialized and external functions
    REPL_FUNCTIONS(16.0),
    BASE64_FUNCTIONS(15.0),
    PHP_INTEROP(12.0),
    PROJECT_SYMBOLS(10.0),

    // Less common elements
    MACROS(5.0),
    DEPRECATED_FUNCTIONS(1.0);
}
