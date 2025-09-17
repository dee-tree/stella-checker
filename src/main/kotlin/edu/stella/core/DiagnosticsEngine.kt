package edu.stella.core

import org.antlr.v4.kotlinruntime.TokenStream

class DiagnosticsEngine(private val tokenStream: TokenStream) {
    var hasError: Boolean = false
        private set

    fun diag(diagnostic: Diag) {
        if (hasError) return
        if (diagnostic.kind == Diag.DiagKind.ERROR) hasError = true
        System.err.println(diagnostic.toString(tokenStream))
    }
}