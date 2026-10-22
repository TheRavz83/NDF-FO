package fr.notedefrais.app.export

import fr.notedefrais.app.data.ExpenseType
import fr.notedefrais.app.data.Receipt
import fr.notedefrais.app.data.Trip
import fr.notedefrais.app.data.MealZone
import fr.notedefrais.app.data.combineSeparateMealReceipts
import fr.notedefrais.app.data.isMealAllowance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.util.zip.ZipFile

class TripEmailExporterTest {
    @Test
    fun receiptsAreSortedByDayThenAtosCode() {
        val receipts = listOf(
            receipt("taxi", LocalDate.of(2026, 7, 23), ExpenseType.TAXI),
            receipt("lunch", LocalDate.of(2026, 7, 22), ExpenseType.LUNCH),
            receipt(
                "transport",
                LocalDate.of(2026, 7, 23),
                ExpenseType.TRANSPORT_OCCASIONAL
            )
        )

        val sorted = sortReceiptsForExport(receipts)

        assertEquals(listOf("lunch", "transport", "taxi"), sorted.map { it.id })
    }

    @Test
    fun lunchIsAlwaysExportedBeforeDinnerOnTheSameDay() {
        val date = LocalDate.of(2026, 7, 22)
        val sorted = sortReceiptsForExport(
            listOf(
                receipt("dinner", date, ExpenseType.DINNER_PARIS),
                receipt("lunch", date, ExpenseType.LUNCH)
            )
        )

        assertEquals(listOf("lunch", "dinner"), sorted.map { it.id })
    }

    @Test
    fun attachmentsUseTheirEntryPositionAndAtosDescription() {
        val date = LocalDate.of(2026, 7, 22)
        val receipts = listOf(
            receipt(
                "first",
                date,
                ExpenseType.LUNCH,
                comment = "Déjeuner avec le client"
            ),
            receipt(
                "second",
                date,
                ExpenseType.DINNER_PARIS,
                comment = "Dîner équipe"
            )
        )

        val names = buildAttachmentNames(receipts)

        assertEquals("01 Déjeuner avec le client.pdf", names.getValue("first"))
        assertEquals("02 Dîner équipe.pdf", names.getValue("second"))
    }

    @Test
    fun existingPdfKeepsPdfExtensionInExport() {
        val date = LocalDate.of(2026, 7, 22)
        val pdfReceipt = receipt(
            id = "invoice",
            date = date,
            type = ExpenseType.LUNCH,
            mimeType = "application/pdf",
            storedFileName = "invoice.pdf"
        )

        val names = buildAttachmentNames(listOf(pdfReceipt))

        assertEquals("01 06 Lunch — 2026-07-22.pdf", names.getValue("invoice"))
    }

    @Test
    fun csvSeparatesDaysAndIncludesExplicitAttachmentNames() {
        val trip = Trip(
            name = "Les Clayes",
            startDate = LocalDate.of(2026, 7, 22),
            endDate = LocalDate.of(2026, 7, 23)
        )
        val receipts = sortReceiptsForExport(
            listOf(
                receipt("taxi", LocalDate.of(2026, 7, 23), ExpenseType.TAXI),
                receipt("lunch", LocalDate.of(2026, 7, 22), ExpenseType.LUNCH)
            )
        )
        val names = buildAttachmentNames(receipts)
        val lines = buildExportLines(receipts, names)

        val csv = buildCsv(trip, lines)

        assertTrue(
            csv.contains(
                "Date;Code;Libellé;Nombre d'items;Montant TTC;Montant remboursable;" +
                    "Commentaire / Description;Pièce jointe"
            )
        )
        assertTrue(csv.contains("01 06 Lunch — 2026-07-22.pdf"))
        assertTrue(Regex("2026-07-22.*\\R\\R2026-07-23", RegexOption.DOT_MATCHES_ALL).containsMatchIn(csv))
    }

    @Test
    fun hotelUsesInclusiveTripDayCountAsItemCount() {
        val trip = Trip(
            name = "Paris",
            startDate = LocalDate.of(2026, 7, 21),
            endDate = LocalDate.of(2026, 7, 23)
        )
        val hotel = receipt(
            id = "hotel",
            date = trip.endDate,
            type = ExpenseType.HOTEL_PARIS_SOPHIA,
            amount = BigDecimal("450.00")
        )

        val line = buildExportLines(
            receipts = listOf(hotel),
            attachmentNames = buildAttachmentNames(listOf(hotel)),
            hotelItemCount = trip.inclusiveDayCount()
        ).single()

        assertEquals(3, line.itemCount)
        assertTrue(buildCsv(trip, listOf(line)).contains(";3;450,00;"))
    }

