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
        is stellaParser.TypeRecordContext -> RecordTy(this.fieldTypes.map { field -> field.label!!.text.toString() to field.stellatype().asTy }, this)
        is stellaParser.TypeVariantContext -> VariantTy(this.fieldTypes.map { it.label!!.text!! to it.stellatype()?.asTy })
        is stellaParser.TypeSumContext -> SumTy(this.left!!.asTy, this.right!!.asTy, this)
        is stellaParser.TypeRefContext -> RefTy(this.stellatype().asTy)
        is stellaParser.TypeTopContext -> TopTy()
        is stellaParser.TypeBottomContext -> BotTy()
        else -> TODO("Type mapping rule is required for type ${this.text.quote()} (${this::class.simpleName?.quote()})")
    }