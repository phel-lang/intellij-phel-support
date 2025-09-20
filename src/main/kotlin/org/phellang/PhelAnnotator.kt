package org.phellang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.phellang.core.psi.PhelSymbolAnalyzer
import org.phellang.language.psi.*
import org.phellang.language.psi.PhelAccess
import org.phellang.language.psi.impl.PhelVecImpl
import java.util.regex.Pattern

/**
 * Advanced annotator for context-sensitive Phel syntax highlighting
 * Handles PHP interop, built-in functions, and namespace-aware highlighting
 */
class PhelAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Check if this element is commented out by #_ form comment
        if (isCommentedOutByFormComment(element)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element.textRange)
                .textAttributes(COMMENTED_OUT_FORM).create()
            return  // Don't apply other highlighting to commented-out forms
        }

        // Check if this element is inside a short function - if so, skip highlighting
        if (isInsideShortFunction(element)) {
            return
        }

        if (element is PhelKeyword) {
            annotatePhelKeyword(element, holder)
        } else if (element is PhelSymbol) {
            val text = element.text

            if (text != null) {
                annotateSymbol(element, text, holder)
            }
        } else if (element is PhelShortFn) {
            annotateShortFn(element, holder)
        } else if (element is PhelSet) {
            annotateSet(element, holder)
        } else if (element.node.elementType == PhelTypes.HASH_BRACE) {
            // Highlight the #{ token as part of a set
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element.textRange)
                .textAttributes(COLLECTION_TYPE).create()
        } else if (element.node.elementType == PhelTypes.BRACE2 && isInsideSet(element)) {
            // Highlight the } token as part of a set (not a map)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element.textRange)
                .textAttributes(COLLECTION_TYPE).create()
        }
    }

    private fun annotateSymbol(symbol: PhelSymbol, text: String, holder: AnnotationHolder) {
        // Function parameters (check first, before other classifications)
        if (PhelSymbolAnalyzer.isDefinition(symbol)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(FUNCTION_PARAMETER).create()
            return
        }

        // Check for PHP interop patterns that span multiple tokens (php/SYMBOL)
        val phpInteropRange = findPhpInteropRange(symbol)
        if (phpInteropRange != null) {
            // Only create annotation if this is the qualified symbol itself, not a sub-element
            if (phpInteropRange == symbol.textRange) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(phpInteropRange)
                    .textAttributes(PHP_INTEROP).create()
                return
            } else {
                // This is a sub-element of a qualified symbol, skip highlighting here
                // The qualified symbol will be highlighted when it's processed
                return
            }
        }

        // PHP interop patterns (single token)
        if (isPhpInterop(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(PHP_INTEROP).create()
            return
        }

        // PHP variables (superglobals)
        if (isPhpVariable(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(PHP_VARIABLE).create()
            return
        }

        // Special forms (highest priority)
        if (SPECIAL_FORMS.contains(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(SPECIAL_FORM).create()
            return
        }

        // Macros
        if (MACROS.contains(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(SPECIAL_FORM).create()
            return
        }

        // Core functions
        if (CORE_FUNCTIONS.contains(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(SPECIAL_FORM).create()
            return
        }

        // Function call position - highlight user-defined functions in function position
        if (isInFunctionCallPosition(symbol)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(SPECIAL_FORM).create()
            return
        }

        // Namespace prefix highlighting
        if (hasNamespacePrefix(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(symbol.textRange)
                .textAttributes(NAMESPACE_PREFIX).create()
            return
        }
    }

    private fun isInFunctionCallPosition(symbol: PhelSymbol): Boolean {
        var current = symbol.parent
        while (current != null && current !is PhelList) {
            current = current.parent
        }

        if (current is PhelList) {
            val list = current

            // Get the first form in the list (function name position)
            val firstForm = list.forms.firstOrNull()

            if (firstForm is PhelSymbol) {
                return firstForm === symbol
            } else if (firstForm is PhelAccess) {
                // Check if our symbol is contained within the first form (access)
                return firstForm.textRange.contains(symbol.textRange)
            }
        }

        return false
    }

    /**
     * Find PHP interop range that spans multiple tokens (php/SYMBOL).
     * Returns the TextRange for the entire php/SYMBOL construction, or null if not found.
     */
    private fun findPhpInteropRange(symbol: PhelSymbol): TextRange? {
        val text = symbol.text ?: return null

        // Check if this is the "php" part of a php/SYMBOL construction
        if (text == "php") {
            return findPhpInteropRangeFromPhp(symbol)
        }

        // Check if this is the SYMBOL part of a php/SYMBOL construction
        // This includes both operators and function names
        if (isPhpInteropSymbol(text) || isPhpFunctionName(text)) {
            return findPhpInteropRangeFromSymbol(symbol)
        }

        return null
    }

    private fun findPhpInteropRangeFromPhp(phpSymbol: PhelSymbol): TextRange? {
        val parent = phpSymbol.parent

        // Check if this is a qualified symbol (php/SYMBOL structure)
        if (parent is PhelSymbol) {
            val qualifiedText = parent.text

            if (qualifiedText != null && qualifiedText.startsWith("php/")) {
                val symbolPart = qualifiedText.substring(4) // Remove "php/" prefix

                if (isPhpInteropSymbol(symbolPart) || isPhpFunctionName(symbolPart)) {
                    // Return range for the entire qualified symbol
                    return parent.textRange
                }
            }
        }

        // Fallback to the old logic for list-based structures
        if (parent !is PhelList) return null

        val children = parent.children
        val phpIndex = children.indexOf(phpSymbol)
        if (phpIndex == -1 || phpIndex + 2 >= children.size) return null

        // Check if next token is a slash
        val nextToken = children[phpIndex + 1]
        if (nextToken.node.elementType != PhelTypes.SLASH) return null

        // Check if token after slash is a symbol
        val symbolToken = children[phpIndex + 2]
        if (symbolToken !is PhelSymbol) return null

        val symbolText = symbolToken.text
        if (symbolText == null || !isPhpInteropSymbol(symbolText)) return null

        // Return range from start of "php" to end of symbol
        val startOffset = phpSymbol.textRange.startOffset
        val endOffset = symbolToken.textRange.endOffset
        return TextRange.create(startOffset, endOffset)
    }

    /**
     * Find PHP interop range when we have the SYMBOL token.
     */
    private fun findPhpInteropRangeFromSymbol(symbol: PhelSymbol): TextRange? {
        val parent = symbol.parent

        // Check if this is a qualified symbol (php/SYMBOL structure)
        if (parent is PhelSymbol) {
            val qualifiedText = parent.text

            if (qualifiedText != null && qualifiedText.startsWith("php/")) {
                val symbolPart = qualifiedText.substring(4) // Remove "php/" prefix

                if (isPhpInteropSymbol(symbolPart) || isPhpFunctionName(symbolPart)) {
                    // Return range for the entire qualified symbol
                    return parent.textRange
                }
            }
        }

        // Fallback to the old logic for list-based structures
        if (parent !is PhelList) return null

        val children = parent.children
        val symbolIndex = children.indexOf(symbol)
        if (symbolIndex < 2) return null

        // Check if previous tokens are "php" and "/"
        val slashToken = children[symbolIndex - 1]
        if (slashToken.node.elementType != PhelTypes.SLASH) return null

        val phpToken = children[symbolIndex - 2]
        if (phpToken !is PhelSymbol || phpToken.text != "php") return null

        // Return range from start of "php" to end of symbol
        val startOffset = phpToken.textRange.startOffset
        val endOffset = symbol.textRange.endOffset
        return TextRange.create(startOffset, endOffset)
    }

    private fun isPhpInteropSymbol(text: String): Boolean {
        return VALID_PHP_SYMBOLS.contains(text)
    }

    private fun isPhpFunctionName(text: String): Boolean {
        // Check if it's a valid PHP identifier (starts with letter or underscore, followed by letters, digits, underscores)
        if (text.isEmpty()) return false

        val firstChar = text[0]
        if (!firstChar.isLetter() && firstChar != '_') return false

        // Check remaining characters
        for (i in 1 until text.length) {
            val char = text[i]
            if (!char.isLetterOrDigit() && char != '_') return false
        }

        return true
    }

    private fun isPhpInterop(text: String): Boolean {
        return text.startsWith("php/")
    }

    private fun isPhpVariable(text: String): Boolean {
        return text.startsWith("php/$")
    }

    private fun hasNamespacePrefix(text: String): Boolean {
        // Check for namespace separators: / or \
        return (text.contains("/") && !text.startsWith("php/")) || text.contains("\\")
    }

    /**
     * Annotate keyword elements to ensure proper highlighting
     */
    private fun annotatePhelKeyword(keyword: PhelKeyword, holder: AnnotationHolder) {
        // Highlight the entire keyword element
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(keyword.textRange)
            .textAttributes(PhelSyntaxHighlighter.KEYWORD).create()
    }

    /**
     * Annotate short function syntax |(fn [x] x)
     */
    private fun annotateShortFn(shortFn: PhelShortFn, holder: AnnotationHolder) {
        // Highlight the entire short function as a special form
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(shortFn.textRange).textAttributes(SPECIAL_FORM)
            .create()
    }

    /**
     * Annotate set data structure #{1 2 3}
     */
    private fun annotateSet(set: PhelSet, holder: AnnotationHolder) {
        // Highlight the entire set as a collection type
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(set.textRange).textAttributes(COLLECTION_TYPE)
            .create()
    }

    /**
     * Check if an element is inside a short function
     */
    private fun isInsideShortFunction(element: PsiElement): Boolean {
        var current = element.parent
        while (current != null) {
            if (current is PhelShortFn) {
                return true
            }
            current = current.parent
        }
        return false
    }

    /**
     * Check if an element is inside a set
     */
    private fun isInsideSet(element: PsiElement): Boolean {
        var current = element.parent
        while (current != null) {
            if (current is PhelSet) {
                return true
            }
            current = current.parent
        }
        return false
    }

    /**
     * Check if an element is commented out by #_ form comment(s).
     * Handles stacking of multiple #_ tokens (e.g., #_#_ comments out two forms).
     */
    private fun isCommentedOutByFormComment(element: PsiElement): Boolean {
        // First try text-based detection for vectors (workaround for grammar bug)
        if (isInCommentedVectorByText(element)) {
            return true
        }

        // Find the form that contains this element
        val containingForm: PhelForm = findContainingForm(element) ?: return false

        // Check if this form is commented out by counting preceding #_ tokens
        // First check the containing form itself, then check parent forms
        return isFormCommentedOut(containingForm) || isParentFormCommentedOut(containingForm)
    }

    /**
     * Find the immediate form that contains the given element.
     */
    private fun findContainingForm(element: PsiElement): PhelForm? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PhelForm) {
                return current
            }
            current = current.parent
        }
        return null
    }

    /**
     * Check if a form is commented out by counting preceding #_ tokens.
     * Handles stacking: #_ comments out 1 form, #_#_ comments out 2 forms, etc.
     */
    private fun isFormCommentedOut(form: PhelForm): Boolean {
        val parent = form.parent ?: return false

        // Get all children of the parent to analyze the sequence
        val children = parent.children
        var formIndex = -1

        // Find the index of our form
        for (i in children.indices) {
            if (children[i] === form) {
                formIndex = i
                break
            }
        }

        if (formIndex == -1) {
            return false
        }

        // Find the sequence of forms that this form belongs to
        // and check if #_ tokens precede that sequence

        // Find the start of the form sequence that contains our form
        var sequenceStart = formIndex
        for (i in formIndex - 1 downTo 0) {
            val child: PsiElement = children[i]
            if (child is PsiWhiteSpace) {
                continue  // Skip whitespace
            } else if (child is PhelForm) {
                sequenceStart = i // Found another form, extend sequence backwards
            } else {
                break // Hit non-form, non-whitespace - end of sequence
            }
        }

        // Count consecutive #_ tokens immediately before the sequence
        var consecutiveFormComments = 0
        for (i in sequenceStart - 1 downTo 0) {
            val child = children[i]

            if (child is PsiWhiteSpace) {
                continue  // Skip whitespace
            } else if (child.node != null && child.node.elementType === PhelTypes.FORM_COMMENT) {
                consecutiveFormComments++
            } else if (isFormCommentMacro(child)) {
                consecutiveFormComments++
            } else {
                break
            }
        }

        if (consecutiveFormComments == 0) {
            return false
        }

        // Determine position of our form within the sequence
        var formPosition = 0
        for (i in sequenceStart..formIndex) {
            val child: PsiElement = children[i]
            if (child is PhelForm) {
                formPosition++
                if (child === form) {
                    break
                }
            }
        }

        return formPosition <= consecutiveFormComments
    }

    /**
     * Check if any parent form is commented out by #_. This handles cases like [#_:one :two]
     * where :one and :two are inside a vector that is commented out.
     */
    private fun isParentFormCommentedOut(form: PhelForm): Boolean {
        var parent = form.parent
        while (parent != null) {
            if (parent is PhelForm) {
                val parentForm = parent
                if (isFormCommentedOut(parentForm)) {
                    return true
                }
                parent = parent.parent
            } else {
                break
            }
        }
        return false
    }

    /**
     * Check if a PSI element is a form_comment_macro (i.e., #_ form).
     */
    private fun isFormCommentMacro(element: PsiElement): Boolean {
        // Check if this is a form_comment_macro by looking at its structure
        if (element.node == null) {
            return false
        }

        // Look for elements that start with #_ token
        val children = element.children
        if (children.size > 0) {
            val firstChild = children[0]
            if (firstChild.node != null && firstChild.node.elementType === PhelTypes.FORM_COMMENT) {
                return true
            }
        }

        // Also check if the element itself is the form_comment token
        return element.node.elementType === PhelTypes.FORM_COMMENT
    }

    /**
     * Workaround for grammar bug: detect form comments by analyzing text content.
     * This handles cases like [#_:one :two] where the PSI tree is missing #_ tokens.
     */
    private fun isInCommentedVectorByText(element: PsiElement): Boolean {
        // Check if we're inside a vector
        val parent = element.parent
        if (parent !is PhelVecImpl) {
            return false
        }

        val vectorText = parent.text

        // Find element's position within the vector text
        val elementOffsetInVector = element.textOffset - parent.textOffset

        // Look for #_ patterns before this element's position
        val beforeElement = vectorText.take(elementOffsetInVector)

        // Simple regex to find #_ patterns
        val pattern = Pattern.compile("#_")
        val matcher = pattern.matcher(beforeElement)

        var formCommentCount = 0
        while (matcher.find()) {
            formCommentCount++
        }

        if (formCommentCount > 0) {
            // Count how many actual forms (keywords/symbols) appear before this element
            val textBeforeForCounting = beforeElement.replace("#_".toRegex(), "").trim { it <= ' ' }

            // Use regex to find all keywords and symbols in the text
            val formPattern = Pattern.compile("(:[\\w-]+|[a-zA-Z][\\w-]*)")
            val formMatcher = formPattern.matcher(textBeforeForCounting)

            var formsBeforeCount = 0
            while (formMatcher.find()) {
                formsBeforeCount++
            }

            // If this element is at position N (0-based) and there are M #_ tokens,
            // then it's commented out if N < M
            return formsBeforeCount < formCommentCount
        }

        return false
    }
}

// PHP interop highlighting
@JvmField
val PHP_INTEROP: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "PHEL_PHP_INTEROP", DefaultLanguageHighlighterColors.STATIC_METHOD
)

