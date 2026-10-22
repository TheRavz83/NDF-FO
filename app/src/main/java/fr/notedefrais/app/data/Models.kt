package fr.notedefrais.app.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

val DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION: BigDecimal = BigDecimal("6.00")

enum class TripStatus(val label: String) {
    DRAFT("En cours de saisie"),
    SENT("Envoyée"),
    VALIDATED("Validée"),
    REIMBURSED("Remboursée")
}

enum class MealZone(
    val label: String,
    val dailyAllowance: BigDecimal
) {
    PARIS_SOPHIA("Paris / Sophia", BigDecimal("45.00")),
    PROVINCE("Province", BigDecimal("40.00"))
}

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val mealZone: MealZone = MealZone.PROVINCE,
    val dailyMealAllowance: BigDecimal = mealZone.dailyAllowance,
    val status: TripStatus = TripStatus.DRAFT,
    val submittedDate: LocalDate? = null
) {
    init {
        require(name.isNotBlank())
        require(!endDate.isBefore(startDate))
        require(dailyMealAllowance >= BigDecimal.ZERO)
        require(
            (status == TripStatus.DRAFT && submittedDate == null) ||
                (status != TripStatus.DRAFT && submittedDate != null)
        )
    }
}

fun effectiveDailyMealAllowance(
    zone: MealZone,
    customLimits: Map<ExpenseType, BigDecimal>
): BigDecimal = effectiveExpenseLimit(zone.lunchDinnerType(), customLimits)
    ?: zone.dailyAllowance

fun Trip.withMealZone(
    zone: MealZone,
    customLimits: Map<ExpenseType, BigDecimal> = emptyMap()
): Trip = copy(
    mealZone = zone,
    dailyMealAllowance = effectiveDailyMealAllowance(zone, customLimits)
)

enum class ExpenseCategory(val label: String) {
    TRANSPORT("Transport"),
    MEAL("Repas"),
    HOTEL("Hôtel"),
    HOUSING("Logement"),
    TELECOM("Télécom"),
    PROFESSIONAL("Frais professionnels"),
    MOBILITY("Mobilité"),
    OTHER("Autre")
}

enum class ExpenseType(
    val code: String,
    val label: String,
    val category: ExpenseCategory
) {
    TRANSPORT_OCCASIONAL("01", "Transports (occasional)", ExpenseCategory.TRANSPORT),
    TRANSPORT_SUBSCRIPTION("01", "Transports (Subscription)", ExpenseCategory.TRANSPORT),
    TRANSPORT_ZONE_EXTENSION("01", "Transports (Zone extension)", ExpenseCategory.TRANSPORT),
    TAXI("02", "Taxi", ExpenseCategory.TRANSPORT),
    TRAIN("03", "Train", ExpenseCategory.TRANSPORT),
    PLANE("04", "Plane", ExpenseCategory.TRANSPORT),
    CAR_RENTAL("05", "Car rental", ExpenseCategory.TRANSPORT),
    DIESEL("05", "Diesel", ExpenseCategory.TRANSPORT),
    GAS("05", "Gas", ExpenseCategory.TRANSPORT),
    MILEAGE_ALLOWANCES_BULL("05", "Mileage allowances BULL", ExpenseCategory.TRANSPORT),
    PARKING("05", "Parking", ExpenseCategory.TRANSPORT),
    TOLL("05", "Toll", ExpenseCategory.TRANSPORT),

    BREAKFAST("06", "Breakfast", ExpenseCategory.MEAL),
    DINNER_DRINK("06", "Dinner - Drink (20% VAT)", ExpenseCategory.MEAL),
    DINNER_COUNTRY("06", "Dinner country", ExpenseCategory.MEAL),
    DINNER_PARIS("06", "Dinner Paris", ExpenseCategory.MEAL),
    HOTEL_MEAL_PART("06", "Hotel - Meal part", ExpenseCategory.MEAL),
    LUNCH_DINNER_COUNTRY("06", "Lnch+Dnnr Cntry (cumulated)", ExpenseCategory.MEAL),
    LUNCH_DINNER_PARIS("06", "Lnch+Dnnr Paris (cumulated)", ExpenseCategory.MEAL),
    LUNCH("06", "Lunch", ExpenseCategory.MEAL),
    LUNCH_DRINK("06", "Lunch - Drink (20% VAT)", ExpenseCategory.MEAL),
    MEAL_ABROAD("06", "Meal abroad", ExpenseCategory.MEAL),
    MEAL_ALLOWANCE_COUNTRY("06", "Meal allowance country", ExpenseCategory.MEAL),
    MEAL_ALLOWANCE_PARIS("06", "Meal allowance Paris", ExpenseCategory.MEAL),
    RECEPTION("06", "Reception", ExpenseCategory.MEAL),

    HOTEL_ABROAD("07", "Hotel abroad", ExpenseCategory.HOTEL),
    HOTEL_EXCEPT_PARIS_SOPHIA("07", "Hotel autres villes de province", ExpenseCategory.HOTEL),
    HOTEL_PARIS_SOPHIA(
        "07",
        "Hotel Île-de-France / Sophia / Lyon / Aix / Marseille / Bordeaux",
        ExpenseCategory.HOTEL
    ),

    HOUSING_PARIS_SOPHIA("08", "Housing allow. Paris/Sophia", ExpenseCategory.HOUSING),
    HOUSING_ABROAD("08", "Housing allowance abroad", ExpenseCategory.HOUSING),
    HOUSING_COUNTRY("08", "Housing allowance country", ExpenseCategory.HOUSING),
    ELECTRICAL_DIAGNOSIS("09", "Electrical diagnosis", ExpenseCategory.HOUSING),
    FURNITURE("09", "Furniture", ExpenseCategory.HOUSING),
    INTERNET_HOME_WORK("09", "Pers. Internet - Home work", ExpenseCategory.HOUSING),

    INTERNET_ON_CALL("10", "Pers. Internet - On call", ExpenseCategory.TELECOM),
    PHONE("10", "Phone", ExpenseCategory.TELECOM),
    PHONE_PACKAGE("10", "Phone package", ExpenseCategory.TELECOM),

    BANK_FEES("10", "Bank fees", ExpenseCategory.PROFESSIONAL),
    BOOKS("10", "Books", ExpenseCategory.PROFESSIONAL),
    CUSTOMER_PRESENT("10", "Customer present", ExpenseCategory.PROFESSIONAL),
    POSTAGE("10", "Postage", ExpenseCategory.PROFESSIONAL),
    SEMINAR("10", "Seminar", ExpenseCategory.PROFESSIONAL),
    SMALL_EQUIPMENT("10", "Small equipment", ExpenseCategory.PROFESSIONAL),
    VISA_PASSPORT("10", "Visa - Passport", ExpenseCategory.PROFESSIONAL),

    RELOCATION("10", "Relocation", ExpenseCategory.MOBILITY),
    MOVE_ACCOMPANYING_MEASURES("11", "Move accompanying measures", ExpenseCategory.MOBILITY),

    OTHER("99", "Autre type de frais", ExpenseCategory.OTHER);

    val displayLabel: String get() = "$code $label"
    val defaultLimit: BigDecimal?
        get() = when (this) {
            LUNCH,
            LUNCH_DRINK,
            DINNER_DRINK,
            DINNER_COUNTRY,
            MEAL_ALLOWANCE_COUNTRY -> BigDecimal("20.00")

            DINNER_PARIS,
            MEAL_ALLOWANCE_PARIS -> BigDecimal("25.00")

            LUNCH_DINNER_COUNTRY -> BigDecimal("40.00")
            LUNCH_DINNER_PARIS -> BigDecimal("45.00")
            HOTEL_EXCEPT_PARIS_SOPHIA -> BigDecimal("130.00")
            HOTEL_PARIS_SOPHIA -> BigDecimal("168.00")
            else -> null
        }

    companion object {
        fun forCategory(category: ExpenseCategory): List<ExpenseType> =
            entries.filter { it.category == category }

        fun defaultFor(category: ExpenseCategory): ExpenseType =
            if (category == ExpenseCategory.MEAL) {
                LUNCH
            } else {
                forCategory(category).firstOrNull() ?: OTHER
            }
    }
}

