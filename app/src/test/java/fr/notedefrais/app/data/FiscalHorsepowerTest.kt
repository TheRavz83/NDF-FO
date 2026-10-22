package fr.notedefrais.app.data

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class FiscalHorsepowerTest {
    @Test
    fun appliesRatesFromTheTwentyTwentySixScale() {
        val km = BigDecimal("25.0")
        assertEquals(BigDecimal("13.23"), FiscalHorsepower.THREE_OR_LESS.amountFor(km))
        assertEquals(BigDecimal("15.15"), FiscalHorsepower.FOUR.amountFor(km))
        assertEquals(BigDecimal("15.90"), FiscalHorsepower.FIVE.amountFor(km))
        assertEquals(BigDecimal("16.63"), FiscalHorsepower.SIX.amountFor(km))
        assertEquals(BigDecimal("17.43"), FiscalHorsepower.SEVEN.amountFor(km))
    }
}
