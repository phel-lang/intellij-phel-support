package org.phellang.core.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.phellang.core.config.PhelConfiguration

/**
 * Centralized error handling for Phel plugin operations.
 * Provides consistent error handling patterns and logging with enhanced functionality.
 */
object PhelErrorHandler {
    @JvmStatic
    val LOG = Logger.getInstance(PhelErrorHandler::class.java)

    /**
     * Execute operation with comprehensive error handling.
     * Provides fallback mechanism if primary operation fails.
     */
    inline fun <T> withErrorHandling(
        operation: () -> T,
        fallback: () -> T,
        operationName: String
    ): T {
        return try {
            operation()
        } catch (e: Exception) {
            LOG.warn("Error in $operationName: ${e.message}", e)
            try {
                fallback()
            } catch (fallbackException: Exception) {
                LOG.error("Fallback also failed for $operationName", fallbackException)
                throw fallbackException
            }
        }
    }

    /**
     * Execute operation with error handling but no fallback.
     * Returns null on error for safe operations.
     */
    inline fun <T> safeOperation(operation: () -> T?): T? {
        return try {
            operation()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Safely get text from PSI element with null checks.
     */
    fun safeGetText(element: PsiElement?): String {
        return safeOperation({ element?.text }) ?: ""
    }

    /**
     * Safely get children from PSI element with error handling.
     */
    fun safeGetChildren(element: PsiElement?): Array<PsiElement> {
        return safeOperation({ element?.children }) ?: emptyArray()
    }

    /**
     * Safe array access with bounds checking.
     */
    fun <T> safeArrayAccess(array: Array<T>?, index: Int): T? {
        return safeOperation({
            if (array != null && index >= 0 && index < array.size) array[index] else null
        })
    }

    /**
     * Safe list access with bounds checking.
     */
    fun <T> safeListAccess(list: List<T>?, index: Int): T? {
        return safeOperation({
            if (list != null && index >= 0 && index < list.size) list[index] else null
        })
    }

    /**
     * Validate PSI element for completion context.
     */
    fun isValidCompletionContext(element: PsiElement?): Boolean {
        return safeOperation({
            element != null && element.isValid && element.containingFile != null
        }) ?: false
    }

    /**
     * Validate PSI element for general operations.
     */
    fun isValidPsiElement(element: PsiElement?): Boolean {
        return safeOperation({
            element != null && element.isValid
        }) ?: false
    }

    /**
     * Log performance metrics if enabled.
     */
    inline fun <T> withPerformanceLogging(operationName: String, operation: () -> T): T {
        return if (PhelConfiguration.ENABLE_PERFORMANCE_MONITORING) {
            val startTime = System.currentTimeMillis()
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            if (duration > 50) { // Log slow operations
                LOG.info("$operationName took ${duration}ms")
            }
            result
        } else {
            operation()
        }
    }

    /**
     * Safely convert any value to string representation.
     */
    fun safeToString(value: Any?): String {
        return safeOperation({
            value?.toString()
        }) ?: "null"
    }

    /**
     * Check if operation should be skipped due to error conditions.
     */
    fun shouldSkipOperation(element: PsiElement?, operationName: String): Boolean {
        if (!isValidPsiElement(element)) {
            if (PhelConfiguration.ENABLE_COMPLETION_LOGGING) {
                LOG.debug("Skipping $operationName due to invalid PSI element")
            }
            return true
        }
        return false
    }
}
