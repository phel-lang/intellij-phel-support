package org.phellang.core.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.phellang.core.config.PhelConfiguration

/**
 * Performance optimization utilities for Phel plugin.
 * Centralizes performance-related decision making and monitoring.
 */
object PhelPerformanceUtils {
    @JvmStatic
    val LOG = Logger.getInstance(PhelPerformanceUtils::class.java)

    /**
     * Check if expensive operations should be skipped based on context.
     */
    fun shouldSkipExpensiveOperations(element: PsiElement): Boolean {
        return PhelErrorHandler.safeOperation({
            // Skip if file is too large
            val file = element.containingFile
            if (isFileTooLarge(file)) {
                return@safeOperation true
            }

            // Skip if PSI tree is too deep (malformed code)
            if (isPsiTreeTooDeep(element)) {
                return@safeOperation true
            }

            // Skip if we're in a performance-critical context
            if (isInPerformanceCriticalContext()) {
                return@safeOperation true
            }

            false
        }) ?: true // Skip on error to be safe
    }

    /**
     * Check if file is too large for complex operations.
     */
    fun isFileTooLarge(file: PsiFile?): Boolean {
        if (file == null) return false
        
        val virtualFile = file.virtualFile ?: return false
        val fileSizeKB = virtualFile.length / 1024
        
        return fileSizeKB > PhelConfiguration.MAX_FILE_SIZE_KB
    }

    /**
     * Check if PSI tree is too deep (indicates malformed code or infinite recursion risk).
     */
    fun isPsiTreeTooDeep(element: PsiElement): Boolean {
        var current: PsiElement? = element
        var depth = 0
        
        while (current != null && depth < PhelConfiguration.MAX_PSI_TREE_DEPTH) {
            current = current.parent
            depth++
        }
        
        return depth >= PhelConfiguration.MAX_PSI_TREE_DEPTH
    }

    /**
     * Check if we're in a performance-critical context (e.g., during indexing).
     */
    fun isInPerformanceCriticalContext(): Boolean {
        // This could be expanded to check IDE state
        return false
    }

    /**
     * Limit collection operations for performance.
     */
    fun <T> limitCollection(
        collection: Collection<T>, 
        maxSize: Int = PhelConfiguration.MAX_COMPLETION_RESULTS
    ): List<T> {
        return if (collection.size > maxSize) {
            collection.take(maxSize)
        } else {
            collection.toList()
        }
    }

    /**
     * Limit collection operations with custom predicate.
     */
    fun <T> limitCollectionWith(
        collection: Collection<T>,
        maxSize: Int = PhelConfiguration.MAX_COMPLETION_RESULTS,
        predicate: (T) -> Boolean
    ): List<T> {
        val filtered = collection.filter(predicate)
        return limitCollection(filtered, maxSize)
    }

    /**
     * Check if operation should timeout.
     */
    fun isTimeoutExceeded(startTime: Long): Boolean {
        return System.currentTimeMillis() - startTime > PhelConfiguration.COMPLETION_TIMEOUT_MS
    }

    /**
     * Throttle operations to prevent UI freezing.
     */
    fun throttleOperation(@Suppress("UNUSED_PARAMETER") operationName: String) {
        if (PhelConfiguration.ENABLE_PERFORMANCE_MONITORING) {
            // Could implement actual throttling logic here
            Thread.yield()
        }
    }

    /**
     * Execute operation with timeout protection.
     */
    inline fun <T> withTimeout(
        timeoutMs: Long = PhelConfiguration.COMPLETION_TIMEOUT_MS,
        operation: () -> T
    ): T? {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = operation()
            if (isTimeoutExceeded(startTime)) {
                LOG.warn("Operation exceeded timeout of ${timeoutMs}ms")
                null
            } else {
                result
            }
        } catch (e: Exception) {
            LOG.warn("Operation failed within timeout", e)
            null
        }
    }

    /**
     * Measure and log operation performance.
     */
    inline fun <T> measurePerformance(operationName: String, operation: () -> T): T {
        return if (PhelConfiguration.ENABLE_PERFORMANCE_MONITORING) {
            val startTime = System.currentTimeMillis()
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            when {
                duration > 500 -> LOG.warn("Slow operation $operationName: ${duration}ms")
                duration > 100 -> LOG.info("Operation $operationName: ${duration}ms")
                else -> LOG.debug("Fast operation $operationName: ${duration}ms")
            }
            
            result
        } else {
            operation()
        }
    }

    /**
     * Check if performance optimization is enabled.
     */
    fun isPerformanceOptimizationEnabled(): Boolean {
        return PhelConfiguration.ENABLE_PERFORMANCE_OPTIMIZATION
    }

    /**
     * Get optimal batch size for operations based on current performance settings.
     */
    fun getOptimalBatchSize(defaultSize: Int = 10): Int {
        return if (isPerformanceOptimizationEnabled()) {
            (defaultSize * 0.7).toInt().coerceAtLeast(5)
        } else {
            defaultSize
        }
    }
}
