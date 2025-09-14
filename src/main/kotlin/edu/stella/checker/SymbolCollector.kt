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
        table.push(ctx)
        table.add(ctx)
        return super.visitLet(ctx).also { table.pop() }
    }
}