    @Test
    fun commentIsExportedAsAtosDescription() {
        val date = LocalDate.of(2026, 7, 21)
        val receipt = receipt(
            id = "taxi",
            date = date,
            type = ExpenseType.TAXI,
            comment = "Trajet gare vers le client"
        )
        val lines = buildExportLines(
            listOf(receipt),
            buildAttachmentNames(listOf(receipt))
        )
        val trip = Trip(name = "Les Clayes", startDate = date, endDate = date)

        assertEquals("Trajet gare vers le client", lines.single().description)
        assertTrue(buildEmailBody(trip, lines).contains("Commentaire : Trajet gare vers le client"))
        assertTrue(buildCsv(trip, lines).contains("Trajet gare vers le client"))
    }

    @Test
    fun emptyCommentFallsBackToExpenseLabelAndDate() {
        val date = LocalDate.of(2026, 7, 21)
        val receipt = receipt("taxi", date, ExpenseType.TAXI)

        val line = buildExportLines(
            listOf(receipt),
            buildAttachmentNames(listOf(receipt))
        ).single()

        assertEquals("02 Taxi — 2026-07-21", line.description)
    }

    @Test
    fun generatedMealAllowanceIsExportedWithoutAttachment() {
        val date = LocalDate.of(2026, 7, 21)
        val lunch = receipt("lunch", date, ExpenseType.LUNCH)
        val allowance = Receipt(
            id = "allowance",
            tripId = "trip",
            date = date,
            amount = BigDecimal("25.00"),
            category = ExpenseType.MEAL_ALLOWANCE_PARIS.category,
            expenseType = ExpenseType.MEAL_ALLOWANCE_PARIS,
            storedFileName = "",
            mimeType = "",
            comment = "Meal allowance automatique — Dinner manquant",
            generatedMealAllowance = true
        )
        val receipts = listOf(lunch, allowance)
        val names = buildAttachmentNames(receipts)

        val lines = buildExportLines(receipts, names)

        assertEquals(2, lines.size)
        assertEquals(1, names.size)
        assertEquals(emptyList<String>(), lines.single { it.expenseType.isMealAllowance }.attachmentNames)
        assertEquals(
            "01 06 Lunch — 2026-07-21.pdf",
            names.getValue("lunch")
        )
    }

    @Test
    fun manualExpenseWithoutAttachmentIsStillExported() {
        val date = LocalDate.of(2026, 7, 21)
        val manualExpense = Receipt(
            id = "manual",
            tripId = "trip",
            date = date,
            amount = BigDecimal("12.50"),
            category = ExpenseType.TAXI.category,
            expenseType = ExpenseType.TAXI,
            storedFileName = "",
            mimeType = "",
            comment = "Taxi sans justificatif"
        )

        val names = buildAttachmentNames(listOf(manualExpense))
        val lines = buildExportLines(listOf(manualExpense), names)

        assertEquals(emptyMap<String, String>(), names)
        assertEquals(1, lines.size)
        assertEquals("Taxi sans justificatif", lines.single().description)
        assertEquals(emptyList<String>(), lines.single().attachmentNames)
        assertEquals(false, manualExpense.hasAttachment)
        assertEquals(false, manualExpense.generatedMealAllowance)
    }

    @Test
    fun missingLunchAllowanceComesBeforeDinnerAndCountsInAttachmentPosition() {
        val date = LocalDate.of(2026, 7, 21)
        val dinner = receipt("dinner", date, ExpenseType.DINNER_PARIS)
        val allowance = Receipt(
            id = "allowance",
            tripId = "trip",
            date = date,
            amount = BigDecimal("25.00"),
            category = ExpenseType.MEAL_ALLOWANCE_PARIS.category,
            expenseType = ExpenseType.MEAL_ALLOWANCE_PARIS,
            storedFileName = "",
            mimeType = "",
            comment = "Meal allowance automatique — Lunch manquant",
            generatedMealAllowance = true
        )
        val sorted = sortReceiptsForExport(listOf(dinner, allowance))

        val names = buildAttachmentNames(sorted)

        assertEquals(listOf("allowance", "dinner"), sorted.map { it.id })
        assertEquals(
            "02 06 Dinner Paris — 2026-07-21.pdf",
            names.getValue("dinner")
        )
    }