data class Receipt(
    val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val category: ExpenseCategory,
    val expenseType: ExpenseType = ExpenseType.defaultFor(category),
    val storedFileName: String,
    val mimeType: String,
    val comment: String = "",
    val reimbursableAmount: BigDecimal = amount,
    val combinedMealCalculation: Boolean = false,
    val generatedMealAllowance: Boolean = false
) {
    init {
        require(amount >= BigDecimal.ZERO)
        require(expenseType.category == category)
        require(!generatedMealAllowance || expenseType.isMealAllowance)
        require(reimbursableAmount >= BigDecimal.ZERO)
        require(reimbursableAmount <= amount)
    }

    val isCapped: Boolean get() = reimbursableAmount < amount
    val hasAttachment: Boolean get() = storedFileName.isNotBlank()
}

data class ReceiptDisplayGroup(
    val receipts: List<Receipt>
) {
    init {
        require(receipts.isNotEmpty())
    }

    val primaryReceipt: Receipt get() = receipts.first()
    val declaredAmount: BigDecimal
        get() = receipts.fold(BigDecimal.ZERO) { total, receipt -> total + receipt.amount }
    val reimbursableAmount: BigDecimal
        get() = receipts.fold(BigDecimal.ZERO) { total, receipt ->
            total + receipt.reimbursableAmount
        }
    val isCapped: Boolean get() = reimbursableAmount < declaredAmount
}

/**
 * Regroupe les justificatifs Lunch + Dinner d'une même journée pour que la liste
 * présente une seule dépense cumulée, tout en conservant chaque fichier séparément.
 */
fun groupReceiptsForDisplay(receipts: List<Receipt>): List<ReceiptDisplayGroup> {
    data class CombinedKey(
        val tripId: String,
        val date: LocalDate,
        val expenseType: ExpenseType
    )

    val combinedByKey = receipts
        .filter { it.expenseType.isLunchDinner }
        .groupBy { CombinedKey(it.tripId, it.date, it.expenseType) }
    val emittedKeys = mutableSetOf<CombinedKey>()

    return buildList {
        receipts.forEach { receipt ->
            if (!receipt.expenseType.isLunchDinner) {
                add(ReceiptDisplayGroup(listOf(receipt)))
                return@forEach
            }

            val key = CombinedKey(receipt.tripId, receipt.date, receipt.expenseType)
            if (emittedKeys.add(key)) {
                add(ReceiptDisplayGroup(combinedByKey.getValue(key)))
            }
        }
    }
}

