package edu.stella.type

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.quote

val stellaParser.StellatypeContext.asTy: Ty
    get() = when(this) {
        is stellaParser.TypeBoolContext -> BoolTy(this)
        is stellaParser.TypeNatContext -> NatTy(this)
        is stellaParser.TypeFunContext -> FunTy(returnType!!.asTy, paramTypes.map(stellaParser.StellatypeContext::asTy), this)
        is stellaParser.TypeUnitContext -> UnitTy(this)
        is stellaParser.TypeListContext -> ListTy(stellatype().asTy, this)
        is stellaParser.TypeParensContext -> stellatype().asTy
        is stellaParser.TypeTupleContext -> TupleTy(types.map(stellaParser.StellatypeContext::asTy), this)
        is stellaParser.TypeRecordContext -> RecordTy(this.fieldTypes.map { field -> field.label!!.toString() to field.stellatype().asTy }, this)
        else -> TODO("Type mapping rule is required for type ${this.text.quote()} (${this::class.simpleName?.quote()})")
    }