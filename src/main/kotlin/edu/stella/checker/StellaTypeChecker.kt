package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.TypeManager
import edu.stella.type.BadTy
import edu.stella.type.BoolTy
import edu.stella.type.FunTy
import edu.stella.type.NatTy
import edu.stella.type.asTy

class StellaTypeChecker(
    private val program: stellaParser.ProgramContext,
    override val symbols: SymbolTable,
    override val types: TypeManager,
    override val diag: DiagnosticsEngine
) : SemaASTVisitor<Unit>() {

    fun check() {
        StellaTypeSynthesizer(this).synthesize(program)
        program.accept(this)
    }

    override fun defaultResult() = Unit

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        ctx.accept(MissingMainChecker(diag))
        ctx.accept(UndefinedVariableChecker(this))
        ctx.accept(NotFunctionApplicationChecker(this))
        ctx.accept(NotATupleChecker(this))
        ctx.accept(NotARecordChecker(this))
        ctx.accept(NotAListChecker(this))

        super.visitProgram(ctx)
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        types.expect(ctx.returnExpr!!, ctx.returnType!!.asTy)
        super.visitDeclFun(ctx)
    }

    override fun visitIf(ctx: stellaParser.IfContext) {
        types.expect(ctx.condition!!, BoolTy())
        super.visitIf(ctx)
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext) {
        types.check(ctx, deep = false) {
            DiagUnexpectedLambda(ctx, types.getExpectation(ctx))
        }

        (types.getExpectation(ctx) as? FunTy)?.let { expected ->
            expected.params.zip(ctx.paramDecls).forEach { (expTy, param) ->
                types.expect(param, expTy)
                types.check(param) { DiagUnexpectedParameterType(param, expTy) }
            }

            types.expect(ctx.returnExpr!!, expected.ret)
        }

        super.visitAbstraction(ctx)
    }

    override fun visitNatRec(ctx: stellaParser.NatRecContext) {
        types.expect(ctx.n!!, NatTy())
        types.check(ctx.n!!)
        val element = types[ctx.initial!!] ?: BadTy()
        types.expect(ctx.step!!, FunTy(FunTy(element, listOf(element)), listOf(NatTy())))

        super.visitNatRec(ctx)
    }

}