    @Test
    fun twoCumulatedMealsOnTheSameDayBecomeOneExportLine() {
        val date = LocalDate.of(2026, 7, 22)
        val receipts = listOf(
            receipt(
                "combined-first",
                date,
                ExpenseType.LUNCH_DINNER_PARIS,
                amount = BigDecimal("18.50")
            ),
            receipt(
                "combined-second",
                date,
                ExpenseType.LUNCH_DINNER_PARIS,
                amount = BigDecimal("21.50")
            )
        )
        val names = buildAttachmentNames(receipts)

        val lines = buildExportLines(receipts, names)

        assertEquals(1, lines.size)
        assertEquals(BigDecimal("40.00"), lines.single().amount)
        assertEquals(
            listOf(
                "01 06 Lnch+Dnnr Paris (cumulated) — 2026-07-22.pdf",
                "01 06 Lnch+Dnnr Paris (cumulated) — 2026-07-22 (2).pdf"
            ),
            lines.single().attachmentNames
        )
        val trip = Trip(
            name = "Paris",
            startDate = date,
            endDate = date
        )
        val csv = buildCsv(trip, lines)
        assertEquals(1, csv.lineSequence().count { it.contains("Lnch+Dnnr Paris") })
        assertTrue(csv.contains("40,00"))
        assertTrue(csv.contains(lines.single().attachmentNames.joinToString(" | ")))
    }

    @Test
    fun acceptedLunchDinnerCombinationExportsOneLineWithBothReceipts() {
        val date = LocalDate.of(2026, 9, 18)
        val combinedReceipts = combineSeparateMealReceipts(
            receipts = listOf(
                receipt(
                    "dinner",
                    date,
                    ExpenseType.DINNER_PARIS,
                    comment = "Dîner équipe"
                ),
                receipt(
                    "lunch",
                    date,
                    ExpenseType.LUNCH,
                    comment = "Déjeuner client"
                )
            ),
            tripId = "trip",
            date = date,
            zone = MealZone.PARIS_SOPHIA
        )

        val names = buildAttachmentNames(combinedReceipts)
        val lines = buildExportLines(combinedReceipts, names)

        assertEquals(1, lines.size)
        assertEquals(BigDecimal("25.00"), lines.single().amount)
        assertEquals(2, lines.single().attachmentNames.size)
        assertTrue(lines.single().attachmentNames.all { it.startsWith("01 ") })
    }

    @Test
    fun cumulatedMealsCombineDistinctComments() {
        val date = LocalDate.of(2026, 7, 22)
        val receipts = listOf(
            receipt(
                "first",
                date,
                ExpenseType.LUNCH_DINNER_PARIS,
                comment = "Déjeuner client"
            ),
            receipt(
                "second",
                date,
                ExpenseType.LUNCH_DINNER_PARIS,
                comment = "Dîner équipe"
            )
        )

        val line = buildExportLines(receipts, buildAttachmentNames(receipts)).single()

        assertEquals("Déjeuner client / Dîner équipe", line.description)
    }

    @Test
    fun archiveNameContainsTripNameAndExportDate() {
        assertEquals(
            "Deplacement_Paris_2026-07-27.zip",
            buildArchiveName("Déplacement Paris", LocalDate.of(2026, 7, 27))
        )
    }

    @Test
    fun zipContainsCsvAndAllRenamedReceipts() {
        val directory = Files.createTempDirectory("fo-notes-export").toFile()
        try {
            val csv = File(directory, "recapitulatif.csv").apply { writeText("Date;Montant") }
            val receipt = File(directory, "06_Lunch_2026-07-22.jpg").apply {
                writeBytes(byteArrayOf(1, 2, 3))
            }
            val archive = File(directory, "Les_Clayes_2026-07-27.zip")

            writeZipArchive(archive, listOf(csv, receipt))

            ZipFile(archive).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toList()
                assertEquals(
                    listOf("recapitulatif.csv", "06_Lunch_2026-07-22.jpg"),
                    names
                )
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun receipt(
        id: String,
        date: LocalDate,
        type: ExpenseType,
        amount: BigDecimal = BigDecimal("12.50"),
        mimeType: String = "image/jpeg",
        storedFileName: String = "$id.jpg",
        comment: String = ""
    ) = Receipt(
        id = id,
        tripId = "trip",
        date = date,
        amount = amount,
        category = type.category,
        expenseType = type,
        storedFileName = storedFileName,
        mimeType = mimeType,
        comment = comment
    )
}