data class ReimbursementCalculation(
    val declaredAmount: BigDecimal,
    val reimbursableAmount: BigDecimal,
    val employerContributionDeducted: BigDecimal = BigDecimal.ZERO,
    val quota: BigDecimal? = null,
    val alreadyReimbursedAgainstQuota: BigDecimal = BigDecimal.ZERO
) {
    init {
        require(declaredAmount >= BigDecimal.ZERO)
        require(reimbursableAmount >= BigDecimal.ZERO)
        require(reimbursableAmount <= declaredAmount)
        require(employerContributionDeducted >= BigDecimal.ZERO)
        require(quota == null || quota >= BigDecimal.ZERO)
        require(alreadyReimbursedAgainstQuota >= BigDecimal.ZERO)
    }

    val amountAfterEmployerContribution: BigDecimal
        get() = (declaredAmount - employerContributionDeducted)
            .coerceAtLeast(BigDecimal.ZERO)

    val isQuotaLimited: Boolean
        get() = quota != null && reimbursableAmount < amountAfterEmployerContribution
}

internal fun ReimbursementCalculation.annotationDetailLines(): List<String> = buildList {
    if (employerContributionDeducted.signum() > 0) {
        add(
            "Participation employeur : ${declaredAmount.annotationAmount()} € - " +
                "${employerContributionDeducted.annotationAmount()} € = " +
                "${amountAfterEmployerContribution.annotationAmount()} €"
        )
    }
    if (isQuotaLimited) {
        add("Quota de ${requireNotNull(quota).annotationAmount()} € atteint")
        if (alreadyReimbursedAgainstQuota.signum() > 0) {
            add(
                "Déjà remboursé sur ce quota : " +
                    "${alreadyReimbursedAgainstQuota.annotationAmount()} €"
            )
        }
        add("Remboursement à hauteur de ${reimbursableAmount.annotationAmount()} €")
    }
}

