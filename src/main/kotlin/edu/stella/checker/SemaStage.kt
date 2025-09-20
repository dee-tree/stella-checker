package edu.stella.checker

import edu.stella.core.DiagnosticsEngine
import edu.stella.core.TypeManager
import edu.stella.type.Ty
import org.antlr.v4.kotlinruntime.tree.ParseTree

interface SemaStage<T> {
    val symbols: SymbolTable
    val types: TypeManager
    val diag: DiagnosticsEngine

    fun getTy(symbol: String, scope: ParseTree): Ty? = symbols.getOrNull(symbol, scope)?.let { types.getSynthesized(it) }
}