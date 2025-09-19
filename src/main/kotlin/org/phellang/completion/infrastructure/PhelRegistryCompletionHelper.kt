package org.phellang.completion.infrastructure

import com.intellij.codeInsight.completion.CompletionResultSet
import org.phellang.completion.data.PhelFunctionRegistry

object PhelRegistryCompletionHelper {

    @JvmStatic
    fun addCoreFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "core")
    }

    @JvmStatic
    fun addStringFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "str")
    }

    @JvmStatic
    fun addJsonFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "json")
    }

    @JvmStatic
    fun addHttpFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "http")
    }

    @JvmStatic
    fun addHtmlFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "html")
    }

    @JvmStatic
    fun addTestFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "test")
    }

    @JvmStatic
    fun addReplFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "repl")
    }

    @JvmStatic
    fun addBase64Functions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "base64")
    }

    @JvmStatic
    fun addPhpInteropFunctions(result: CompletionResultSet) {
        addNamespaceFunctions(result, "php")
    }

    private fun addNamespaceFunctions(result: CompletionResultSet, namespace: String) {
        val functions = PhelFunctionRegistry.getFunctions(namespace)
        functions.forEach { function ->
            PhelCompletionUtils.addRankedCompletion(
                result,
                function.name,
                function.signature,
                function.description,
                function.priority
            )
        }
    }
}
