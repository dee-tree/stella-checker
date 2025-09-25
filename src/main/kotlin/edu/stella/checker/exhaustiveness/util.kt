package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.BoolTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.SumTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy
import edu.stella.type.VariantTy

private val Ty.finite: Boolean get() = when (this) {
    is UnitTy -> true
    is BoolTy -> true
    is NatTy -> true
    is TupleTy -> components.all(Ty::finite)
    is RecordTy -> components.unzip().second.all(Ty::finite)

    else -> TODO("am I over a finite domain?")
}

internal fun <T : Ty> T.patternChecker(): ExhaustivenessSolver<T> = when (this) {
    is BoolTy -> BoolExhaustivenessSolver()
    is NatTy -> NatExhaustivenessSolver()
    is SumTy -> SumExhaustivenessSolver(this)
    is VariantTy -> VariantExhaustivenessSolver(this)
    is ListTy -> ListExhaustivenessSolver(this)
    is TupleTy -> TupleExhaustivenessSolver(this)
    is UnitTy -> UnitExhaustivenessSolver()
    else -> WildcardExhaustivenessSolver<T>(this)
} as ExhaustivenessSolver<T>

fun stellaParser.PatternContext.isCorrect(ty: Ty): Boolean = ty.patternChecker().isValidPattern(this)