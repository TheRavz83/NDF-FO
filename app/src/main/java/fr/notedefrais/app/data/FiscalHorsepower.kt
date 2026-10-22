package fr.notedefrais.app.data

import java.math.BigDecimal
import java.math.RoundingMode

/** Barème kilométrique 2026 des voitures, tranche « distance d jusqu'à 5 000 km » (service-public.gouv.fr, A14686). */
enum class FiscalHorsepower(val label: String, val ratePerKm: BigDecimal) {
    THREE_OR_LESS("3 CV et moins", BigDecimal("0.529")),
    FOUR("4 CV", BigDecimal("0.606")),
    FIVE("5 CV", BigDecimal("0.636")),
    SIX("6 CV", BigDecimal("0.665")),
    SEVEN("7 CV", BigDecimal("0.697"));

    fun amountFor(kilometers: BigDecimal): BigDecimal =
        (kilometers * ratePerKm).setScale(2, RoundingMode.HALF_UP)
}