@JvmField
val PHP_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "PHEL_PHP_VARIABLE", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE
)

@JvmField
val SPECIAL_FORM: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("PHEL_SPECIAL_FORM", DefaultLanguageHighlighterColors.KEYWORD)

// Namespace highlighting
@JvmField
val NAMESPACE_PREFIX: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("PHEL_NAMESPACE", DefaultLanguageHighlighterColors.IDENTIFIER)

// Form comment highlighting - for forms commented out by #_
@JvmField
val COMMENTED_OUT_FORM: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "PHEL_COMMENTED_OUT_FORM", DefaultLanguageHighlighterColors.LINE_COMMENT
)

// Function parameter highlighting (more distinct color)
@JvmField
val FUNCTION_PARAMETER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "PHEL_FUNCTION_PARAMETER", DefaultLanguageHighlighterColors.INSTANCE_FIELD
)

// Collection type highlighting (for sets, vectors, maps, lists)
@JvmField
val COLLECTION_TYPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "PHEL_COLLECTION_TYPE", DefaultLanguageHighlighterColors.STATIC_METHOD
)

// Core Phel special forms
private val SPECIAL_FORMS = mutableSetOf<String?>(
    "def",
    "fn",
    "if",
    "let",
    "do",
    "quote",
    "var",
    "recur",
    "throw",
    "try",
    "catch",
    "finally",
    "loop",
    "case",
    "cond",
    "when",
    "when-not",
    "when-let",
    "if-let",
    "if-not",
    "ns",
    "defn",
    "defn-",
    "defmacro",
    "defmacro-",
    "defstruct",
    "definterface"
)

