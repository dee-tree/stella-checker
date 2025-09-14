package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.StellaCompileException
import edu.stella.core.quote
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTree

class SymbolTable(val program: stellaParser.ProgramContext) {
    private var scope = SymbolScope.new(program)
    private val nodeToScope = arrayListOf<Pair<ParseTree, SymbolScope>>()

    fun push(scope: RuleContext) {
        this.scope = this.scope.new(scope)
        nodeToScope += scope to this.scope
    }

    fun pop() {
        scope = scope.parent ?: throw StellaCompileException("Can't pop from global scope of symbol table")
    }

    operator fun contains(symbol: String): Boolean = symbol in scope

    fun contains(symbol: String, ctx: ParseTree): Boolean {
        for ((node, scope) in nodeToScope) {
            if (node.sourceInterval == ctx.sourceInterval) return symbol in scope
        }
        return ctx.getParent()?.let { contains(symbol, it) } ?: false
    }

    fun getOrNull(symbol: String, ctx: ParseTree): ParserRuleContext? {
        for ((node, scope) in nodeToScope) {
            if (node.sourceInterval == ctx.sourceInterval) return scope.getOrNull(symbol)
        }
        return ctx.getParent()?.let { getOrNull(symbol, it) }
    }

    fun get(symbol: String, ctx: ParseTree): ParserRuleContext = getOrNull(symbol, ctx)
        ?: throw StellaCompileException("Symbol ${symbol.quote()} does not present in ctx")

    fun add(decl: stellaParser.DeclFunContext) = scope.add(decl)
    fun add(decl: stellaParser.DeclFunGenericContext) = scope.add(decl)
    fun add(decl: stellaParser.ParamDeclContext) = scope.add(decl)
    fun add(decl: stellaParser.BindingContext) = scope.add(decl)
    fun add(decl: stellaParser.LetContext) = scope.add(decl)
    fun add(decl: stellaParser.PatternVarContext) = scope.add(decl)
}

private class SymbolScope private constructor(
    val parent: SymbolScope? = null,
    val name: String? = null,
    private val level: UInt = 0u,
    val ctx: ParseTree
) {
    private val children = arrayListOf<SymbolScope>()
    private val symbols = mutableMapOf<String, ParserRuleContext>()

    fun new(scope: ParseTree) = SymbolScope(
        parent = this,
        name = "scope(${level + 1u}-${children.size})",
        level = level + 1u,
        ctx = scope
    ).also { children += it }

    private fun add(symbol: String, decl: ParserRuleContext) {
        if (symbol in symbols)
            throw StellaCompileException("Symbol ${symbol.quote()} already presents in symbol table")

        symbols[symbol] = decl
    }

    fun add(decl: stellaParser.DeclFunContext) = add(decl.name!!.text!!, decl)
    fun add(decl: stellaParser.DeclFunGenericContext) = add(decl.name!!.text!!, decl)
    fun add(decl: stellaParser.ParamDeclContext) = add(decl.name!!.text!!, decl)
    fun add(decl: stellaParser.BindingContext) = add(decl.name!!.text!!, decl)
    fun add(decl: stellaParser.LetContext) = decl.patternBindings.forEach { binding ->
        when (val pat = binding.pattern()) {
            is stellaParser.PatternVarContext -> add(pat.name!!.text!!, pat)
        }
    }
    fun add(decl: stellaParser.PatternVarContext) = add(decl.name!!.text!!, decl)


    val isGlobal: Boolean
        get() = parent == null

    override fun toString(): String = name ?: "scope(${level})"

    operator fun contains(symbol: String): Boolean {
        if (symbol in symbols) return true
        return parent?.contains(symbol) ?: false
    }

    fun getOrNull(symbol: String): ParserRuleContext? {
        return symbols[symbol] ?: parent?.getOrNull(symbol)
    }

    fun get(symbol: String): ParserRuleContext = getOrNull(symbol)
        ?: throw StellaCompileException("Symbol ${symbol.quote()} is not in the scope $this")

    companion object {
        fun new(scope: stellaParser.ProgramContext) = SymbolScope(name = "global", ctx = scope)
    }
}