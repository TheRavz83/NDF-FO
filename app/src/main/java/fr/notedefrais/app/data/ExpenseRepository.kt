package fr.notedefrais.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ExpenseRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("expense_data", Context.MODE_PRIVATE)
    private val receiptsDirectory = File(context.filesDir, "receipts").apply { mkdirs() }
    private val originalsDirectory = File(context.filesDir, "receipt_originals").apply { mkdirs() }

    fun load(): ExpenseState {
        val trips = runCatching {
            val array = JSONArray(preferences.getString(KEY_TRIPS, "[]"))
            buildList {
                for (index in 0 until array.length()) add(array.getJSONObject(index).toTrip())
            }
        }.getOrDefault(emptyList())

        val receipts = runCatching {
            val array = JSONArray(preferences.getString(KEY_RECEIPTS, "[]"))
            buildList {
                for (index in 0 until array.length()) {
                    val receipt = array.getJSONObject(index).toReceipt()
                    if (!receipt.hasAttachment || File(receiptsDirectory, receipt.storedFileName).exists()) {
                        add(receipt)
                    }
                }
            }
        }.getOrDefault(emptyList())

        val customExpenseLimits = loadExpenseLimits()
        val loadedState = ExpenseState(
            trips = trips
                .map { trip ->
                    if (trip.status == TripStatus.DRAFT) {
                        trip.copy(
                            dailyMealAllowance = effectiveDailyMealAllowance(
                                trip.mealZone,
                                customExpenseLimits
                            )
                        )
                    } else {
                        trip
                    }
                }
                .sortedByDescending { it.startDate },
            receipts = receipts.sortedWith(compareByDescending<Receipt> { it.date }.thenBy { it.category }),
            customExpenseLimits = customExpenseLimits,
            mealVoucherEmployerContribution = loadMealVoucherEmployerContribution()
        )
        if (preferences.getInt(KEY_LIMIT_POLICY_VERSION, 0) >= LIMIT_POLICY_VERSION) {
            return loadedState
        }
        return runCatching {
            recalculateDraftReceipts(loadedState).also { migrated ->
                persist(migrated)
                preferences.edit()
                    .putInt(KEY_LIMIT_POLICY_VERSION, LIMIT_POLICY_VERSION)
                    .apply()
            }
        }.getOrDefault(loadedState)
    }

    fun saveTrip(trip: Trip, state: ExpenseState): ExpenseState {
        val updated = state.copy(trips = (state.trips + trip).sortedByDescending { it.startDate })
        persist(updated)
        return updated
    }

    fun updateTripTracking(
        tripId: String,
        status: TripStatus,
        submittedDate: LocalDate?,
        acceptedMealAllowanceDates: Set<LocalDate>,
        state: ExpenseState
    ): ExpenseState {
        val preparedState = if (status == TripStatus.SENT) {
            prepareTripForSubmissionInternal(tripId, acceptedMealAllowanceDates, state)
        } else {
            state
        }
        val trip = preparedState.trips.firstOrNull { it.id == tripId }
            ?: error("Le déplacement est introuvable.")
        val updatedTrip = trip.copy(
            status = status,
            submittedDate = if (status == TripStatus.DRAFT) null else submittedDate
        )
        val updated = preparedState.copy(
            trips = preparedState.trips
                .map { if (it.id == tripId) updatedTrip else it }
                .sortedByDescending { it.startDate }
        )
        persist(updated)
        return updated
    }

    fun mealAllowanceSuggestions(
        tripId: String,
        state: ExpenseState
    ): List<SingleMealAllowanceSuggestion> {
        val trip = state.trips.firstOrNull { it.id == tripId }
            ?: error("Le déplacement est introuvable.")
        return findSingleMealAllowanceSuggestions(
            receipts = state.receipts,
            trip = trip,
            customLimits = state.customExpenseLimits,
            mealVoucherEmployerContribution = state.mealVoucherEmployerContribution
        )
    }

    fun prepareTripForSubmission(
        tripId: String,
        acceptedMealAllowanceDates: Set<LocalDate>,
        state: ExpenseState
    ): ExpenseState {
        val updated = prepareTripForSubmissionInternal(
            tripId,
            acceptedMealAllowanceDates,
            state
        )
        persist(updated)
        return updated
    }

    private fun prepareTripForSubmissionInternal(
        tripId: String,
        acceptedMealAllowanceDates: Set<LocalDate>,
        state: ExpenseState
    ): ExpenseState {
        val trip = state.trips.firstOrNull { it.id == tripId }
            ?: error("Le déplacement est introuvable.")
        val convertedReceipts = optimizeMealReceiptsForSubmission(
            receipts = state.receipts,
            trip = trip,
            customLimits = state.customExpenseLimits,
            mealVoucherEmployerContribution = state.mealVoucherEmployerContribution,
            acceptedAllowanceDates = acceptedMealAllowanceDates
        )
        val originalById = state.receipts.associateBy { it.id }
        val convertedById = convertedReceipts.associateBy { it.id }
        val changedDates = (originalById.keys + convertedById.keys)
            .asSequence()
            .mapNotNull { id ->
                val original = originalById[id]
                val converted = convertedById[id]
                if (
                    original?.expenseType != converted?.expenseType ||
                    original?.amount != converted?.amount ||
                    original?.generatedMealAllowance != converted?.generatedMealAllowance
                ) {
                    converted?.date ?: original?.date
                } else {
                    null
                }
            }
            .distinct()
            .sorted()
            .toList()
        var updated = state.copy(receipts = convertedReceipts)
        changedDates.forEach { date ->
            updated = recalculateTripDay(updated, trip, date)
        }
        return updated
    }

    fun updateTripMealZone(
        tripId: String,
        mealZone: MealZone,
        state: ExpenseState
    ): ExpenseState {
        val trip = state.trips.firstOrNull { it.id == tripId }
            ?: error("Le déplacement est introuvable.")
        val updatedTrip = trip.withMealZone(mealZone, state.customExpenseLimits)
        var updated = state.copy(
            trips = state.trips
                .map { if (it.id == tripId) updatedTrip else it }
                .sortedByDescending { it.startDate }
        )
        state.receipts
            .asSequence()
            .filter { it.tripId == tripId }
            .map { it.date }
            .distinct()
            .sorted()
            .forEach { date ->
                updated = prepareMealDay(
                    state = updated,
                    trip = updatedTrip,
                    date = date,
                    changedReceiptId = null,
                    useCombinedMealCalculation = false
                )
                updated = recalculateTripDay(updated, updatedTrip, date)
            }
        persist(updated)
        return updated
    }

    fun deleteTrip(tripId: String, state: ExpenseState): ExpenseState {
        val trip = state.trips.firstOrNull { it.id == tripId }
            ?: error("Le déplacement est introuvable.")
        val tripReceipts = state.receipts.filter { it.tripId == trip.id }
        val updated = state.copy(
            trips = state.trips.filterNot { it.id == trip.id },
            receipts = state.receipts.filterNot { it.tripId == trip.id }
        )
        persist(updated)
        tripReceipts.forEach { receipt ->
            if (receipt.hasAttachment) {
                fileFor(receipt).delete()
                File(originalsDirectory, receipt.storedFileName).delete()
            }
        }
        return updated
    }

    fun saveExpenseLimits(
        limits: Map<ExpenseType, BigDecimal>,
        mealVoucherEmployerContribution: BigDecimal,
        state: ExpenseState
    ): ExpenseState {
        require(limits.values.all { it >= BigDecimal.ZERO })
        require(mealVoucherEmployerContribution >= BigDecimal.ZERO)
        val draftTripIds = state.trips
            .filter { it.status == TripStatus.DRAFT }
            .mapTo(mutableSetOf()) { it.id }
        val updated = recalculateDraftReceipts(
            state.copy(
                trips = state.trips.map { trip ->
                    if (trip.status == TripStatus.DRAFT) {
                        trip.copy(
                            dailyMealAllowance = effectiveDailyMealAllowance(
                                trip.mealZone,
                                limits
                            )
                        )
                    } else {
                        trip
                    }
                },
                receipts = state.receipts.filterNot {
                    it.generatedMealAllowance && it.tripId in draftTripIds
                },
                customExpenseLimits = limits,
                mealVoucherEmployerContribution = mealVoucherEmployerContribution
            )
        )
        persist(updated)
        return updated
    }

    fun importReceipt(
        source: Uri,
        trip: Trip,
        date: LocalDate,
        amount: BigDecimal,
        expenseType: ExpenseType,
        mimeType: String,
        comment: String,
        useCombinedMealCalculation: Boolean,
        state: ExpenseState
    ): ExpenseState {
        val normalizedExpenseType = expenseType.forMealZone(trip.mealZone)
        val category = normalizedExpenseType.category
        require(!date.isBefore(trip.startDate) && !date.isAfter(trip.endDate))
        val extension = extensionFor(mimeType)
        val genericName = buildString {
            append(date.format(FILE_DATE))
            append("_")
            append(category.name.lowercase())
            append("_")
            append(UUID.randomUUID().toString().take(8))
            append(".")
            append(extension)
        }
        val destination = File(receiptsDirectory, genericName)
        val original = File(originalsDirectory, genericName)

        try {
            context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { "Le fichier sélectionné est inaccessible." }
                original.outputStream().use { output -> input.copyTo(output) }
            }
            original.copyTo(destination, overwrite = true)
            val receipt = Receipt(
                tripId = trip.id,
                date = date,
                amount = amount,
                category = category,
                expenseType = normalizedExpenseType,
                storedFileName = genericName,
                mimeType = mimeType.ifBlank { mimeTypeFor(extension) },
                comment = comment.trim(),
                reimbursableAmount = amount
            )
            val withReceipt = state.copy(receipts = state.receipts + receipt)
            val oldDayPrepared = if (receipt.date != date) {
                prepareMealDay(
                    state = withReceipt,
                    trip = trip,
                    date = receipt.date,
                    changedReceiptId = null,
                    useCombinedMealCalculation = false
                )
            } else {
                withReceipt
            }
            val oldDayRecalculated = if (receipt.date != date) {
                recalculateTripDay(oldDayPrepared, trip, receipt.date)
            } else {
                oldDayPrepared
            }
            val prepared = prepareMealDay(
                state = oldDayRecalculated,
                trip = trip,
                date = date,
                changedReceiptId = receipt.id,
                useCombinedMealCalculation = useCombinedMealCalculation
            )
            val updated = recalculateTripDay(prepared, trip, date)
            persist(updated)
            return updated
        } catch (error: Throwable) {
            destination.delete()
            original.delete()
            throw error
        }
    }

    fun addReceiptWithoutAttachment(
        trip: Trip,
        date: LocalDate,
        amount: BigDecimal,
        expenseType: ExpenseType,
        comment: String,
        useCombinedMealCalculation: Boolean,
        state: ExpenseState
    ): ExpenseState {
        require(!date.isBefore(trip.startDate) && !date.isAfter(trip.endDate))
        val normalizedExpenseType = expenseType.forMealZone(trip.mealZone)
        val normalizedAmount = normalizedExpenseAmount(
            requestedAmount = amount,
            expenseType = normalizedExpenseType,
            hasAttachment = false,
            customLimits = state.customExpenseLimits
        )
        require(normalizedAmount > BigDecimal.ZERO)
        val receipt = Receipt(
            tripId = trip.id,
            date = date,
            amount = normalizedAmount,
            category = normalizedExpenseType.category,
            expenseType = normalizedExpenseType,
            storedFileName = "",
            mimeType = "",
            comment = comment.trim(),
            reimbursableAmount = normalizedAmount
        )
        val withReceipt = state.copy(receipts = state.receipts + receipt)
        val prepared = prepareMealDay(
            state = withReceipt,
            trip = trip,
            date = date,
            changedReceiptId = receipt.id,
            useCombinedMealCalculation = useCombinedMealCalculation
        )
        val updated = recalculateTripDay(prepared, trip, date)
        persist(updated)
        return updated
    }

    fun fileFor(receipt: Receipt): File = File(receiptsDirectory, receipt.storedFileName)

    fun updateReceipt(
        receipt: Receipt,
        trip: Trip,
        date: LocalDate,
        amount: BigDecimal,
        expenseType: ExpenseType,
        comment: String,
        useCombinedMealCalculation: Boolean,
        state: ExpenseState
    ): ExpenseState {
        require(!receipt.generatedMealAllowance) {
            "Une ligne Meal allowance automatique ne peut pas être modifiée directement."
        }
        require(state.receipts.any { it.id == receipt.id })
        require(receipt.tripId == trip.id)
        require(!date.isBefore(trip.startDate) && !date.isAfter(trip.endDate))
        val normalizedExpenseType = expenseType.forMealZone(trip.mealZone)
        val normalizedAmount = normalizedExpenseAmount(
            requestedAmount = amount,
            expenseType = normalizedExpenseType,
            hasAttachment = receipt.hasAttachment,
            customLimits = state.customExpenseLimits
        )
        require(normalizedAmount > BigDecimal.ZERO)

        if (receipt.hasAttachment) {
            val original = File(originalsDirectory, receipt.storedFileName)
            require(original.exists()) { "Le justificatif original est introuvable." }
        }

        val updatedReceipt = receipt.copy(
            date = date,
            amount = normalizedAmount,
            category = normalizedExpenseType.category,
            expenseType = normalizedExpenseType,
            comment = comment.trim(),
            reimbursableAmount = normalizedAmount,
            combinedMealCalculation = false
        )

        return try {
            val withReceipt = state.copy(
                receipts = state.receipts.map {
                    if (it.id == receipt.id) updatedReceipt else it
                }
            )
            val oldDayPrepared = if (receipt.date != date) {
                prepareMealDay(
                    state = withReceipt,
                    trip = trip,
                    date = receipt.date,
                    changedReceiptId = null,
                    useCombinedMealCalculation = false
                )
            } else {
                withReceipt
            }
            val oldDayRecalculated = if (receipt.date != date) {
                recalculateTripDay(oldDayPrepared, trip, receipt.date)
            } else {
                oldDayPrepared
            }
            val prepared = prepareMealDay(
                state = oldDayRecalculated,
                trip = trip,
                date = date,
                changedReceiptId = receipt.id,
                useCombinedMealCalculation = useCombinedMealCalculation
            )
            val updated = recalculateTripDay(prepared, trip, date)
            persist(updated)
            updated
        } catch (error: Throwable) {
            throw error
        }
    }

    private fun prepareMealDay(
        state: ExpenseState,
        trip: Trip,
        date: LocalDate,
        changedReceiptId: String?,
        useCombinedMealCalculation: Boolean
    ): ExpenseState {
        val changedReceipt = changedReceiptId?.let { id ->
            state.receipts.firstOrNull { it.id == id }
        }
        val changedType = changedReceipt?.expenseType?.forMealZone(trip.mealZone)
        val sameDay = state.receipts.filter {
            it.tripId == trip.id && it.date == date && !it.generatedMealAllowance
        }

        if (changedType?.isLunchDinner == true) {
            require(sameDay.none {
                it.id != changedReceiptId &&
                    (it.expenseType.isLunch ||
                        it.expenseType.isDinner ||
                        it.expenseType.isMealAllowance)
            }) {
                "Un lunch, un dinner ou un meal allowance existe déjà ce jour-là. " +
                    "Le type lunch + dinner n’est plus disponible."
            }
        }
        if (changedType?.isMealAllowance == true) {
            require(sameDay.none {
                it.id != changedReceiptId && it.expenseType.isLunchDinner
            }) {
                "Un lunch + dinner existe déjà ce jour-là. " +
                    "Il ne peut pas être cumulé avec un meal allowance."
            }
        }

        var normalized = state.receipts
            .filterNot {
                it.tripId == trip.id &&
                    it.date == date &&
                    it.generatedMealAllowance
            }
            .map { current ->
                if (current.tripId != trip.id || current.date != date) {
                    current
                } else {
                    var type = current.expenseType.forMealZone(trip.mealZone)
                    if (
                        current.id != changedReceiptId &&
                        type.isLunchDinner &&
                        changedType != null &&
                        (changedType.isLunch || changedType.isDinner)
                    ) {
                        type = requireNotNull(
                            changedType.oppositeTypeForExistingLunchDinner(trip.mealZone)
                        )
                    }
                    current.copy(
                        expenseType = type,
                        category = type.category
                    )
                }
            }

        val structured = normalized.filter {
            it.tripId == trip.id &&
                it.date == date &&
                (it.expenseType.isLunch || it.expenseType.isDinner)
        }
        val hasLunchAndDinner =
            structured.any { it.expenseType.isLunch } &&
                structured.any { it.expenseType.isDinner }
        val retainCombinedCalculation =
            hasLunchAndDinner && structured.any { it.combinedMealCalculation }
        val hasMealAllowance = normalized.any {
            it.tripId == trip.id &&
                it.date == date &&
                it.expenseType.isMealAllowance
        }
        val applyCombinedCalculation =
            hasLunchAndDinner &&
                !hasMealAllowance &&
                (useCombinedMealCalculation || retainCombinedCalculation)

        if (applyCombinedCalculation) {
            return state.copy(
                receipts = combineSeparateMealReceipts(
                    receipts = normalized,
                    tripId = trip.id,
                    date = date,
                    zone = trip.mealZone
                )
            )
        }

        normalized = normalized.map { current ->
            if (
                current.tripId == trip.id &&
                current.date == date &&
                (current.expenseType.isLunch || current.expenseType.isDinner)
            ) {
                current.copy(combinedMealCalculation = false)
            } else if (
                current.tripId == trip.id &&
                current.date == date &&
                current.combinedMealCalculation
            ) {
                current.copy(combinedMealCalculation = false)
            } else {
                current
            }
        }
        return state.copy(receipts = normalized)
    }

    private fun recalculateTripDay(
        state: ExpenseState,
        trip: Trip,
        date: LocalDate
    ): ExpenseState {
        val recalculatedById = mutableMapOf<String, Receipt>()
        val previousReceipts = mutableListOf<Receipt>()
        val replacements = mutableListOf<Pair<File, File>>()
        try {
            state.receipts
                .filter { it.tripId == trip.id && it.date == date }
                .forEach { receipt ->
                    val calculation = calculateReceiptReimbursement(
                        amount = receipt.amount,
                        expenseType = receipt.expenseType,
                        trip = trip,
                        date = receipt.date,
                        previousReceipts = previousReceipts,
                        customLimits = state.customExpenseLimits,
                        mealVoucherEmployerContribution =
                            state.mealVoucherEmployerContribution,
                        combinedMealCalculation = receipt.combinedMealCalculation
                    )
                    val reimbursableAmount = calculation.reimbursableAmount
                    val recalculated = receipt.copy(reimbursableAmount = reimbursableAmount)
                    recalculatedById[receipt.id] = recalculated
                    previousReceipts += recalculated
                    if (!receipt.hasAttachment) return@forEach

                    val original = File(originalsDirectory, receipt.storedFileName)
                    require(original.exists()) {
                        "Le justificatif original ${receipt.storedFileName} est introuvable."
                    }
                    val replacement = File(
                        receiptsDirectory,
                        ".${receipt.storedFileName}.meal"
                    )
                    if (reimbursableAmount < receipt.amount) {
                        ReceiptAnnotator.annotate(
                            source = original,
                            destination = replacement,
                            mimeType = receipt.mimeType,
                            calculation = calculation
                        )
                    } else {
                        original.copyTo(replacement, overwrite = true)
                    }
                    replacements += replacement to fileFor(receipt)
                }

            replacements.forEach { (replacement, destination) ->
                replacement.copyTo(destination, overwrite = true)
                replacement.delete()
            }
            return state.copy(
                receipts = state.receipts
                    .map { recalculatedById[it.id] ?: it }
                    .sortedWith(compareByDescending<Receipt> { it.date }.thenBy { it.category })
            )
        } catch (error: Throwable) {
            replacements.forEach { (replacement, _) -> replacement.delete() }
            throw error
        }
    }

    private fun recalculateDraftReceipts(state: ExpenseState): ExpenseState {
        val recalculatedById = mutableMapOf<String, Receipt>()
        val replacements = mutableListOf<Pair<File, File>>()
        try {
            state.trips
                .filter { it.status == TripStatus.DRAFT }
                .forEach { trip ->
                    val previousReceipts = mutableListOf<Receipt>()
                    state.receipts
                        .filter { it.tripId == trip.id }
                        .sortedBy { it.date }
                        .forEach { receipt ->
                            val calculation = calculateReceiptReimbursement(
                                amount = receipt.amount,
                                expenseType = receipt.expenseType,
                                trip = trip,
                                date = receipt.date,
                                previousReceipts = previousReceipts,
                                customLimits = state.customExpenseLimits,
                                mealVoucherEmployerContribution =
                                    state.mealVoucherEmployerContribution,
                                combinedMealCalculation = receipt.combinedMealCalculation
                            )
                            val reimbursableAmount = calculation.reimbursableAmount
                            val recalculated = receipt.copy(
                                reimbursableAmount = reimbursableAmount
                            )
                            recalculatedById[receipt.id] = recalculated
                            previousReceipts += recalculated
                            if (!receipt.hasAttachment) return@forEach

                            val original = File(originalsDirectory, receipt.storedFileName)
                            require(original.exists()) {
                                "Le justificatif original ${receipt.storedFileName} est introuvable."
                            }
                            val replacement = File(
                                receiptsDirectory,
                                ".${receipt.storedFileName}.limits"
                            )
                            if (reimbursableAmount < receipt.amount) {
                                ReceiptAnnotator.annotate(
                                    source = original,
                                    destination = replacement,
                                    mimeType = receipt.mimeType,
                                    calculation = calculation
                                )
                            } else {
                                original.copyTo(replacement, overwrite = true)
                            }
                            replacements += replacement to fileFor(receipt)
                        }
                }

            replacements.forEach { (replacement, destination) ->
                replacement.copyTo(destination, overwrite = true)
                replacement.delete()
            }
            return state.copy(
                receipts = state.receipts
                    .map { recalculatedById[it.id] ?: it }
                    .sortedWith(compareByDescending<Receipt> { it.date }.thenBy { it.category })
            )
        } catch (error: Throwable) {
            replacements.forEach { (replacement, _) -> replacement.delete() }
            throw error
        }
    }

    fun deleteReceipt(receipt: Receipt, state: ExpenseState): ExpenseState {
        val trip = state.trips.firstOrNull { it.id == receipt.tripId }
        val withoutReceipt = state.copy(receipts = state.receipts.filterNot { it.id == receipt.id })
        val prepared = if (trip != null) {
            prepareMealDay(
                state = withoutReceipt,
                trip = trip,
                date = receipt.date,
                changedReceiptId = null,
                useCombinedMealCalculation = false
            )
        } else {
            withoutReceipt
        }
        val updated = if (trip != null) {
            recalculateTripDay(prepared, trip, receipt.date)
        } else {
            prepared
        }
        if (receipt.hasAttachment) {
            fileFor(receipt).delete()
            File(originalsDirectory, receipt.storedFileName).delete()
        }
        persist(updated)
        return updated
    }

    private fun persist(state: ExpenseState) {
        val tripsJson = JSONArray().apply {
            state.trips.forEach { put(it.toJson()) }
        }
        val receiptsJson = JSONArray().apply {
            state.receipts.forEach { put(it.toJson()) }
        }
        preferences.edit()
            .putString(KEY_TRIPS, tripsJson.toString())
            .putString(KEY_RECEIPTS, receiptsJson.toString())
            .putString(KEY_EXPENSE_LIMITS, state.customExpenseLimits.toJson().toString())
            .putString(
                KEY_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION,
                state.mealVoucherEmployerContribution.toPlainString()
            )
            .putInt(KEY_LIMIT_POLICY_VERSION, LIMIT_POLICY_VERSION)
            .apply()
    }

    private fun loadExpenseLimits(): Map<ExpenseType, BigDecimal> = runCatching {
        val json = JSONObject(preferences.getString(KEY_EXPENSE_LIMITS, "{}").orEmpty())
        buildMap {
            json.keys().forEach { key ->
                val type = runCatching { ExpenseType.valueOf(key) }.getOrNull()
                val value = runCatching { BigDecimal(json.getString(key)) }.getOrNull()
                if (type != null && value != null && value >= BigDecimal.ZERO) put(type, value)
            }
        }
    }.getOrDefault(emptyMap())

    private fun loadMealVoucherEmployerContribution(): BigDecimal = runCatching {
        BigDecimal(
            preferences.getString(
                KEY_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION,
                DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION.toPlainString()
            )
        ).takeIf { it >= BigDecimal.ZERO }
            ?: DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION
    }.getOrDefault(DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION)

    private fun Map<ExpenseType, BigDecimal>.toJson() = JSONObject().apply {
        forEach { (type, limit) -> put(type.name, limit.toPlainString()) }
    }

    private fun Trip.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("startDate", startDate.toString())
        .put("endDate", endDate.toString())
        .put("mealZone", mealZone.name)
        .put("dailyMealAllowance", dailyMealAllowance.toPlainString())
        .put("status", status.name)
        .put("submittedDate", submittedDate?.toString())

    private fun Receipt.toJson() = JSONObject()
        .put("id", id)
        .put("tripId", tripId)
        .put("date", date.toString())
        .put("amount", amount.toPlainString())
        .put("category", category.name)
        .put("expenseType", expenseType.name)
        .put("storedFileName", storedFileName)
        .put("mimeType", mimeType)
        .put("comment", comment)
        .put("reimbursableAmount", reimbursableAmount.toPlainString())
        .put("combinedMealCalculation", combinedMealCalculation)
        .put("generatedMealAllowance", generatedMealAllowance)

    private fun JSONObject.toTrip(): Trip {
        val status = runCatching {
            TripStatus.valueOf(optString("status", TripStatus.DRAFT.name))
        }.getOrDefault(TripStatus.DRAFT)
        val submittedDate = optString("submittedDate")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val safeStatus = if (status != TripStatus.DRAFT && submittedDate == null) {
            TripStatus.DRAFT
        } else {
            status
        }
        val storedAllowance = BigDecimal(
            optString("dailyMealAllowance", MealZone.PROVINCE.dailyAllowance.toPlainString())
        )
        val mealZone = runCatching {
            MealZone.valueOf(getString("mealZone"))
        }.getOrElse {
            if (storedAllowance >= MealZone.PARIS_SOPHIA.dailyAllowance) {
                MealZone.PARIS_SOPHIA
            } else {
                MealZone.PROVINCE
            }
        }
        return Trip(
            id = getString("id"),
            name = getString("name"),
            startDate = LocalDate.parse(getString("startDate")),
            endDate = LocalDate.parse(getString("endDate")),
            mealZone = mealZone,
            dailyMealAllowance = mealZone.dailyAllowance,
            status = safeStatus,
            submittedDate = if (safeStatus == TripStatus.DRAFT) null else submittedDate
        )
    }

    private fun JSONObject.toReceipt(): Receipt {
        val category = ExpenseCategory.valueOf(getString("category"))
        val expenseType = runCatching {
            ExpenseType.valueOf(getString("expenseType"))
        }.getOrElse {
            ExpenseType.defaultFor(category)
        }
        return Receipt(
            id = getString("id"),
            tripId = getString("tripId"),
            date = LocalDate.parse(getString("date")),
            amount = BigDecimal(getString("amount")),
            category = category,
            expenseType = expenseType,
            storedFileName = getString("storedFileName"),
            mimeType = getString("mimeType"),
            comment = optString("comment", ""),
            reimbursableAmount = BigDecimal(optString("reimbursableAmount", getString("amount"))),
            combinedMealCalculation = optBoolean("combinedMealCalculation", false),
            generatedMealAllowance = optBoolean("generatedMealAllowance", false)
        )
    }

    private fun extensionFor(mimeType: String) = when (mimeType.lowercase()) {
        "application/pdf" -> "pdf"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    private fun mimeTypeFor(extension: String) = when (extension) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    companion object {
        private const val KEY_TRIPS = "trips"
        private const val KEY_RECEIPTS = "receipts"
        private const val KEY_EXPENSE_LIMITS = "expense_limits"
        private const val KEY_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION =
            "meal_voucher_employer_contribution"
        private const val KEY_LIMIT_POLICY_VERSION = "limit_policy_version"
        private const val LIMIT_POLICY_VERSION = 5
        private val FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
