package edu.stella.core

import org.antlr.v4.kotlinruntime.TokenStream

abstract class Diag(val kind: DiagKind) {
    abstract fun toString(tokenStream: TokenStream): String

    public enum class DiagKind {
        ERROR
    }
}