package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor

class SymbolCollector() : stellaParserBaseVisitor<SymbolTable>() {
    private lateinit var table: SymbolTable

    override fun defaultResult(): SymbolTable = table

    override fun visitProgram(ctx: stellaParser.ProgramContext): SymbolTable {
        table = SymbolTable(ctx)
        return super.visitProgram(ctx)
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext): SymbolTable {
        table.add(ctx)
        table.push(ctx)
        return super.visitDeclFun(ctx).also { table.pop() }
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext): SymbolTable {
        table.push(ctx)
        return super.visitAbstraction(ctx).also { table.pop() }
    }

    override fun visitDeclFunGeneric(ctx: stellaParser.DeclFunGenericContext): SymbolTable {
        table.add(ctx)
        table.push(ctx)
        return super.visitDeclFunGeneric(ctx)
    }

    override fun visitParamDecl(ctx: stellaParser.ParamDeclContext): SymbolTable {
        table.add(ctx)
        return super.visitParamDecl(ctx)
    }

    override fun visitMatchCase(ctx: stellaParser.MatchCaseContext): SymbolTable {
        table.push(ctx)
        return super.visitMatchCase(ctx).also { table.pop() }
    }

    override fun visitPatternVar(ctx: stellaParser.PatternVarContext): SymbolTable {
        table.add(ctx)
        return super.visitPatternVar(ctx)
    }

//    override fun visitBinding(ctx: stellaParser.BindingContext): SymbolTable {
//        table.add(ctx)
//        table.push(ctx)
//        return super.visitBinding(ctx).also { table.pop() }
//    }

    override fun visitLet(ctx: stellaParser.LetContext): SymbolTable {
        // manual walk instead of super call
        ctx.patternBindings.forEach {
            it.expr().accept(this)
        }

        table.push(ctx.expr())
        ctx.patternBindings.forEach {
            it.pattern().accept(this)
        }

        return ctx.expr().accept(this).also { table.pop() }
    }
}