internal fun BigDecimal.annotationAmount(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')

val ExpenseType.isLunch: Boolean
    get() = this == ExpenseType.LUNCH

val ExpenseType.isDinner: Boolean
    get() = this == ExpenseType.DINNER_COUNTRY || this == ExpenseType.DINNER_PARIS

val ExpenseType.isLunchDinner: Boolean
    get() = this == ExpenseType.LUNCH_DINNER_COUNTRY ||
        this == ExpenseType.LUNCH_DINNER_PARIS

val ExpenseType.isMealAllowance: Boolean
    get() = this == ExpenseType.MEAL_ALLOWANCE_COUNTRY ||
        this == ExpenseType.MEAL_ALLOWANCE_PARIS

val ExpenseType.isStructuredMeal: Boolean
    get() = isLunch || isDinner || isLunchDinner

val ExpenseType.receivesMealVoucherDeduction: Boolean
    get() = isLunch || isLunchDinner

fun MealZone.dinnerType(): ExpenseType = when (this) {
    MealZone.PARIS_SOPHIA -> ExpenseType.DINNER_PARIS
    MealZone.PROVINCE -> ExpenseType.DINNER_COUNTRY
}

fun MealZone.lunchDinnerType(): ExpenseType = when (this) {
    MealZone.PARIS_SOPHIA -> ExpenseType.LUNCH_DINNER_PARIS
    MealZone.PROVINCE -> ExpenseType.LUNCH_DINNER_COUNTRY
}

fun MealZone.mealAllowanceType(): ExpenseType = when (this) {
    MealZone.PARIS_SOPHIA -> ExpenseType.MEAL_ALLOWANCE_PARIS
    MealZone.PROVINCE -> ExpenseType.MEAL_ALLOWANCE_COUNTRY
}

/**
 * Retourne le plafond réellement applicable à un type de dépense.
 *
 * Un plafond saisi explicitement est toujours prioritaire. Pour un cumul
 * Lunch + Dinner sans plafond cumulé explicite, le cumul est construit à
 * partir des plafonds effectifs du lunch et du dinner de la zone. Ainsi une
 * personnalisation de l'un de ces deux repas est également prise en compte
 * lorsque l'application regroupe leurs justificatifs.
 */
fun effectiveExpenseLimit(
    expenseType: ExpenseType,
    customLimits: Map<ExpenseType, BigDecimal>
): BigDecimal? {
    customLimits[expenseType]?.let { return it }
    if (!expenseType.isLunchDinner) return expenseType.defaultLimit

    val dinnerType = when (expenseType) {
        ExpenseType.LUNCH_DINNER_PARIS -> ExpenseType.DINNER_PARIS
        ExpenseType.LUNCH_DINNER_COUNTRY -> ExpenseType.DINNER_COUNTRY
        else -> return expenseType.defaultLimit
    }
    val lunchLimit = customLimits[ExpenseType.LUNCH] ?: ExpenseType.LUNCH.defaultLimit
    val dinnerLimit = customLimits[dinnerType] ?: dinnerType.defaultLimit
    return if (lunchLimit != null && dinnerLimit != null) {
        lunchLimit + dinnerLimit
    } else {
        expenseType.defaultLimit
    }
}

/**
 * Un forfait saisi sans justificatif représente un droit fixe et non une
 * dépense déclarée librement. Son montant est donc toujours celui du forfait
 * effectif (personnalisé ou par défaut).
 */
fun normalizedExpenseAmount(
    requestedAmount: BigDecimal,
    expenseType: ExpenseType,
    hasAttachment: Boolean,
    customLimits: Map<ExpenseType, BigDecimal>
): BigDecimal = if (!hasAttachment && expenseType.isMealAllowance) {
    effectiveExpenseLimit(expenseType, customLimits)
        ?: requestedAmount
} else {
    requestedAmount
}

/**
 * À la soumission, un dinner isolé qui dépasse son plafond est déclaré comme
 * Lunch + Dinner afin d'utiliser le plafond cumulé. Les justificatifs restent
 * distincts et seront regroupés par l'affichage et l'export.
 */
fun convertOverLimitDinnerOnlyDays(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>
): List<Receipt> {
    val tripReceiptsByDate = receipts
        .filter { it.tripId == trip.id }
        .groupBy { it.date }
    val dinnerLimit = effectiveExpenseLimit(trip.mealZone.dinnerType(), customLimits)
        ?: return receipts
    val datesToConvert = tripReceiptsByDate
        .filterValues { dailyReceipts ->
            val hasLunchOrCombined = dailyReceipts.any {
                it.expenseType.isLunch || it.expenseType.isLunchDinner
            }
            val hasMealAllowance = dailyReceipts.any { it.expenseType.isMealAllowance }
            val dinners = dailyReceipts.filter { it.expenseType.isDinner }
            !hasLunchOrCombined && !hasMealAllowance &&
                dinners.isNotEmpty() &&
                dinners.fold(BigDecimal.ZERO) { total, receipt -> total + receipt.amount } >
                dinnerLimit
        }
        .keys
    if (datesToConvert.isEmpty()) return receipts

    val combinedType = trip.mealZone.lunchDinnerType()
    return receipts.map { receipt ->
        if (
            receipt.tripId == trip.id &&
            receipt.date in datesToConvert &&
            receipt.expenseType.isDinner
        ) {
            receipt.copy(
                category = combinedType.category,
                expenseType = combinedType,
                combinedMealCalculation = false
            )
        } else {
            receipt
        }
    }
}

data class SingleMealAllowanceSuggestion(
    val date: LocalDate,
    val missingMealLabel: String,
    val allowanceAmount: BigDecimal,
    val reimbursementWithAllowance: BigDecimal,
    val reimbursementWithoutAddition: BigDecimal
) {
    val reimbursementGain: BigDecimal
        get() = reimbursementWithAllowance - reimbursementWithoutAddition
}

private data class SingleMealAllowanceCandidate(
    val suggestion: SingleMealAllowanceSuggestion?,
    val enteredMeal: Receipt,
    val fallbackReceipt: Receipt,
    val generatedAllowance: Receipt
)

/**
 * Repère les journées où conserver le seul repas saisi et ajouter le repas
 * manquant en Meal allowance serait plus avantageux que le cumul Lunch + Dinner.
 * Cette fonction analyse uniquement : elle ne crée aucune ligne.
 */
fun findSingleMealAllowanceSuggestions(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal
): List<SingleMealAllowanceSuggestion> = singleMealAllowanceCandidates(
    receipts = receipts,
    trip = trip,
    customLimits = customLimits,
    mealVoucherEmployerContribution = mealVoucherEmployerContribution
).mapNotNull { it.suggestion }

/**
 * Applique la stratégie de soumission. Une Meal allowance sans justificatif
 * n'est ajoutée que pour les dates explicitement acceptées par l'utilisateur.
 * En cas de refus, le repas existant utilise la meilleure solution sans ajout
 * (calcul Lunch + Dinner ou Meal allowance appliqué au justificatif existant).
 */
fun applySingleMealAllowanceStrategy(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    acceptedAllowanceDates: Set<LocalDate> = emptySet()
): List<Receipt> {
    val candidates = singleMealAllowanceCandidates(
        receipts = receipts,
        trip = trip,
        customLimits = customLimits,
        mealVoucherEmployerContribution = mealVoucherEmployerContribution
    )
    if (candidates.isEmpty()) return receipts

    val replacements = candidates.associate { candidate ->
        candidate.enteredMeal.id to candidate.fallbackReceipt
    }
    val additions = candidates
        .filter { it.suggestion?.date in acceptedAllowanceDates }
        .map { it.generatedAllowance }
    val acceptedReceiptIds = candidates
        .filter { it.suggestion?.date in acceptedAllowanceDates }
        .mapTo(mutableSetOf()) { it.enteredMeal.id }

    return receipts.map { receipt ->
        if (receipt.id in acceptedReceiptIds) receipt else replacements[receipt.id] ?: receipt
    } + additions
}

private fun singleMealAllowanceCandidates(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal
): List<SingleMealAllowanceCandidate> {
    val allowanceType = trip.mealZone.mealAllowanceType()
    val allowanceAmount = effectiveExpenseLimit(allowanceType, customLimits)
        ?.takeIf { it.signum() > 0 }
        ?: return emptyList()

    fun reimbursableTotal(dailyReceipts: List<Receipt>): BigDecimal {
        val previousReceipts = mutableListOf<Receipt>()
        return dailyReceipts
            .filter { it.category == ExpenseCategory.MEAL }
            .fold(BigDecimal.ZERO) { total, receipt ->
                val calculation = calculateReceiptReimbursement(
                    amount = receipt.amount,
                    expenseType = receipt.expenseType,
                    trip = trip,
                    date = receipt.date,
                    previousReceipts = previousReceipts,
                    customLimits = customLimits,
                    mealVoucherEmployerContribution = mealVoucherEmployerContribution,
                    combinedMealCalculation = receipt.combinedMealCalculation
                )
                previousReceipts += receipt.copy(
                    reimbursableAmount = calculation.reimbursableAmount
                )
                total + calculation.reimbursableAmount
            }
    }

    return receipts
        .filter { it.tripId == trip.id }
        .groupBy { it.date }
        .mapNotNull { (date, dailyReceipts) ->
            if (dailyReceipts.any { it.expenseType.isMealAllowance }) return@mapNotNull null
            val enteredMeals = dailyReceipts.filter {
                it.expenseType.isLunch || it.expenseType.isDinner
            }
            if (
                enteredMeals.size != 1 ||
                dailyReceipts.any { it.expenseType.isLunchDinner }
            ) {
                return@mapNotNull null
            }

            val enteredMeal = enteredMeals.single()
            val combinedType = trip.mealZone.lunchDinnerType()
            val combinedReceipt = enteredMeal.copy(
                category = combinedType.category,
                expenseType = combinedType,
                combinedMealCalculation = false
            )
            val combinedScenario = dailyReceipts.map { receipt ->
                if (receipt.id == enteredMeal.id) combinedReceipt else receipt
            }
            val currentReimbursement = reimbursableTotal(dailyReceipts)
            val combinedReimbursement = reimbursableTotal(combinedScenario)
            val fallbackReceipt: Receipt
            val fallbackReimbursement: BigDecimal
            if (combinedReimbursement > currentReimbursement) {
                fallbackReceipt = combinedReceipt
                fallbackReimbursement = combinedReimbursement
            } else {
                fallbackReceipt = enteredMeal
                fallbackReimbursement = currentReimbursement
            }
            val missingMealLabel = if (enteredMeal.expenseType.isLunch) {
                "Dinner manquant"
            } else {
                "Lunch manquant"
            }
            val generatedAllowance = Receipt(
                id = "meal-allowance:${trip.id}:$date",
                tripId = trip.id,
                date = date,
                amount = allowanceAmount,
                category = allowanceType.category,
                expenseType = allowanceType,
                storedFileName = "",
                mimeType = "",
                comment = "Meal allowance automatique — $missingMealLabel",
                reimbursableAmount = allowanceAmount,
                generatedMealAllowance = true
            )
            val allowanceScenario = dailyReceipts + generatedAllowance
            val allowanceReimbursement = reimbursableTotal(allowanceScenario)
            SingleMealAllowanceCandidate(
                suggestion = if (allowanceReimbursement > fallbackReimbursement) {
                    SingleMealAllowanceSuggestion(
                        date = date,
                        missingMealLabel = missingMealLabel,
                        allowanceAmount = allowanceAmount,
                        reimbursementWithAllowance = allowanceReimbursement,
                        reimbursementWithoutAddition = fallbackReimbursement
                    )
                } else {
                    null
                },
                enteredMeal = enteredMeal,
                fallbackReceipt = fallbackReceipt,
                generatedAllowance = generatedAllowance
            )
        }
        .sortedBy { it.enteredMeal.date }
}

/**
 * Compare le calcul des repas saisis au Meal allowance de la zone et retient
 * ce dernier uniquement s'il augmente réellement le remboursement journalier.
 */
fun convertToMoreAdvantageousMealAllowance(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal
): List<Receipt> {
    val allowanceType = trip.mealZone.mealAllowanceType()
    val allowanceAmount = effectiveExpenseLimit(allowanceType, customLimits)
        ?.takeIf { it.signum() > 0 }
        ?: return receipts
    val datesToConvert = receipts
        .filter { it.tripId == trip.id }
        .groupBy { it.date }
        .filterValues { dailyReceipts ->
            if (dailyReceipts.any { it.generatedMealAllowance }) {
                return@filterValues false
            }
            val structuredMeals = dailyReceipts.filter { it.expenseType.isStructuredMeal }
            // Un forfait remplace une seule dépense repas. Convertir plusieurs
            // tickets créerait plusieurs lignes de forfait concurrentes pour
            // la même journée et fausserait la comparaison.
            if (structuredMeals.size != 1) {
                return@filterValues false
            }
            if (structuredMeals.single().amount < allowanceAmount) {
                // Ce cas est soumis au choix explicite de l'utilisateur lors
                // de la saisie du repas ; la soumission ne doit pas l'écraser.
                return@filterValues false
            }

            fun reimbursableTotal(useMealAllowance: Boolean): BigDecimal {
                val previousReceipts = mutableListOf<Receipt>()
                return dailyReceipts.fold(BigDecimal.ZERO) { total, receipt ->
                    val candidate = if (
                        useMealAllowance && receipt.expenseType.isStructuredMeal
                    ) {
                        receipt.copy(
                            category = allowanceType.category,
                            expenseType = allowanceType,
                            combinedMealCalculation = false
                        )
                    } else {
                        receipt
                    }
                    val calculation = calculateReceiptReimbursement(
                        amount = candidate.amount,
                        expenseType = candidate.expenseType,
                        trip = trip,
                        date = candidate.date,
                        previousReceipts = previousReceipts,
                        customLimits = customLimits,
                        mealVoucherEmployerContribution =
                            mealVoucherEmployerContribution,
                        combinedMealCalculation = candidate.combinedMealCalculation
                    )
                    previousReceipts += candidate.copy(
                        reimbursableAmount = calculation.reimbursableAmount
                    )
                    total + calculation.reimbursableAmount
                }
            }

            reimbursableTotal(useMealAllowance = true) >
                reimbursableTotal(useMealAllowance = false)
        }
        .keys
    if (datesToConvert.isEmpty()) return receipts

    return receipts.map { receipt ->
        if (
            receipt.tripId == trip.id &&
            receipt.date in datesToConvert &&
            receipt.expenseType.isStructuredMeal
        ) {
            receipt.copy(
                category = allowanceType.category,
                expenseType = allowanceType,
                amount = allowanceAmount,
                reimbursableAmount = allowanceAmount,
                combinedMealCalculation = false
            )
        } else {
            receipt
        }
    }
}

fun ExpenseType.forMealZone(zone: MealZone): ExpenseType = when {
    isDinner -> zone.dinnerType()
    isLunchDinner -> zone.lunchDinnerType()
    else -> this
}

fun ExpenseType.oppositeTypeForExistingLunchDinner(zone: MealZone): ExpenseType? = when {
    isLunch -> zone.dinnerType()
    isDinner -> ExpenseType.LUNCH
    else -> null
}

internal fun combineSeparateMealReceipts(
    receipts: List<Receipt>,
    tripId: String,
    date: LocalDate,
    zone: MealZone
): List<Receipt> {
    val separateMeals = receipts.withIndex()
        .filter { (_, receipt) ->
            receipt.tripId == tripId &&
                receipt.date == date &&
                (receipt.expenseType.isLunch || receipt.expenseType.isDinner)
        }
    if (
        separateMeals.none { (_, receipt) -> receipt.expenseType.isLunch } ||
        separateMeals.none { (_, receipt) -> receipt.expenseType.isDinner }
    ) {
        return receipts
    }

    // Le lunch reste le premier justificatif du cumul : c'est sur lui que la
    // participation employeur doit être déduite lors du recalcul journalier.
    val combinedType = zone.lunchDinnerType()
    val convertedMeals = separateMeals
        .sortedWith(
            compareBy<IndexedValue<Receipt>>(
                { if (it.value.expenseType.isLunch) 0 else 1 },
                { it.index }
            )
        )
        .map { (_, receipt) ->
            receipt.copy(
                category = combinedType.category,
                expenseType = combinedType,
                combinedMealCalculation = false
            )
        }
        .iterator()

    return receipts.map { receipt ->
        if (
            receipt.tripId == tripId &&
            receipt.date == date &&
            (receipt.expenseType.isLunch || receipt.expenseType.isDinner)
        ) {
            convertedMeals.next()
        } else {
            receipt
        }
    }
}

/**
 * Réévalue les Lunch et Dinner séparés avec les plafonds courants. Cette passe
 * est également exécutée à la soumission afin qu'un changement de paramètres
 * postérieur à la saisie ne conserve pas une stratégie devenue défavorable.
 */
fun combineAdvantageousSeparateMealDays(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal
): List<Receipt> {
    var optimized = receipts
    receipts
        .filter { it.tripId == trip.id }
        .groupBy { it.date }
        .toSortedMap()
        .forEach { (date, dailyReceipts) ->
            if (dailyReceipts.any { it.expenseType.isMealAllowance }) {
                return@forEach
            }
            val entries = dailyReceipts
                .filter { it.expenseType.isLunch || it.expenseType.isDinner }
                .map { MealEntry(it.amount, it.expenseType.forMealZone(trip.mealZone)) }
            if (
                isCombinedMealCalculationBeneficial(
                    entries = entries,
                    zone = trip.mealZone,
                    customLimits = customLimits,
                    employerContribution = mealVoucherEmployerContribution
                )
            ) {
                optimized = combineSeparateMealReceipts(
                    receipts = optimized,
                    tripId = trip.id,
                    date = date,
                    zone = trip.mealZone
                )
            }
        }
    return optimized
}

/** Applique dans un ordre stable toutes les règles repas de la soumission. */
fun optimizeMealReceiptsForSubmission(
    receipts: List<Receipt>,
    trip: Trip,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    acceptedAllowanceDates: Set<LocalDate> = emptySet()
): List<Receipt> {
    val combined = combineAdvantageousSeparateMealDays(
        receipts = receipts,
        trip = trip,
        customLimits = customLimits,
        mealVoucherEmployerContribution = mealVoucherEmployerContribution
    )
    val withOptionalMissingAllowance = applySingleMealAllowanceStrategy(
        receipts = combined,
        trip = trip,
        customLimits = customLimits,
        mealVoucherEmployerContribution = mealVoucherEmployerContribution,
        acceptedAllowanceDates = acceptedAllowanceDates
    )
    val convertedDinner = convertOverLimitDinnerOnlyDays(
        receipts = withOptionalMissingAllowance,
        trip = trip,
        customLimits = customLimits
    )
    return convertToMoreAdvantageousMealAllowance(
        receipts = convertedDinner,
        trip = trip,
        customLimits = customLimits,
        mealVoucherEmployerContribution = mealVoucherEmployerContribution
    )
}

data class MealEntry(
    val amount: BigDecimal,
    val expenseType: ExpenseType
)

fun isCombinedMealCalculationBeneficial(
    entries: List<MealEntry>,
    zone: MealZone,
    customLimits: Map<ExpenseType, BigDecimal>,
    employerContribution: BigDecimal = BigDecimal.ZERO
): Boolean {
    val lunches = entries.filter { it.expenseType.isLunch }
    val dinners = entries.filter { it.expenseType.isDinner }
    if (lunches.isEmpty() || dinners.isEmpty()) return false

    val lunchTotalBeforeContribution =
        lunches.fold(BigDecimal.ZERO) { total, entry -> total + entry.amount }
    val lunchTotal = (lunchTotalBeforeContribution - employerContribution)
        .coerceAtLeast(BigDecimal.ZERO)
    val dinnerTotal = dinners.fold(BigDecimal.ZERO) { total, entry -> total + entry.amount }
    val lunchLimit = effectiveExpenseLimit(ExpenseType.LUNCH, customLimits)
        ?: BigDecimal.ZERO
    val dinnerType = zone.dinnerType()
    val dinnerLimit = effectiveExpenseLimit(dinnerType, customLimits) ?: BigDecimal.ZERO
    val combinedType = zone.lunchDinnerType()
    val combinedLimit = effectiveExpenseLimit(combinedType, customLimits)
        ?: zone.dailyAllowance
    val individualReimbursement =
        lunchTotal.coerceAtMost(lunchLimit) + dinnerTotal.coerceAtMost(dinnerLimit)
    val total = lunchTotal + dinnerTotal
    val combinedReimbursement = total.coerceAtMost(combinedLimit)
    return combinedReimbursement > individualReimbursement
}

data class ExpenseState(
    val trips: List<Trip> = emptyList(),
    val receipts: List<Receipt> = emptyList(),
    val customExpenseLimits: Map<ExpenseType, BigDecimal> = emptyMap(),
    val mealVoucherEmployerContribution: BigDecimal =
        DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION
)

data class DailySummary(
    val date: LocalDate,
    val mealSpent: BigDecimal,
    val allowance: BigDecimal
) {
    val remaining: BigDecimal get() = allowance - mealSpent
    val ratio: Float
        get() = if (allowance.signum() == 0) {
            if (mealSpent.signum() == 0) 0f else 1f
        } else {
            (mealSpent.toFloat() / allowance.toFloat()).coerceIn(0f, 1f)
        }
}

internal fun calculateReceiptReimbursement(
    amount: BigDecimal,
    expenseType: ExpenseType,
    trip: Trip,
    date: LocalDate,
    previousReceipts: List<Receipt>,
    customLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    combinedMealCalculation: Boolean = false
): ReimbursementCalculation {
    val contributionEnabled = mealVoucherEmployerContribution.signum() > 0
    val contributionAlreadyApplied = contributionEnabled &&
        previousReceipts.any {
            it.tripId == trip.id &&
                it.date == date &&
                it.expenseType.receivesMealVoucherDeduction
        }
    val reimbursableBase = applyMealVoucherEmployerContribution(
        receiptAmount = amount,
        expenseType = expenseType,
        employerContribution = mealVoucherEmployerContribution,
        contributionAlreadyApplied = contributionAlreadyApplied
    )
    val contributionDeducted = if (
        contributionEnabled &&
        expenseType.receivesMealVoucherDeduction &&
        !contributionAlreadyApplied
    ) {
        amount - reimbursableBase
    } else {
        BigDecimal.ZERO
    }
    if (combinedMealCalculation && (expenseType.isLunch || expenseType.isDinner)) {
        val combinedType = trip.mealZone.lunchDinnerType()
        val combinedLimit = effectiveExpenseLimit(combinedType, customLimits)
            ?: trip.dailyMealAllowance
        val alreadySpentCombined = previousReceipts
            .filter {
                it.tripId == trip.id &&
                    it.date == date &&
                    it.combinedMealCalculation &&
                    (it.expenseType.isLunch || it.expenseType.isDinner)
            }
            .fold(BigDecimal.ZERO) { total, previous ->
                total + previous.reimbursableAmount
            }
        val reimbursableAmount = calculateReimbursableAmount(
            receiptAmount = reimbursableBase,
            dailyAllowance = combinedLimit,
            alreadySpent = alreadySpentCombined
        )
        return ReimbursementCalculation(
            declaredAmount = amount,
            reimbursableAmount = reimbursableAmount,
            employerContributionDeducted = contributionDeducted,
            quota = combinedLimit,
            alreadyReimbursedAgainstQuota = alreadySpentCombined
        )
    }
    val alreadySpentForType = if (expenseType.isMealAllowance) {
        // Le meal allowance est un forfait par repas : deux lignes le même
        // jour disposent chacune de leur forfait complet.
        BigDecimal.ZERO
    } else {
        previousReceipts
            .filter {
                it.tripId == trip.id &&
                    it.date == date &&
                    it.expenseType == expenseType
            }
            .fold(BigDecimal.ZERO) { total, previous ->
                total + previous.reimbursableAmount
            }
    }
    val alreadySpentForMeals = previousReceipts
        .filter {
            it.tripId == trip.id &&
                it.date == date &&
                it.category == ExpenseCategory.MEAL
        }
        .fold(BigDecimal.ZERO) { total, previous ->
            total + previous.reimbursableAmount
        }
    val configuredTypeLimit = effectiveExpenseLimit(expenseType, customLimits)
    val quota = when {
        configuredTypeLimit != null -> configuredTypeLimit
        expenseType.category == ExpenseCategory.MEAL -> trip.dailyMealAllowance
        else -> null
    }
    val alreadyReimbursedAgainstQuota = when {
        expenseType.isMealAllowance -> BigDecimal.ZERO
        configuredTypeLimit != null && expenseType.category == ExpenseCategory.MEAL ->
            alreadySpentForType
        configuredTypeLimit == null && expenseType.category == ExpenseCategory.MEAL ->
            alreadySpentForMeals
        else -> BigDecimal.ZERO
    }
    val reimbursableAmount = calculateReceiptReimbursableAmount(
        receiptAmount = reimbursableBase,
        expenseType = expenseType,
        dailyMealAllowance = trip.dailyMealAllowance,
        alreadySpentForType = alreadySpentForType,
        alreadySpentForMeals = alreadySpentForMeals,
        customTypeLimit = configuredTypeLimit
    )
    return ReimbursementCalculation(
        declaredAmount = amount,
        reimbursableAmount = reimbursableAmount,
        employerContributionDeducted = contributionDeducted,
        quota = quota,
        alreadyReimbursedAgainstQuota = alreadyReimbursedAgainstQuota
    )
}

fun calculateReimbursableAmount(
    receiptAmount: BigDecimal,
    dailyAllowance: BigDecimal,
    alreadySpent: BigDecimal,
    typeLimit: BigDecimal? = null
): BigDecimal {
    val remaining = (dailyAllowance - alreadySpent).coerceAtLeast(BigDecimal.ZERO)
    return applyExpenseLimit(receiptAmount, typeLimit)
        .coerceAtMost(remaining)
        .coerceAtLeast(BigDecimal.ZERO)
}

fun applyExpenseLimit(
    receiptAmount: BigDecimal,
    typeLimit: BigDecimal?
): BigDecimal = typeLimit
    ?.coerceAtLeast(BigDecimal.ZERO)
    ?.let(receiptAmount::coerceAtMost)
    ?: receiptAmount

fun applyMealVoucherEmployerContribution(
    receiptAmount: BigDecimal,
    expenseType: ExpenseType,
    employerContribution: BigDecimal,
    contributionAlreadyApplied: Boolean
): BigDecimal =
    if (
        employerContribution.signum() > 0 &&
        expenseType.receivesMealVoucherDeduction &&
        !contributionAlreadyApplied
    ) {
        (receiptAmount - employerContribution.coerceAtLeast(BigDecimal.ZERO))
            .coerceAtLeast(BigDecimal.ZERO)
    } else {
        receiptAmount
    }

fun calculateReceiptReimbursableAmount(
    receiptAmount: BigDecimal,
    expenseType: ExpenseType,
    dailyMealAllowance: BigDecimal,
    alreadySpentForType: BigDecimal = BigDecimal.ZERO,
    alreadySpentForMeals: BigDecimal = BigDecimal.ZERO,
    customTypeLimit: BigDecimal? = null
): BigDecimal {
    val typeLimit = customTypeLimit ?: expenseType.defaultLimit
    val spentAgainstTypeLimit = if (expenseType.isMealAllowance) {
        BigDecimal.ZERO
    } else {
        alreadySpentForType
    }
    if (typeLimit != null) {
        return if (expenseType.category == ExpenseCategory.MEAL) {
            calculateReimbursableAmount(
                receiptAmount = receiptAmount,
                dailyAllowance = typeLimit,
                alreadySpent = spentAgainstTypeLimit
            )
        } else {
            applyExpenseLimit(receiptAmount, typeLimit)
        }
    }
    return if (expenseType.category == ExpenseCategory.MEAL) {
        calculateReimbursableAmount(
            receiptAmount = receiptAmount,
            dailyAllowance = dailyMealAllowance,
            alreadySpent = alreadySpentForMeals
        )
    } else {
        receiptAmount
    }
}

fun calculateDailyMealAllowance(
    baseAllowance: BigDecimal,
    mealTypes: List<ExpenseType>,
    customLimits: Map<ExpenseType, BigDecimal>
): BigDecimal {
    val configuredMealTypes = mealTypes.filter { it.category == ExpenseCategory.MEAL }
    if (configuredMealTypes.isEmpty()) return baseAllowance

    val countedMealTypes = buildList {
        addAll(configuredMealTypes.filterNot { it.isMealAllowance }.distinct())
        // Contrairement aux autres types de repas, chaque occurrence de meal
        // allowance représente un forfait autonome.
        addAll(configuredMealTypes.filter { it.isMealAllowance })
    }

    val limits = countedMealTypes.map { type ->
        effectiveExpenseLimit(type, customLimits)
    }
    // Un type sans plafond propre continue d'utiliser le plafond journalier
    // du déplacement. Sinon la somme des plafonds effectifs décrit exactement
    // le droit applicable aux types de repas présents ce jour-là, y compris
    // lorsqu'il est inférieur au forfait de zone.
    if (limits.any { it == null }) return baseAllowance
    return limits.filterNotNull().fold(BigDecimal.ZERO, BigDecimal::add)
}
