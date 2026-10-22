package fr.notedefrais.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseTypeTest {
    @Test
    fun everyCategoryHasAtLeastOneType() {
        ExpenseCategory.entries.forEach { category ->
            assertTrue(
                "${category.name} doit proposer au moins un type",
                ExpenseType.forCategory(category).isNotEmpty()
            )
        }
    }

    @Test
    fun typesAreGroupedUnderTheirOwnCategory() {
        ExpenseCategory.entries.forEach { category ->
            ExpenseType.forCategory(category).forEach { type ->
                assertEquals(category, type.category)
            }
        }
    }

    @Test
    fun atosCatalogContainsAllVisibleExpenseTypes() {
        assertEquals(47, ExpenseType.entries.size)
        assertEquals(46, ExpenseType.entries.count { it != ExpenseType.OTHER })
        assertEquals("01 Transports (occasional)", ExpenseType.TRANSPORT_OCCASIONAL.displayLabel)
        assertEquals("11 Move accompanying measures", ExpenseType.MOVE_ACCOMPANYING_MEASURES.displayLabel)
    }

    @Test
    fun policyDefaultsMatchMealAndHotelRules() {
        assertEquals(BigDecimal("20.00"), ExpenseType.LUNCH.defaultLimit)
        assertEquals(BigDecimal("20.00"), ExpenseType.DINNER_COUNTRY.defaultLimit)
        assertEquals(BigDecimal("25.00"), ExpenseType.DINNER_PARIS.defaultLimit)
        assertEquals(BigDecimal("40.00"), ExpenseType.LUNCH_DINNER_COUNTRY.defaultLimit)
        assertEquals(BigDecimal("45.00"), ExpenseType.LUNCH_DINNER_PARIS.defaultLimit)
        assertEquals(BigDecimal("130.00"), ExpenseType.HOTEL_EXCEPT_PARIS_SOPHIA.defaultLimit)
        assertEquals(BigDecimal("168.00"), ExpenseType.HOTEL_PARIS_SOPHIA.defaultLimit)
    }

    @Test
    fun customCumulatedParisLimitOverridesGenericDailyMealAllowance() {
        val reimbursable = calculateReceiptReimbursableAmount(
            receiptAmount = BigDecimal("50.00"),
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            dailyMealAllowance = BigDecimal("40.00"),
            customTypeLimit = BigDecimal("45.00")
        )

        assertEquals(BigDecimal("45.00"), reimbursable)
    }

    @Test
    fun customLunchLimitIsUsedForReimbursement() {
        val customLimit = effectiveExpenseLimit(
            expenseType = ExpenseType.LUNCH,
            customLimits = mapOf(ExpenseType.LUNCH to BigDecimal("32.00"))
        )
        val reimbursable = calculateReceiptReimbursableAmount(
            receiptAmount = BigDecimal("40.00"),
            expenseType = ExpenseType.LUNCH,
            dailyMealAllowance = BigDecimal("45.00"),
            customTypeLimit = customLimit
        )

        assertEquals(BigDecimal("32.00"), reimbursable)
    }

    @Test
    fun cumulatedMealInheritsCustomLunchAndDinnerLimits() {
        val limit = effectiveExpenseLimit(
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            customLimits = mapOf(
                ExpenseType.LUNCH to BigDecimal("24.00"),
                ExpenseType.DINNER_PARIS to BigDecimal("31.00")
            )
        )

        assertEquals(BigDecimal("55.00"), limit)
    }

    @Test
    fun inheritedCumulatedLimitIsUsedForReimbursement() {
        val customLimits = mapOf(
            ExpenseType.LUNCH to BigDecimal("24.00"),
            ExpenseType.DINNER_PARIS to BigDecimal("31.00")
        )
        val reimbursable = calculateReceiptReimbursableAmount(
            receiptAmount = BigDecimal("70.00"),
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            dailyMealAllowance = BigDecimal("45.00"),
            customTypeLimit = effectiveExpenseLimit(
                ExpenseType.LUNCH_DINNER_PARIS,
                customLimits
            )
        )

        assertEquals(BigDecimal("55.00"), reimbursable)
    }

    @Test
    fun explicitCumulatedLimitOverridesCustomLunchAndDinnerLimits() {
        val limit = effectiveExpenseLimit(
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            customLimits = mapOf(
                ExpenseType.LUNCH to BigDecimal("24.00"),
                ExpenseType.DINNER_PARIS to BigDecimal("31.00"),
                ExpenseType.LUNCH_DINNER_PARIS to BigDecimal("52.00")
            )
        )

        assertEquals(BigDecimal("52.00"), limit)
    }

    @Test
    fun defaultLunchAndParisDinnerCanReachFortyFiveTogether() {
        val lunch = calculateReceiptReimbursableAmount(
            receiptAmount = BigDecimal("30.00"),
            expenseType = ExpenseType.LUNCH,
            dailyMealAllowance = BigDecimal("40.00")
        )
        val dinner = calculateReceiptReimbursableAmount(
            receiptAmount = BigDecimal("30.00"),
            expenseType = ExpenseType.DINNER_PARIS,
            dailyMealAllowance = BigDecimal("40.00"),
            alreadySpentForMeals = lunch
        )

        assertEquals(BigDecimal("20.00"), lunch)
        assertEquals(BigDecimal("25.00"), dinner)
        assertEquals(BigDecimal("45.00"), lunch + dinner)
    }

    @Test
    fun parisLunchAndDinnerRaiseDailyAllowanceToFortyFive() {
        val allowance = calculateDailyMealAllowance(
            baseAllowance = BigDecimal("40.00"),
            mealTypes = listOf(ExpenseType.LUNCH, ExpenseType.DINNER_PARIS),
            customLimits = emptyMap()
        )

        assertEquals(BigDecimal("45.00"), allowance)
    }

    @Test
    fun dailyAllowanceUsesAConfiguredLimitEvenWhenItIsBelowTheZoneDefault() {
        val allowance = calculateDailyMealAllowance(
            baseAllowance = BigDecimal("45.00"),
            mealTypes = listOf(ExpenseType.LUNCH_DINNER_PARIS),
            customLimits = mapOf(ExpenseType.LUNCH_DINNER_PARIS to BigDecimal("35.00"))
        )

        assertEquals(BigDecimal("35.00"), allowance)
    }

    @Test
    fun dailyAllowanceUsesCustomLunchLimitInsteadOfZoneDefault() {
        val allowance = calculateDailyMealAllowance(
            baseAllowance = BigDecimal("45.00"),
            mealTypes = listOf(ExpenseType.LUNCH),
            customLimits = mapOf(ExpenseType.LUNCH to BigDecimal("16.00"))
        )

        assertEquals(BigDecimal("16.00"), allowance)
    }

    @Test
    fun tripZoneSelectsTheMatchingDinnerAndCumulatedTypes() {
        assertEquals(ExpenseType.DINNER_PARIS, MealZone.PARIS_SOPHIA.dinnerType())
        assertEquals(
            ExpenseType.LUNCH_DINNER_PARIS,
            MealZone.PARIS_SOPHIA.lunchDinnerType()
        )
        assertEquals(ExpenseType.DINNER_COUNTRY, MealZone.PROVINCE.dinnerType())
        assertEquals(
            ExpenseType.LUNCH_DINNER_COUNTRY,
            MealZone.PROVINCE.lunchDinnerType()
        )
    }

    @Test
    fun parisTripAutomaticallyUsesFortyFiveEuroDailyAllowance() {
        val trip = Trip(
            name = "Paris",
            startDate = java.time.LocalDate.of(2026, 7, 27),
            endDate = java.time.LocalDate.of(2026, 7, 27),
            mealZone = MealZone.PARIS_SOPHIA
        )

        assertEquals(BigDecimal("45.00"), trip.dailyMealAllowance)
    }

    @Test
    fun changingTripZoneAlsoChangesItsDailyMealAllowance() {
        val trip = Trip(
            name = "Province",
            startDate = java.time.LocalDate.of(2026, 7, 27),
            endDate = java.time.LocalDate.of(2026, 7, 27),
            mealZone = MealZone.PROVINCE
        )

        val relocated = trip.withMealZone(MealZone.PARIS_SOPHIA)

        assertEquals(MealZone.PARIS_SOPHIA, relocated.mealZone)
        assertEquals(BigDecimal("45.00"), relocated.dailyMealAllowance)
    }

    @Test
    fun newParisTripUsesTheConfiguredCumulatedAllowance() {
        assertEquals(
            BigDecimal("52.00"),
            effectiveDailyMealAllowance(
                zone = MealZone.PARIS_SOPHIA,
                customLimits = mapOf(
                    ExpenseType.LUNCH_DINNER_PARIS to BigDecimal("52.00")
                )
            )
        )
    }

    @Test
    fun dailyAllowanceUsesCustomizedLunchAndDinnerWhenNoCumulatedValueExists() {
        assertEquals(
            BigDecimal("47.00"),
            effectiveDailyMealAllowance(
                zone = MealZone.PARIS_SOPHIA,
                customLimits = mapOf(
                    ExpenseType.LUNCH to BigDecimal("21.00"),
                    ExpenseType.DINNER_PARIS to BigDecimal("26.00")
                )
            )
        )
    }

    @Test
    fun changingZoneUsesTheConfiguredAllowanceForTheDestination() {
        val date = LocalDate.of(2026, 7, 27)
        val trip = Trip(
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )

        val relocated = trip.withMealZone(
            MealZone.PARIS_SOPHIA,
            mapOf(ExpenseType.LUNCH_DINNER_PARIS to BigDecimal("51.00"))
        )

        assertEquals(BigDecimal("51.00"), relocated.dailyMealAllowance)
    }

    @Test
    fun combinedCalculationIsSuggestedWhenSeparateLimitWouldLoseMoney() {
        val beneficial = isCombinedMealCalculationBeneficial(
            entries = listOf(
                MealEntry(BigDecimal("25.00"), ExpenseType.LUNCH),
                MealEntry(BigDecimal("15.00"), ExpenseType.DINNER_PARIS)
            ),
            zone = MealZone.PARIS_SOPHIA,
            customLimits = emptyMap()
        )

        assertTrue(beneficial)
    }

    @Test
    fun existingCumulatedMealBecomesTheOppositeOfTheNewSeparateMeal() {
        assertEquals(
            ExpenseType.DINNER_PARIS,
            ExpenseType.LUNCH.oppositeTypeForExistingLunchDinner(MealZone.PARIS_SOPHIA)
        )
        assertEquals(
            ExpenseType.LUNCH,
            ExpenseType.DINNER_COUNTRY
                .oppositeTypeForExistingLunchDinner(MealZone.PROVINCE)
        )
    }

    @Test
    fun combinedCalculationIsSuggestedAboveQuotaWhenItStillPaysMore() {
        val beneficial = isCombinedMealCalculationBeneficial(
            entries = listOf(
                MealEntry(BigDecimal("30.00"), ExpenseType.LUNCH),
                MealEntry(BigDecimal("20.00"), ExpenseType.DINNER_PARIS)
            ),
            zone = MealZone.PARIS_SOPHIA,
            customLimits = emptyMap()
        )

        assertTrue(beneficial)
    }

    @Test
    fun defaultEmployerContributionIsSixEuros() {
        assertEquals(
            BigDecimal("6.00"),
            DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION
        )
    }

    @Test
    fun employerContributionIsDeductedFromFirstLunch() {
        val reimbursableBase = applyMealVoucherEmployerContribution(
            receiptAmount = BigDecimal("18.00"),
            expenseType = ExpenseType.LUNCH,
            employerContribution = BigDecimal("6.00"),
            contributionAlreadyApplied = false
        )

        assertEquals(BigDecimal("12.00"), reimbursableBase)
    }

    @Test
    fun zeroEmployerContributionDoesNotDeductAnything() {
        val reimbursableBase = applyMealVoucherEmployerContribution(
            receiptAmount = BigDecimal("18.00"),
            expenseType = ExpenseType.LUNCH,
            employerContribution = BigDecimal.ZERO,
            contributionAlreadyApplied = false
        )

        assertEquals(BigDecimal("18.00"), reimbursableBase)
    }

    @Test
    fun employerContributionIsOnlyDeductedOncePerDay() {
        val secondCumulatedReceipt = applyMealVoucherEmployerContribution(
            receiptAmount = BigDecimal("20.00"),
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            employerContribution = BigDecimal("6.00"),
            contributionAlreadyApplied = true
        )

        assertEquals(BigDecimal("20.00"), secondCumulatedReceipt)
    }

    @Test
    fun employerContributionIsNotDeductedFromDinner() {
        val dinner = applyMealVoucherEmployerContribution(
            receiptAmount = BigDecimal("20.00"),
            expenseType = ExpenseType.DINNER_PARIS,
            employerContribution = BigDecimal("6.00"),
            contributionAlreadyApplied = false
        )

        assertEquals(BigDecimal("20.00"), dinner)
    }

    @Test
    fun annotationExplainsEmployerContributionCalculation() {
        val lines = ReimbursementCalculation(
            declaredAmount = BigDecimal("20.00"),
            reimbursableAmount = BigDecimal("14.00"),
            employerContributionDeducted = BigDecimal("6.00"),
            quota = BigDecimal("20.00")
        ).annotationDetailLines()

        assertEquals(
            listOf("Participation employeur : 20,00 € - 6,00 € = 14,00 €"),
            lines
        )
    }

    @Test
    fun annotationExplainsReachedQuotaAndRetainedAmount() {
        val lines = ReimbursementCalculation(
            declaredAmount = BigDecimal("30.00"),
            reimbursableAmount = BigDecimal("8.00"),
            quota = BigDecimal("20.00"),
            alreadyReimbursedAgainstQuota = BigDecimal("12.00")
        ).annotationDetailLines()

        assertEquals(
            listOf(
                "Quota de 20,00 € atteint",
                "Déjà remboursé sur ce quota : 12,00 €",
                "Remboursement à hauteur de 8,00 €"
            ),
            lines
        )
    }

    @Test
    fun annotationCanExplainContributionAndQuotaTogether() {
        val lines = ReimbursementCalculation(
            declaredAmount = BigDecimal("30.00"),
            reimbursableAmount = BigDecimal("10.00"),
            employerContributionDeducted = BigDecimal("6.00"),
            quota = BigDecimal("20.00"),
            alreadyReimbursedAgainstQuota = BigDecimal("10.00")
        ).annotationDetailLines()

        assertEquals(4, lines.size)
        assertEquals(
            "Participation employeur : 30,00 € - 6,00 € = 24,00 €",
            lines.first()
        )
        assertEquals("Remboursement à hauteur de 10,00 €", lines.last())
    }

    @Test
    fun combinedSuggestionAccountsForEmployerContribution() {
        val beneficial = isCombinedMealCalculationBeneficial(
            entries = listOf(
                MealEntry(BigDecimal("30.00"), ExpenseType.LUNCH),
                MealEntry(BigDecimal("10.00"), ExpenseType.DINNER_PARIS)
            ),
            zone = MealZone.PARIS_SOPHIA,
            customLimits = emptyMap(),
            employerContribution = BigDecimal("6.00")
        )

        assertTrue(beneficial)
    }

    @Test
    fun acceptingCombinedCalculationConvertsBothReceiptsAndKeepsTheirFiles() {
        val date = LocalDate.of(2026, 9, 18)
        val dinner = Receipt(
            id = "dinner",
            tripId = "trip",
            date = date,
            amount = BigDecimal("15.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.jpg",
            mimeType = "image/jpeg"
        )
        val lunch = Receipt(
            id = "lunch",
            tripId = "trip",
            date = date,
            amount = BigDecimal("25.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf"
        )

        val combined = combineSeparateMealReceipts(
            receipts = listOf(dinner, lunch),
            tripId = "trip",
            date = date,
            zone = MealZone.PARIS_SOPHIA
        )

        assertEquals(listOf("lunch", "dinner"), combined.map { it.id })
        assertEquals(
            listOf(ExpenseType.LUNCH_DINNER_PARIS, ExpenseType.LUNCH_DINNER_PARIS),
            combined.map { it.expenseType }
        )
        assertEquals(listOf("lunch.pdf", "dinner.jpg"), combined.map { it.storedFileName })

        val firstBase = applyMealVoucherEmployerContribution(
            receiptAmount = combined[0].amount,
            expenseType = combined[0].expenseType,
            employerContribution = BigDecimal("6.00"),
            contributionAlreadyApplied = false
        )
        val secondBase = applyMealVoucherEmployerContribution(
            receiptAmount = combined[1].amount,
            expenseType = combined[1].expenseType,
            employerContribution = BigDecimal("6.00"),
            contributionAlreadyApplied = true
        )
        assertEquals(BigDecimal("19.00"), firstBase)
        assertEquals(BigDecimal("15.00"), secondBase)
    }

    @Test
    fun cumulatedMealReceiptsAreDisplayedAsOneLineWithBothAttachments() {
        val date = LocalDate.of(2026, 9, 18)
        val first = Receipt(
            id = "lunch",
            tripId = "trip",
            date = date,
            amount = BigDecimal("25.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf",
            reimbursableAmount = BigDecimal("19.00")
        )
        val second = Receipt(
            id = "dinner",
            tripId = "trip",
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH_DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf",
            reimbursableAmount = BigDecimal("26.00")
        )

        val groups = groupReceiptsForDisplay(listOf(first, second))

        assertEquals(1, groups.size)
        assertEquals(listOf("lunch.pdf", "dinner.pdf"), groups.single().receipts.map { it.storedFileName })
        assertEquals(BigDecimal("55.00"), groups.single().declaredAmount)
        assertEquals(BigDecimal("45.00"), groups.single().reimbursableAmount)
        assertTrue(groups.single().isCapped)
    }

    @Test
    fun submissionConvertsAnOverLimitDinnerWithoutLunchAndRecalculatesItsMark() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            id = "dinner",
            tripId = trip.id,
            date = date,
            amount = BigDecimal("60.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf",
            reimbursableAmount = BigDecimal("25.00")
        )

        val converted = convertOverLimitDinnerOnlyDays(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap()
        ).single()
        val calculation = calculateReceiptReimbursement(
            amount = converted.amount,
            expenseType = converted.expenseType,
            trip = trip,
            date = date,
            previousReceipts = emptyList(),
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(ExpenseType.LUNCH_DINNER_PARIS, converted.expenseType)
        assertEquals("dinner.pdf", converted.storedFileName)
        assertEquals(BigDecimal("6.00"), calculation.employerContributionDeducted)
        assertEquals(BigDecimal("45.00"), calculation.quota)
        assertEquals(BigDecimal("45.00"), calculation.reimbursableAmount)
        assertTrue(calculation.isQuotaLimited)
        assertEquals(3, calculation.annotationDetailLines().size)
    }

    @Test
    fun zeroEmployerContributionAddsNoDeductionOrMarkAfterDinnerConversion() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )
        val converted = convertOverLimitDinnerOnlyDays(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap()
        ).single()

        val calculation = calculateReceiptReimbursement(
            amount = converted.amount,
            expenseType = converted.expenseType,
            trip = trip,
            date = date,
            previousReceipts = emptyList(),
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal.ZERO
        )

        assertEquals(ExpenseType.LUNCH_DINNER_PARIS, converted.expenseType)
        assertEquals(BigDecimal("30.00"), calculation.reimbursableAmount)
        assertEquals(BigDecimal.ZERO, calculation.employerContributionDeducted)
        assertEquals(emptyList<String>(), calculation.annotationDetailLines())
    }

    @Test
    fun submissionUsesMealAllowanceWhenItBeatsConvertedDinner() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )
        val dinnerConverted = convertOverLimitDinnerOnlyDays(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap()
        )

        val optimized = convertToMoreAdvantageousMealAllowance(
            receipts = dinnerConverted,
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(ExpenseType.MEAL_ALLOWANCE_PARIS, optimized.single().expenseType)
    }

    @Test
    fun submissionDoesNotOverrideDeclinedAllowanceForAReceiptBelowTheAllowance() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val lunch = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("12.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf"
        )

        val optimized = convertToMoreAdvantageousMealAllowance(
            receipts = listOf(lunch),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(ExpenseType.LUNCH, optimized.single().expenseType)
        assertEquals(BigDecimal("12.00"), optimized.single().amount)
    }

    @Test
    fun submissionKeepsCombinedDinnerWhenZeroContributionMakesItMoreAdvantageous() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )
        val dinnerConverted = convertOverLimitDinnerOnlyDays(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap()
        )

        val optimized = convertToMoreAdvantageousMealAllowance(
            receipts = dinnerConverted,
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal.ZERO
        )

        assertEquals(ExpenseType.LUNCH_DINNER_PARIS, optimized.single().expenseType)
    }

    @Test
    fun customMealAllowanceIsIncludedInSubmissionComparison() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("55.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_COUNTRY,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )
        val customLimits = mapOf(
            ExpenseType.MEAL_ALLOWANCE_COUNTRY to BigDecimal("48.00")
        )
        val dinnerConverted = convertOverLimitDinnerOnlyDays(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = customLimits
        )

        val optimized = convertToMoreAdvantageousMealAllowance(
            receipts = dinnerConverted,
            trip = trip,
            customLimits = customLimits,
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(ExpenseType.MEAL_ALLOWANCE_COUNTRY, optimized.single().expenseType)
        assertEquals(BigDecimal("48.00"), optimized.single().amount)
        assertEquals(BigDecimal("48.00"), optimized.single().reimbursableAmount)
    }

    @Test
    fun allowanceWithoutAttachmentAlwaysUsesItsConfiguredAmount() {
        assertEquals(
            BigDecimal("25.00"),
            normalizedExpenseAmount(
                requestedAmount = BigDecimal("1.00"),
                expenseType = ExpenseType.MEAL_ALLOWANCE_PARIS,
                hasAttachment = false,
                customLimits = emptyMap()
            )
        )
        assertEquals(
            BigDecimal("31.50"),
            normalizedExpenseAmount(
                requestedAmount = BigDecimal("999.00"),
                expenseType = ExpenseType.MEAL_ALLOWANCE_PARIS,
                hasAttachment = false,
                customLimits = mapOf(
                    ExpenseType.MEAL_ALLOWANCE_PARIS to BigDecimal("31.50")
                )
            )
        )
    }

    @Test
    fun attachedAllowanceKeepsTheDeclaredReceiptAmount() {
        assertEquals(
            BigDecimal("18.00"),
            normalizedExpenseAmount(
                requestedAmount = BigDecimal("18.00"),
                expenseType = ExpenseType.MEAL_ALLOWANCE_PARIS,
                hasAttachment = true,
                customLimits = mapOf(
                    ExpenseType.MEAL_ALLOWANCE_PARIS to BigDecimal("31.50")
                )
            )
        )
    }

    @Test
    fun disabledAllowanceCannotKeepAnArbitraryManualAmount() {
        assertEquals(
            BigDecimal.ZERO,
            normalizedExpenseAmount(
                requestedAmount = BigDecimal("50.00"),
                expenseType = ExpenseType.MEAL_ALLOWANCE_COUNTRY,
                hasAttachment = false,
                customLimits = mapOf(
                    ExpenseType.MEAL_ALLOWANCE_COUNTRY to BigDecimal.ZERO
                )
            )
        )
    }

    @Test
    fun combinedCalculationCanBeBetterEvenWhenTheTotalExceedsItsQuota() {
        val customLimits = mapOf(
            ExpenseType.LUNCH to BigDecimal("10.00"),
            ExpenseType.DINNER_COUNTRY to BigDecimal("10.00"),
            ExpenseType.LUNCH_DINNER_COUNTRY to BigDecimal("40.00")
        )

        assertTrue(
            isCombinedMealCalculationBeneficial(
                entries = listOf(
                    MealEntry(BigDecimal("30.00"), ExpenseType.LUNCH),
                    MealEntry(BigDecimal("30.00"), ExpenseType.DINNER_COUNTRY)
                ),
                zone = MealZone.PROVINCE,
                customLimits = customLimits
            )
        )
    }

    @Test
    fun severalStructuredMealsAreNeverDuplicatedAsMealAllowances() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val receipts = listOf(
            Receipt(
                tripId = trip.id,
                date = date,
                amount = BigDecimal("35.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.LUNCH,
                storedFileName = "lunch.pdf",
                mimeType = "application/pdf"
            ),
            Receipt(
                tripId = trip.id,
                date = date,
                amount = BigDecimal("35.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.DINNER_COUNTRY,
                storedFileName = "dinner.pdf",
                mimeType = "application/pdf"
            )
        )

        val optimized = convertToMoreAdvantageousMealAllowance(
            receipts = receipts,
            trip = trip,
            customLimits = mapOf(
                ExpenseType.MEAL_ALLOWANCE_COUNTRY to BigDecimal("50.00")
            ),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(
            listOf(ExpenseType.LUNCH, ExpenseType.DINNER_COUNTRY),
            optimized.map { it.expenseType }
        )
    }

    @Test
    fun submissionReevaluatesSeparateMealsWithTheLatestLimits() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val receipts = listOf(
            Receipt(
                id = "lunch",
                tripId = trip.id,
                date = date,
                amount = BigDecimal("30.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.LUNCH,
                storedFileName = "lunch.pdf",
                mimeType = "application/pdf"
            ),
            Receipt(
                id = "dinner",
                tripId = trip.id,
                date = date,
                amount = BigDecimal("30.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.DINNER_COUNTRY,
                storedFileName = "dinner.pdf",
                mimeType = "application/pdf"
            )
        )

        val optimized = optimizeMealReceiptsForSubmission(
            receipts = receipts,
            trip = trip,
            customLimits = mapOf(
                ExpenseType.LUNCH to BigDecimal("10.00"),
                ExpenseType.DINNER_COUNTRY to BigDecimal("10.00"),
                ExpenseType.LUNCH_DINNER_COUNTRY to BigDecimal("40.00")
            ),
            mealVoucherEmployerContribution = BigDecimal.ZERO
        )

        assertEquals(2, optimized.size)
        assertTrue(optimized.all { it.expenseType == ExpenseType.LUNCH_DINNER_COUNTRY })
        assertEquals(listOf("lunch", "dinner"), optimized.map { it.id })
    }

    @Test
    fun submissionChoosesAllowanceWithItsExactAmountWhenItWins() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )

        val optimized = optimizeMealReceiptsForSubmission(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        ).single()

        assertEquals(ExpenseType.MEAL_ALLOWANCE_PARIS, optimized.expenseType)
        assertEquals(BigDecimal("25.00"), optimized.amount)
    }

    @Test
    fun submissionKeepsCombinedMealWhenItBeatsTheAllowance() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )

        val optimized = optimizeMealReceiptsForSubmission(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal.ZERO
        ).single()

        assertEquals(ExpenseType.LUNCH_DINNER_PARIS, optimized.expenseType)
        assertEquals(BigDecimal("30.00"), optimized.amount)
    }

    @Test
    fun mealAllowancePreventsLunchAndDinnerFromBeingCombined() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val receipts = listOf(
            Receipt(
                id = "lunch",
                tripId = trip.id,
                date = date,
                amount = BigDecimal("30.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.LUNCH,
                storedFileName = "lunch.pdf",
                mimeType = "application/pdf"
            ),
            Receipt(
                id = "dinner",
                tripId = trip.id,
                date = date,
                amount = BigDecimal("30.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.DINNER_COUNTRY,
                storedFileName = "dinner.pdf",
                mimeType = "application/pdf"
            ),
            Receipt(
                id = "allowance",
                tripId = trip.id,
                date = date,
                amount = BigDecimal("20.00"),
                category = ExpenseCategory.MEAL,
                expenseType = ExpenseType.MEAL_ALLOWANCE_COUNTRY,
                storedFileName = "",
                mimeType = "",
                comment = "Forfait saisi manuellement"
            )
        )

        val optimized = optimizeMealReceiptsForSubmission(
            receipts = receipts,
            trip = trip,
            customLimits = mapOf(
                ExpenseType.LUNCH to BigDecimal("10.00"),
                ExpenseType.DINNER_COUNTRY to BigDecimal("10.00"),
                ExpenseType.LUNCH_DINNER_COUNTRY to BigDecimal("40.00")
            ),
            mealVoucherEmployerContribution = BigDecimal.ZERO
        )

        assertEquals(
            listOf(
                ExpenseType.LUNCH,
                ExpenseType.DINNER_COUNTRY,
                ExpenseType.MEAL_ALLOWANCE_COUNTRY
            ),
            optimized.map { it.expenseType }
        )
        assertEquals(false, optimized.any { it.expenseType.isLunchDinner })
    }

    @Test
    fun singleLunchKeepsItsReceiptAndAddsMissingDinnerAllowanceWhenBetter() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val lunch = Receipt(
            id = "lunch",
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf"
        )

        val optimized = applySingleMealAllowanceStrategy(
            receipts = listOf(lunch),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00"),
            acceptedAllowanceDates = setOf(date)
        )

        assertEquals(2, optimized.size)
        assertEquals(ExpenseType.LUNCH, optimized.first().expenseType)
        val allowance = optimized.single { it.generatedMealAllowance }
        assertEquals(ExpenseType.MEAL_ALLOWANCE_PARIS, allowance.expenseType)
        assertEquals(BigDecimal("25.00"), allowance.amount)
        assertTrue(allowance.comment.contains("Dinner manquant"))
        assertEquals(false, allowance.hasAttachment)
    }

    @Test
    fun singleDinnerAddsMissingLunchAllowanceWhenThatCombinationIsBetter() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val dinner = Receipt(
            id = "dinner",
            tripId = trip.id,
            date = date,
            amount = BigDecimal("45.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_COUNTRY,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )

        val optimized = applySingleMealAllowanceStrategy(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00"),
            acceptedAllowanceDates = setOf(date)
        )

        assertEquals(2, optimized.size)
        assertEquals(ExpenseType.DINNER_COUNTRY, optimized.first().expenseType)
        assertTrue(
            optimized.single { it.generatedMealAllowance }
                .comment.contains("Lunch manquant")
        )
    }

    @Test
    fun advantageousMissingMealIsOnlyAddedAfterExplicitAcceptance() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val lunch = Receipt(
            id = "lunch",
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf"
        )

        val suggestions = findSingleMealAllowanceSuggestions(
            receipts = listOf(lunch),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )
        val declined = applySingleMealAllowanceStrategy(
            receipts = listOf(lunch),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(1, suggestions.size)
        assertTrue(suggestions.single().reimbursementGain > BigDecimal.ZERO)
        assertEquals("Dinner manquant", suggestions.single().missingMealLabel)
        assertEquals(1, declined.size)
        assertEquals(false, declined.single().generatedMealAllowance)
        assertEquals(ExpenseType.LUNCH_DINNER_PARIS, declined.single().expenseType)
    }

    @Test
    fun acceptedMissingMealKeepsTheOriginalReceiptType() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val lunch = Receipt(
            id = "lunch",
            tripId = trip.id,
            date = date,
            amount = BigDecimal("30.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf"
        )
        val accepted = applySingleMealAllowanceStrategy(
            receipts = listOf(lunch),
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00"),
            acceptedAllowanceDates = setOf(date)
        )

        val optimized = convertToMoreAdvantageousMealAllowance(
            receipts = accepted,
            trip = trip,
            customLimits = emptyMap(),
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(ExpenseType.LUNCH, optimized.single { !it.generatedMealAllowance }.expenseType)
        assertTrue(optimized.single { it.generatedMealAllowance }.expenseType.isMealAllowance)
    }

    @Test
    fun singleMealUsesLunchDinnerWhenItIsMoreAdvantageousThanMissingAllowance() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val dinner = Receipt(
            id = "dinner",
            tripId = trip.id,
            date = date,
            amount = BigDecimal("70.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_COUNTRY,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )
        val customLimits = mapOf(
            ExpenseType.LUNCH_DINNER_COUNTRY to BigDecimal("65.00"),
            ExpenseType.MEAL_ALLOWANCE_COUNTRY to BigDecimal("10.00")
        )

        val optimized = applySingleMealAllowanceStrategy(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = customLimits,
            mealVoucherEmployerContribution = BigDecimal("6.00")
        )

        assertEquals(1, optimized.size)
        assertEquals(ExpenseType.LUNCH_DINNER_COUNTRY, optimized.single().expenseType)
        assertEquals(false, optimized.single().generatedMealAllowance)
    }

    @Test
    fun submissionKeepsDinnerWhenItDoesNotExceedItsCustomLimit() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Paris",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PARIS_SOPHIA
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("35.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_PARIS,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )

        val result = convertOverLimitDinnerOnlyDays(
            receipts = listOf(dinner),
            trip = trip,
            customLimits = mapOf(ExpenseType.DINNER_PARIS to BigDecimal("35.00"))
        )

        assertEquals(ExpenseType.DINNER_PARIS, result.single().expenseType)
    }

    @Test
    fun submissionDoesNotConvertDinnerWhenLunchExists() {
        val date = LocalDate.of(2026, 10, 22)
        val trip = Trip(
            id = "trip",
            name = "Province",
            startDate = date,
            endDate = date,
            mealZone = MealZone.PROVINCE
        )
        val lunch = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("12.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.LUNCH,
            storedFileName = "lunch.pdf",
            mimeType = "application/pdf"
        )
        val dinner = Receipt(
            tripId = trip.id,
            date = date,
            amount = BigDecimal("45.00"),
            category = ExpenseCategory.MEAL,
            expenseType = ExpenseType.DINNER_COUNTRY,
            storedFileName = "dinner.pdf",
            mimeType = "application/pdf"
        )

        val result = convertOverLimitDinnerOnlyDays(
            receipts = listOf(lunch, dinner),
            trip = trip,
            customLimits = emptyMap()
        )

        assertEquals(
            listOf(ExpenseType.LUNCH, ExpenseType.DINNER_COUNTRY),
            result.map { it.expenseType }
        )
    }
}