// Core Phel macros
private val MACROS = mutableSetOf<String?>(
    "and",
    "or",
    "->",
    "->>",
    "as->",
    "cond->",
    "cond->>",
    "some->",
    "some->>",
    "doto",
    "for",
    "dofor",
    "foreach",
    "comment",
    "declare"
)

// Valid PHP interop symbols
private val VALID_PHP_SYMBOLS = setOf(
    "===",
    "!==",
    "==",
    "!=",
    "&&",
    "||",
    "<<",
    ">>",
    "++",
    "--",
    "^",
    "~",
    "+",
    "-",
    "*",
    "/",
    "%",
    "&",
    "|",
    "<",
    ">",
    "<=",
    ">=",
    "=",
    "!",
    "::",
    "->"
)

// Common core functions (subset for performance)
private val CORE_FUNCTIONS = mutableSetOf<String?>(
    // Collection operations
    "map",
    "filter",
    "reduce",
    "apply",
    "count",
    "first",
    "rest",
    "cons",
    "conj",
    "get",
    "get-in",
    "assoc",
    "dissoc",
    "update",
    "merge",
    "keys",
    "values",
    "peek",
    "pop",
    "push",
    "remove",
    "second",
    "ffirst",
    "next",
    "nnext",
    "nfirst",
    "set",

    // Sequence operations
    "take",
    "drop",
    "take-while",
    "drop-while",
    "take-nth",
    "take-last",
    "distinct",
    "reverse",
    "sort",
    "sort-by",
    "shuffle",
    "flatten",
    "range",
    "repeat",
    "interleave",
    "interpose",
    "partition",
    "split-at",
    "split-with",

    // String operations
    "str",
    "print",
    "println",
    "print-str",
    "format",
    "printf",
    "str-contains?",
    "slurp",
    "spit",

    // Function operations
    "identity",
    "comp",
    "partial",
    "constantly",
    "complement",
    "juxt",
    "memoize",

    // Math operations
    "inc",
    "dec",
    "sum",
    "mean",
    "extreme",
    "+",
    "-",
    "*",
    "/",
    "mod",
    "rem",
    "quot",
    "=",
    "not=",
    "<",
    "<=",
    ">",
    ">=",
    "min",
    "max",
    "abs",

    // Predicates
    "pos?",
    "neg?",
    "zero?",
    "one?",
    "even?",
    "odd?",
    "nil?",
    "some?",
    "all?",
    "true?",
    "false?",
    "truthy?",
    "boolean?",
    "number?",
    "string?",
    "keyword?",
    "symbol?",
    "list?",
    "vector?",
    "map?",
    "set?",
    "seq?",
    "coll?",
    "empty?",
    "not-empty",
    "float?",
    "int?",
    "function?",
    "struct?",
    "hash-map?",
    "php-array?",
    "php-resource?",
    "php-object?",
    "indexed?",
    "associative?",
    "var?",

    // Random functions
    "rand",
    "rand-int",
    "rand-nth",

    // Meta and symbol operations
    "meta",
    "name",
    "namespace",
    "full-name",
    "symbol",
    "keyword",
    "gensym",

    // Evaluation and compilation
    "eval",
    "compile",
    "macroexpand",
    "macroexpand-1",
    "read-string",

    // Bit operations
    "bit-and",
    "bit-or",
    "bit-xor",
    "bit-not",
    "bit-shift-left",
    "bit-shift-right",
    "bit-set",
    "bit-clear",
    "bit-flip",
    "bit-test",

    // Other core functions
    "not",
    "compare",
    "type",
    "contains?",
    "find",
    "find-index",
    "frequencies",
    "group-by",
    "union",
    "intersection",
    "difference",
    "symmetric-difference",
    "select-keys",
    "invert",
    "merge-with",
    "deep-merge",
    "disj",

    // String module functions (phel\str)
    "split",
    "join",
    "replace",
    "replace-first",
    "trim",
    "triml",
    "trimr",
    "capitalize",
    "lower-case",
    "upper-case",
    "starts-with?",
    "ends-with?",
    "blank?",
    "includes?",
    "index-of",
    "last-index-of",
    "split-lines",
    "pad-left",
    "pad-right",
    "trim-newline",
    "escape",
    "re-quote-replacement",

    // JSON module functions (phel\json)
    "encode",
    "decode",

    // Base64 module functions (phel\base64)
    "encode-url",
    "decode-url",

    // HTML module functions (phel\html)
    "escape-html",
    "doctype",

    // Test module functions (phel\test)
    "report",
    "print-summary",
    "run-tests",
    "successful?",

    // REPL module functions (phel\repl)
    "loaded-namespaces",
    "resolve",
    "print-colorful",
    "println-colorful",
    "compile-str",

    // Trace module functions (phel\trace)
    "dotrace",
    "reset-trace-state!",
    "set-trace-id-padding!",

    // HTTP module functions (phel\http) - most commonly used
    "request-from-globals",
    "response-from-map",
    "response-from-string",
    "emit-response"
)