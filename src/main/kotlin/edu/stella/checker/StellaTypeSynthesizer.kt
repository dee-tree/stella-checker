package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.BadTy
import edu.stella.type.BoolTy
import edu.stella.type.FunTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy
import edu.stella.type.asTy

class StellaTypeSynthesizer(
    private val delegate: SemaStage<Unit>
) :  SemaASTVisitor<Unit>(),  SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    fun synthesize(program: stellaParser.ProgramContext) {
        program.accept(this)
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        types.learn(ctx, FunTy(ctx.returnType!!.asTy, ctx.paramDecls.map { param -> param.paramType!!.asTy }))
        super.visitDeclFun(ctx)
    }

    override fun visitParamDecl(ctx: stellaParser.ParamDeclContext) {
        types.learn(ctx, ctx.paramType!!.asTy)
        super.visitParamDecl(ctx)
    }

    override fun visitVar(ctx: stellaParser.VarContext) {
        super.visitVar(ctx)
        types.learn(ctx, getTy(ctx.name!!.text!!, ctx) ?: BadTy(ctx))
    }

    override fun visitConstFalse(ctx: stellaParser.ConstFalseContext) {
        types.learn(ctx, BoolTy())
        super.visitConstFalse(ctx)
    }

    override fun visitConstTrue(ctx: stellaParser.ConstTrueContext) {
        types.learn(ctx, BoolTy())
        super.visitConstTrue(ctx)
    }

    override fun visitConstInt(ctx: stellaParser.ConstIntContext) {
        types.learn(ctx, NatTy())
        super.visitConstInt(ctx)
    }

    override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext) {
        super.visitParenthesisedExpr(ctx)
        types.learn(ctx, types[ctx.expr()] ?: BadTy(ctx.expr()))
    }

    override fun visitTerminatingSemicolon(ctx: stellaParser.TerminatingSemicolonContext) {
        super.visitTerminatingSemicolon(ctx)
        types.learn(ctx, types[ctx.expr()] ?: BadTy(ctx.expr()))
    }

    override fun visitIsZero(ctx: stellaParser.IsZeroContext) {
        super.visitIsZero(ctx)
        types.learn(ctx, BoolTy())
    }

    override fun visitSucc(ctx: stellaParser.SuccContext) {
        super.visitSucc(ctx)
        types.learn(ctx, NatTy())
    }

    override fun visitPred(ctx: stellaParser.PredContext) {
        super.visitPred(ctx)
        types.learn(ctx, NatTy())
    }

    override fun visitApplication(ctx: stellaParser.ApplicationContext) {
        super.visitApplication(ctx)

        val funcTy = types[ctx.func!!] as? FunTy
        types.learn(ctx, funcTy?.ret ?: BadTy(ctx))
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext) {
        val paramTys = mutableListOf<Ty>()
        ctx.paramDecls.forEach { param ->
            val ty = param.paramType!!.asTy
            paramTys += ty
            types.learn(param, ty)
        }

        ctx.returnExpr!!.accept(this)

        val retTy = types[ctx.returnExpr!!] ?: BadTy(ctx.returnExpr)

        types.learn(ctx, FunTy(retTy, paramTys))
    }

    override fun visitConstUnit(ctx: stellaParser.ConstUnitContext) {
        types.learn(ctx, UnitTy())
        super.visitConstUnit(ctx)
    }

    override fun visitTuple(ctx: stellaParser.TupleContext) {
        super.visitTuple(ctx)

        val tys = mutableListOf<Ty>()
        ctx.exprs.forEach { component ->
            val ty = types[component]!!
            tys += ty
        }
        types.learn(ctx, TupleTy(tys))
    }

    override fun visitRecord(ctx: stellaParser.RecordContext) {
        super.visitRecord(ctx)

        val tys = mutableListOf<Pair<String, Ty>>()
        ctx.bindings.forEach { binding ->
            val ty = types[binding.expr()]!!
            tys += binding.name!!.text!! to ty
        }

        types.learn(ctx, RecordTy(tys))
    }

    override fun visitPatternVar(ctx: stellaParser.PatternVarContext) {
        super.visitPatternVar(ctx)
        types.learn(ctx, getTy(ctx.name!!.text!!, ctx) ?: BadTy())
    }

    override fun visitTypeAsc(ctx: stellaParser.TypeAscContext) {
        types.learn(ctx, ctx.stellatype().asTy)
        super.visitTypeAsc(ctx)
    }

    override fun visitLet(ctx: stellaParser.LetContext) {
        ctx.patternBindings.forEach { binding ->
            binding.accept(this)
        }

        ctx.expr().accept(this)

        // Do not call super.visitLet() since it is visited in manual order
    }


}