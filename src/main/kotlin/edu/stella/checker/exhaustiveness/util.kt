package edu.stella.checker.exhaustiveness

import edu.stella.type.BoolTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy

private val Ty.finite: Boolean get() = when (this) {
    is UnitTy -> true
    is BoolTy -> true
    is NatTy -> true
    is TupleTy -> components.all(Ty::finite)
    is RecordTy -> components.unzip().second.all(Ty::finite)

    else -> TODO("am I over a finite domain?")
}