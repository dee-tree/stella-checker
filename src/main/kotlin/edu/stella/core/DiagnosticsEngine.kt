package edu.stella.core

import edu.stella.checker.TypeCheckError
import edu.stella.checker.TypeCheckErrorKind
import org.antlr.v4.kotlinruntime.TokenStream

class DiagnosticsEngine(private val tokenStream: TokenStream) {
    private val diagnostics = mutableListOf<Diag>()

    var hasError: Boolean = false
        private set

    internal operator fun contains(err: TypeCheckErrorKind): Boolean = diagnostics.any {
        it is TypeCheckError && it.error == err
    }

    fun diag(diagnostic: Diag) {
        if (hasError) return
        if (diagnostic.kind == Diag.DiagKind.ERROR) hasError = true
        diagnostics += diagnostic
        System.err.println(diagnostic.toString(tokenStream))
    }
}