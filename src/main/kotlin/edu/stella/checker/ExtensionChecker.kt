package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

class ExtensionChecker(private val program: stellaParser.ProgramContext) {

    val isExceptionTypeDeclarationEnabled by lazy {
        Extensions.EXCEPTION_TYPE_DECLARATION in this
    }

    val isExceptionTypeVariantEnabled by lazy {
        Extensions.EXCEPTION_TYPE_VARIANT in this
    }

    val isPanicEnabled by lazy {
        Extensions.PANIC in this
    }

    val isStructuralSubtypingEnabled by lazy {
        Extensions.STRUCTURAL_SUBTYPING in this
    }

    val isAmbiguousTypeAsBottomEnabled by lazy {
        Extensions.AMBIGUOUS_TYPE_AS_BOTTOM in this
    }

    private operator fun contains(ext: Extensions): Boolean = program.extensions.any {
        it is stellaParser.AnExtensionContext && ext.ext in it.extensionNames.map { it.text!! }
    }


    enum class Extensions(val ext: String) {
        EXCEPTION_TYPE_DECLARATION("#exception-type-declaration"),
        EXCEPTION_TYPE_VARIANT("#open-variant-exceptions"),
        PANIC("#panic"),
        STRUCTURAL_SUBTYPING("#structural-subtyping"),
        AMBIGUOUS_TYPE_AS_BOTTOM("#ambiguous-type-as-bottom"),
    }
}