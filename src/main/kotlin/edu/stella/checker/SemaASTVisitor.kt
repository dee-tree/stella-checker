package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.TypeManager

abstract class SemaASTVisitor<T> : stellaParserBaseVisitor<T>(), SemaStage<T> {
    abstract override val symbols: SymbolTable
    abstract override val types: TypeManager
    abstract override val extensions: ExtensionChecker
    abstract override val diag: DiagnosticsEngine
}