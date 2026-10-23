package fr.notedefrais.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropException
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import fr.notedefrais.app.data.DailySummary
import fr.notedefrais.app.data.DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION
import fr.notedefrais.app.data.ExpenseCategory
import fr.notedefrais.app.data.ExpenseType
import fr.notedefrais.app.data.MealEntry
import fr.notedefrais.app.data.MealZone
import fr.notedefrais.app.data.Receipt
import fr.notedefrais.app.data.ReceiptDisplayGroup
import fr.notedefrais.app.data.SingleMealAllowanceSuggestion
import fr.notedefrais.app.data.Trip
import fr.notedefrais.app.data.TripStatus
import fr.notedefrais.app.data.calculateReceiptReimbursement
import fr.notedefrais.app.data.dinnerType
import fr.notedefrais.app.data.effectiveExpenseLimit
import fr.notedefrais.app.data.forMealZone
import fr.notedefrais.app.data.groupReceiptsForDisplay
import fr.notedefrais.app.data.isCombinedMealCalculationBeneficial
import fr.notedefrais.app.data.isDinner
import fr.notedefrais.app.data.isLunch
import fr.notedefrais.app.data.isLunchDinner
import fr.notedefrais.app.data.isMealAllowance
import fr.notedefrais.app.data.isStructuredMeal
import fr.notedefrais.app.data.lunchDinnerType
import fr.notedefrais.app.data.mealAllowanceType
import fr.notedefrais.app.data.receivesMealVoucherDeduction
import fr.notedefrais.app.export.TripEmailExporter
import fr.notedefrais.app.ocr.OcrAmountCandidate
import fr.notedefrais.app.ocr.OcrDateCandidate
import fr.notedefrais.app.ocr.ReceiptOcr
import fr.notedefrais.app.remote.RemoteAnnouncement
import java.io.File
import fr.notedefrais.app.data.FiscalHorsepower
import fr.notedefrais.app.remote.RouteDistance
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTheme {
                ExpenseApp()
            }
        }
    }
}

private val FoRed = Color(0xFFE30613)
private val FoDarkRed = Color(0xFFA8000A)
private val FoPaleRed = Color(0xFFFDEBED)
private val FoNavy = Color(0xFF1C2A44)
private val FoBackground = Color(0xFFFFF8F7)
private val LimitRed = Color(0xFFB00020)

private val FoColorScheme = lightColorScheme(
    primary = FoRed,
    onPrimary = Color.White,
    primaryContainer = FoPaleRed,
    onPrimaryContainer = FoDarkRed,
    secondary = FoNavy,
    onSecondary = Color.White,
    background = FoBackground,
    onBackground = FoNavy,
    surface = Color.White,
    onSurface = Color(0xFF211A1B),
    surfaceVariant = Color(0xFFF5EDEF),
    onSurfaceVariant = Color(0xFF6C5B5E),
    error = LimitRed
)

@Composable
private fun ExpenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoColorScheme,
        content = content
    )
}

@Composable
private fun FoNotesBrand(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = FoRed,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "FO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Fo Notes",
                color = FoNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ExpenseApp(vm: ExpenseViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var remoteAnnouncement by remember { mutableStateOf<String?>(null) }
    val selectedTrip = state.trips.firstOrNull { it.id == selectedTripId }

    LaunchedEffect(Unit) {
        remoteAnnouncement = RemoteAnnouncement.fetch()
    }

    if (showSettings) {
        ExpenseSettingsScreen(
            limits = state.customExpenseLimits,
            mealVoucherEmployerContribution = state.mealVoucherEmployerContribution,
            onBack = { showSettings = false },
            onSave = vm::saveExpenseLimits
        )
    } else if (selectedTrip == null) {
        TripsScreen(
            trips = state.trips,
            receipts = state.receipts,
            onTripClick = { selectedTripId = it.id },
            onCreateTrip = vm::createTrip,
            onOpenSettings = { showSettings = true },
            onMealAllowanceSuggestions = vm::mealAllowanceSuggestions,
            onPrepareTripForSubmission = vm::prepareTripForSubmission,
            onUpdateTripTracking = vm::updateTripTracking,
            onUpdateTripMealZone = vm::updateTripMealZone,
            onDeleteTrip = vm::deleteTrip,
            fileFor = vm::fileFor
        )
    } else {
        TripScreen(
            trip = selectedTrip,
            receipts = state.receipts.filter { it.tripId == selectedTrip.id },
            summaries = vm.dailySummaries(selectedTrip),
            expenseLimits = state.customExpenseLimits,
            mealVoucherEmployerContribution = state.mealVoucherEmployerContribution,
            onBack = { selectedTripId = null },
            onAddReceipt = vm::addReceipt,
            onAddReceiptWithoutAttachment = vm::addReceiptWithoutAttachment,
            onUpdateReceipt = vm::updateReceipt,
            onDeleteReceipt = vm::deleteReceipt,
            onMealAllowanceSuggestions = vm::mealAllowanceSuggestions,
            onPrepareTripForSubmission = vm::prepareTripForSubmission,
            fileFor = vm::fileFor
        )
    }

    remoteAnnouncement?.let { message ->
        AlertDialog(
            onDismissRequest = { remoteAnnouncement = null },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = FoRed
                )
            },
            title = { Text("Information Fo Notes") },
            text = {
                Text(
                    message,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { remoteAnnouncement = null }) {
                    Text("J’ai compris")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripsScreen(
    trips: List<Trip>,
    receipts: List<Receipt>,
    onTripClick: (Trip) -> Unit,
    onCreateTrip: (String, LocalDate, LocalDate, MealZone) -> Result<Unit>,
    onOpenSettings: () -> Unit,
    onMealAllowanceSuggestions: (String) -> Result<List<SingleMealAllowanceSuggestion>>,
    onPrepareTripForSubmission: (String, Set<LocalDate>) -> Result<List<Receipt>>,
    onUpdateTripTracking: (String, TripStatus, LocalDate?, Set<LocalDate>) -> Result<Unit>,
    onUpdateTripMealZone: (String, MealZone) -> Result<Unit>,
    onDeleteTrip: (String) -> Result<Unit>,
    fileFor: (Receipt) -> File
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToTrack by remember { mutableStateOf<Trip?>(null) }
    var tripToRelocate by remember { mutableStateOf<Trip?>(null) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var pendingExport by remember {
        mutableStateOf<Pair<Trip, List<SingleMealAllowanceSuggestion>>?>(null)
    }
    var pendingTracking by remember {
        mutableStateOf<PendingTrackingUpdate?>(null)
    }

    fun exportTrip(trip: Trip, acceptedDates: Set<LocalDate>) {
        onPrepareTripForSubmission(trip.id, acceptedDates)
            .onSuccess { preparedReceipts ->
                exportTripByEmail(context, trip, preparedReceipts, fileFor)
            }
            .onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Préparation de l’export impossible",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun requestExport(trip: Trip) {
        onMealAllowanceSuggestions(trip.id)
            .onSuccess { suggestions ->
                if (suggestions.isEmpty()) exportTrip(trip, emptySet())
                else pendingExport = trip to suggestions
            }
            .onFailure { error ->
                Toast.makeText(context, error.message ?: "Calcul impossible", Toast.LENGTH_LONG).show()
            }
    }

    fun updateTracking(request: PendingTrackingUpdate, acceptedDates: Set<LocalDate>) {
        onUpdateTripTracking(
            request.trip.id,
            request.status,
            request.submittedDate,
            acceptedDates
        ).onSuccess {
            pendingTracking = null
            Toast.makeText(context, "Suivi mis à jour", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(
                context,
                it.message ?: "Mise à jour impossible",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        containerColor = FoBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    FoNotesBrand(subtitle = "Déplacements")
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramétrage")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouveau déplacement") }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyTrips(
                modifier = Modifier.padding(padding),
                onCreate = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Vos justificatifs, rangés par mission.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(trips, key = { it.id }) { trip ->
                    val tripReceipts = receipts.filter { it.tripId == trip.id }
                    TripCard(
                        trip = trip,
                        receipts = tripReceipts,
                        onClick = { onTripClick(trip) },
                        onTracking = { tripToTrack = trip },
                        onLocation = { tripToRelocate = trip },
                        onDelete = { tripToDelete = trip },
                        onExport = { requestExport(trip) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTripDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, start, end, mealZone ->
                onCreateTrip(name, start, end, mealZone).onSuccess { showCreateDialog = false }
            }
        )
    }

    tripToTrack?.let { trip ->
        TripTrackingDialog(
            trip = trip,
            onDismiss = { tripToTrack = null },
            onConfirm = { status, submittedDate ->
                val request = PendingTrackingUpdate(trip, status, submittedDate)
                if (status != TripStatus.SENT) {
                    tripToTrack = null
                    updateTracking(request, emptySet())
                } else {
                    onMealAllowanceSuggestions(trip.id)
                        .onSuccess { suggestions ->
                            tripToTrack = null
                            if (suggestions.isEmpty()) {
                                updateTracking(request, emptySet())
                            } else {
                                pendingTracking = request.copy(suggestions = suggestions)
                            }
                        }
                        .onFailure {
                            Toast.makeText(
                                context,
                                it.message ?: "Calcul impossible",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        )
    }

    pendingExport?.let { (trip, suggestions) ->
        MealAllowanceChoiceDialog(
            suggestions = suggestions,
            onDismiss = { pendingExport = null },
            onAccept = { acceptedDates ->
                pendingExport = null
                exportTrip(trip, acceptedDates)
            },
            onDecline = {
                pendingExport = null
                exportTrip(trip, emptySet())
            }
        )
    }

    pendingTracking?.let { request ->
        MealAllowanceChoiceDialog(
            suggestions = request.suggestions,
            onDismiss = { pendingTracking = null },
            onAccept = { acceptedDates -> updateTracking(request, acceptedDates) },
            onDecline = { updateTracking(request, emptySet()) }
        )
    }

    tripToRelocate?.let { trip ->
        TripMealZoneDialog(
            trip = trip,
            onDismiss = { tripToRelocate = null },
            onConfirm = { mealZone ->
                onUpdateTripMealZone(trip.id, mealZone)
                    .onSuccess {
                        tripToRelocate = null
                        Toast.makeText(
                            context,
                            "Zone et plafonds mis à jour",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .onFailure {
                        Toast.makeText(
                            context,
                            it.message ?: "Modification impossible",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        )
    }

    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Supprimer cette note de frais ?") },
            text = {
                Text(
                    "Le déplacement « ${trip.name} » et tous ses justificatifs seront " +
                        "supprimés définitivement de l’application."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTrip(trip.id)
                            .onSuccess {
                                tripToDelete = null
                                Toast.makeText(
                                    context,
                                    "Note de frais supprimée",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Suppression impossible",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSettingsScreen(
    limits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    onBack: () -> Unit,
    onSave: (Map<ExpenseType, BigDecimal>, BigDecimal) -> Result<Unit>
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(ExpenseCategory.TRANSPORT) }
    var values by remember(limits) {
        mutableStateOf(
            limits.mapValues { (_, amount) -> amount.toFrenchAmount() }
        )
    }
    var employerContribution by remember(mealVoucherEmployerContribution) {
        mutableStateOf(mealVoucherEmployerContribution.toFrenchAmount())
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        val parsed = buildMap {
            values.forEach { (type, text) ->
                if (text.isNotBlank()) {
                    val amount = text.toMoneyOrNull()
                    if (amount == null || amount.signum() < 0) {
                        error = "Le plafond « ${type.displayLabel} » est invalide."
                        return
                    }
                    put(type, amount)
                }
            }
        }
        val parsedEmployerContribution = if (employerContribution.isBlank()) {
            DEFAULT_MEAL_VOUCHER_EMPLOYER_CONTRIBUTION
        } else {
            employerContribution.toMoneyOrNull()
        }
        if (
            parsedEmployerContribution == null ||
            parsedEmployerContribution.signum() < 0
        ) {
            error = "La participation employeur est invalide."
            return
        }
        error = null
        onSave(parsed, parsedEmployerContribution)
            .onSuccess {
                Toast.makeText(context, "Paramétrage enregistré", Toast.LENGTH_SHORT).show()
                onBack()
            }
            .onFailure {
                error = it.message ?: "Enregistrement impossible."
            }
    }

    Scaffold(
        containerColor = FoBackground,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                title = {
                    FoNotesBrand(subtitle = "Plafonds de dépenses")
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text("Enregistrer")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Laissez un champ vide pour conserver la règle par défaut. Les plafonds Paris, province et étranger se règlent séparément.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Ticket restaurant",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = employerContribution,
                            onValueChange = {
                                employerContribution = it
                                error = null
                            },
                            label = { Text("Participation employeur (€)") },
                            supportingText = {
                                Text(
                                    "Déduite du premier lunch ou du premier " +
                                        "lunch + dinner de chaque journée. " +
                                        "Saisissez 0 pour la désactiver. Par défaut : 6,00 €."
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                ExpenseCategoryDropdown(
                    selected = category,
                    onSelected = { category = it }
                )
            }
            error?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
            items(
                items = ExpenseType.forCategory(category),
                key = { it.name }
            ) { type ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(type.displayLabel, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = values[type].orEmpty(),
                            onValueChange = { value ->
                                values = values.toMutableMap().apply {
                                    if (value.isBlank()) remove(type) else put(type, value)
                                }
                                error = null
                            },
                            label = { Text("Plafond personnalisé (€)") },
                            placeholder = { Text("Valeur par défaut") },
                            supportingText = {
                                Text(
                                    if (type.isMealAllowance) {
                                        "Par défaut : ${type.defaultLimit!!.euros()}. " +
                                            "Comparé automatiquement aux repas lors de l’envoi."
                                    } else if (type.defaultLimit != null) {
                                        "Par défaut : ${type.defaultLimit!!.euros()}"
                                    } else if (type.category == ExpenseCategory.MEAL) {
                                        "Par défaut : plafond repas quotidien du déplacement"
                                    } else {
                                        "Par défaut : aucun plafond spécifique"
                                    }
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) {
                    Text("Enregistrer les plafonds")
                }
            }
        }
    }
}

@Composable
private fun EmptyTrips(modifier: Modifier = Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(color = FoPaleRed, shape = CircleShape, modifier = Modifier.size(88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ReceiptLong, null, tint = FoRed, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Aucun déplacement", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Créez une mission pour commencer à classer vos factures et suivre vos plafonds.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCreate) { Text("Créer mon premier déplacement") }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    receipts: List<Receipt>,
    onClick: () -> Unit,
    onTracking: () -> Unit,
    onLocation: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    val total = receipts.fold(BigDecimal.ZERO) { sum, receipt -> sum + receipt.amount }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = FoPaleRed, shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        Icons.Default.DirectionsCar, null, tint = FoRed,
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(trip.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(
                        trip.dateRangeLabel(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                Text(total.euros(), color = FoRed, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = tripStatusColor(trip.status),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        trip.status.label,
                        color = tripStatusTextColor(trip.status),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                trip.submittedDate?.let { submittedDate ->
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Soumise le ${submittedDate.shortDateLabel()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE9ECE7))
            Spacer(Modifier.height(12.dp))
            Row {
                val attachmentCount = receipts.count { it.hasAttachment }
                Text(
                    "$attachmentCount justificatif${if (attachmentCount > 1) "s" else ""}",
                    fontSize = 13.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${trip.mealZone.label} • ${trip.dailyMealAllowance.euros()} / jour",
                    color = FoRed,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTracking, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Suivi")
                }
                OutlinedButton(
                    onClick = onExport,
                    enabled = receipts.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("E-mail")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onLocation, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Changer la zone")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer la note de frais",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripScreen(
    trip: Trip,
    receipts: List<Receipt>,
    summaries: List<DailySummary>,
    expenseLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    onBack: () -> Unit,
    onAddReceipt: (Uri, Trip, LocalDate, BigDecimal, ExpenseType, String, String, Boolean) -> Result<Unit>,
    onAddReceiptWithoutAttachment: (Trip, LocalDate, BigDecimal, ExpenseType, String, Boolean) -> Result<Unit>,
    onUpdateReceipt: (Receipt, Trip, LocalDate, BigDecimal, ExpenseType, String, Boolean) -> Result<Unit>,
    onDeleteReceipt: (Receipt) -> Unit,
    onMealAllowanceSuggestions: (String) -> Result<List<SingleMealAllowanceSuggestion>>,
    onPrepareTripForSubmission: (String, Set<LocalDate>) -> Result<List<Receipt>>,
    fileFor: (Receipt) -> File
) {
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<PendingSource?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var receiptToEdit by remember { mutableStateOf<Receipt?>(null) }
    var receiptToDelete by remember { mutableStateOf<Receipt?>(null) }
    var pendingExportSuggestions by remember {
        mutableStateOf<List<SingleMealAllowanceSuggestion>?>(null)
    }

    fun exportTrip(acceptedDates: Set<LocalDate>) {
        onPrepareTripForSubmission(trip.id, acceptedDates)
            .onSuccess { preparedReceipts ->
                exportTripByEmail(context, trip, preparedReceipts, fileFor)
            }
            .onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Préparation de l’export impossible",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun requestExport() {
        onMealAllowanceSuggestions(trip.id)
            .onSuccess { suggestions ->
                if (suggestions.isEmpty()) exportTrip(emptySet())
                else pendingExportSuggestions = suggestions
            }
            .onFailure { error ->
                Toast.makeText(context, error.message ?: "Calcul impossible", Toast.LENGTH_LONG).show()
            }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            pendingSource = PendingSource(uri, mime.ifBlank { "image/jpeg" })
        }
    }
    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        result.uriContent?.let { croppedUri ->
            pendingSource = PendingSource(croppedUri, "image/jpeg")
        }
        if (result.error != null &&
            result.error !is CropException.Cancellation &&
            result.uriContent == null
        ) {
            Toast.makeText(
                context,
                "Le recadrage n’a pas pu être effectué.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { sourceUri ->
                val directory = File(context.cacheDir, "camera").apply { mkdirs() }
                val croppedFile = File(
                    directory,
                    "cropped_${System.currentTimeMillis()}.jpg"
                )
                val croppedUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    croppedFile
                )
                cropLauncher.launch(
                    CropImageContractOptions(
                        uri = sourceUri,
                        cropImageOptions = CropImageOptions(
                            imageSourceIncludeCamera = false,
                            imageSourceIncludeGallery = false,
                            cropShape = CropImageView.CropShape.RECTANGLE,
                            guidelines = CropImageView.Guidelines.ON,
                            fixAspectRatio = false,
                            activityTitle = "Recadrer la facture",
                            cropMenuCropButtonTitle = "Valider",
                            activityMenuIconColor = android.graphics.Color.WHITE,
                            activityMenuTextColor = android.graphics.Color.WHITE,
                            customOutputUri = croppedUri,
                            outputCompressFormat = Bitmap.CompressFormat.JPEG,
                            outputCompressQuality = 95
                        )
                    )
                )
            }
        }
    }

    fun startCamera() {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        containerColor = FoBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(trip.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(trip.dateRangeLabel(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { requestExport() },
                        enabled = receipts.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Exporter par e-mail")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSourceDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une dépense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TripOverview(summaries, receipts)
            }
            items(summaries, key = { it.date.toString() }) { summary ->
                DayCard(
                    summary = summary,
                    receipts = receipts.filter { it.date == summary.date },
                    onOpen = { receipt ->
                        openReceipt(context, receipt, fileFor(receipt))
                    },
                    onEdit = { receiptToEdit = it },
                    onDelete = { receiptToDelete = it }
                )
            }
        }
    }

    pendingExportSuggestions?.let { suggestions ->
        MealAllowanceChoiceDialog(
            suggestions = suggestions,
            onDismiss = { pendingExportSuggestions = null },
            onAccept = { acceptedDates ->
                pendingExportSuggestions = null
                exportTrip(acceptedDates)
            },
            onDecline = {
                pendingExportSuggestions = null
                exportTrip(emptySet())
            }
        )
    }

    if (showSourceDialog) {
        SourceDialog(
            onDismiss = { showSourceDialog = false },
            onCamera = {
                showSourceDialog = false
                startCamera()
            },
            onFile = {
                showSourceDialog = false
                documentLauncher.launch(arrayOf("application/pdf", "image/*"))
            },
            onManual = {
                showSourceDialog = false
                pendingSource = PendingSource(uri = null, mimeType = "")
            }
        )
    }

    pendingSource?.let { source ->
        AddReceiptDialog(
            trip = trip,
            source = source,
            receipts = receipts,
            expenseLimits = expenseLimits,
            mealVoucherEmployerContribution = mealVoucherEmployerContribution,
            onDismiss = { pendingSource = null },
            onConfirm = { date, amount, expenseType, comment, useCombinedCalculation ->
                val result = source.uri?.let { uri ->
                    onAddReceipt(
                        uri,
                        trip,
                        date,
                        amount,
                        expenseType,
                        source.mimeType,
                        comment,
                        useCombinedCalculation
                    )
                } ?: onAddReceiptWithoutAttachment(
                    trip,
                    date,
                    amount,
                    expenseType,
                    comment,
                    useCombinedCalculation
                )
                result
                    .onSuccess {
                        pendingSource = null
                        Toast.makeText(context, "Justificatif enregistré", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "Enregistrement impossible", Toast.LENGTH_LONG).show()
                    }
            }
        )
    }

    receiptToEdit?.let { receipt ->
        EditReceiptDialog(
            trip = trip,
            receipt = receipt,
            receipts = receipts,
            expenseLimits = expenseLimits,
            mealVoucherEmployerContribution = mealVoucherEmployerContribution,
            onDismiss = { receiptToEdit = null },
            onConfirm = { date, amount, expenseType, comment, useCombinedCalculation ->
                onUpdateReceipt(
                    receipt,
                    trip,
                    date,
                    amount,
                    expenseType,
                    comment,
                    useCombinedCalculation
                )
                    .onSuccess {
                        receiptToEdit = null
                        Toast.makeText(context, "Justificatif modifié", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(
                            context,
                            it.message ?: "Modification impossible",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        )
    }

    receiptToDelete?.let { receipt ->
        AlertDialog(
            onDismissRequest = { receiptToDelete = null },
            title = { Text("Supprimer ce justificatif ?") },
            text = { Text("Le fichier sera supprimé définitivement de l’application.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteReceipt(receipt)
                    receiptToDelete = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { receiptToDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun TripOverview(summaries: List<DailySummary>, receipts: List<Receipt>) {
    val total = receipts.fold(BigDecimal.ZERO) { sum, receipt -> sum + receipt.amount }
    val meals = summaries.fold(BigDecimal.ZERO) { sum, day -> sum + day.mealSpent }
    val allowance = summaries.fold(BigDecimal.ZERO) { sum, day -> sum + day.allowance }
    Card(
        colors = CardDefaults.cardColors(containerColor = FoNavy),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("VUE D’ENSEMBLE", color = Color.White.copy(alpha = .72f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(total.euros(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Total de tous les frais", color = Color.White.copy(alpha = .75f))
            Spacer(Modifier.height(18.dp))
            Row {
                OverviewStat("Repas", meals.euros(), Modifier.weight(1f))
                OverviewStat("Droits repas", allowance.euros(), Modifier.weight(1f))
                OverviewStat(
                    "Pièces",
                    receipts.count { it.hasAttachment }.toString(),
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayCard(
    summary: DailySummary,
    receipts: List<Receipt>,
    onOpen: (Receipt) -> Unit,
    onEdit: (Receipt) -> Unit,
    onDelete: (Receipt) -> Unit
) {
    val over = summary.remaining.signum() < 0
    val receiptGroups = groupReceiptsForDisplay(receipts)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(summary.date.fullDateLabel(), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(
                        if (over) "Plafond dépassé de ${summary.remaining.abs().euros()}"
                        else "${summary.remaining.euros()} disponibles pour les repas",
                        color = if (over) LimitRed else FoRed,
                        fontSize = 13.sp
                    )
                }
                Text(
                    "${summary.mealSpent.euros()} / ${summary.allowance.euros()}",
                    fontWeight = FontWeight.SemiBold,
                    color = if (over) LimitRed else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { summary.ratio },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (over) LimitRed else FoRed,
                trackColor = FoPaleRed
            )
            if (receipts.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Aucun justificatif ce jour", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            } else {
                Spacer(Modifier.height(12.dp))
                receiptGroups.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider(color = Color(0xFFF0F1EE))
                    if (group.receipts.size == 1) {
                        val receipt = group.primaryReceipt
                        ReceiptRow(
                            receipt = receipt,
                            onOpen = { onOpen(receipt) },
                            onEdit = { onEdit(receipt) },
                            onDelete = { onDelete(receipt) }
                        )
                    } else {
                        CumulatedReceiptRow(
                            group = group,
                            onOpen = onOpen,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CumulatedReceiptRow(
    group: ReceiptDisplayGroup,
    onOpen: (Receipt) -> Unit,
    onEdit: (Receipt) -> Unit,
    onDelete: (Receipt) -> Unit
) {
    val primaryReceipt = group.primaryReceipt
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = categoryColor(primaryReceipt.category),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    categoryIcon(primaryReceipt.category),
                    contentDescription = null,
                    tint = FoRed,
                    modifier = Modifier.padding(9.dp).size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(primaryReceipt.expenseType.displayLabel, fontWeight = FontWeight.Medium)
                Text(
                    "${group.receipts.size} dépenses regroupées",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    group.reimbursableAmount.euros(),
                    fontWeight = FontWeight.SemiBold,
                    color = if (group.isCapped) LimitRed else MaterialTheme.colorScheme.onSurface
                )
                if (group.isCapped) {
                    Text(
                        "sur ${group.declaredAmount.euros()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        group.receipts.forEachIndexed { index, receipt ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 45.dp),
                    color = Color(0xFFF0F1EE)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = receipt.hasAttachment) { onOpen(receipt) }
                    .padding(start = 45.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (receipt.hasAttachment) Icons.Default.InsertDriveFile else Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        receipt.comment.ifBlank { "Justificatif ${index + 1}" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        when {
                            receipt.hasAttachment -> receipt.storedFileName
                            receipt.generatedMealAllowance -> "Ajout automatique • sans justificatif"
                            else -> "Saisie manuelle • sans justificatif"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    receipt.amount.euros(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!receipt.generatedMealAllowance) {
                    IconButton(onClick = { onEdit(receipt) }) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Modifier cette dépense",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(receipt) }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer cette dépense",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    receipt: Receipt,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = receipt.hasAttachment, onClick = onOpen)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = categoryColor(receipt.category),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.clickable(
                enabled = !receipt.generatedMealAllowance,
                onClick = onEdit
            )
        ) {
            Icon(
                categoryIcon(receipt.category),
                contentDescription = "Modifier ce justificatif",
                tint = FoRed,
                modifier = Modifier.padding(9.dp).size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(receipt.expenseType.displayLabel, fontWeight = FontWeight.Medium)
            if (receipt.comment.isNotBlank()) {
                Text(
                    receipt.comment,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                when {
                    receipt.hasAttachment -> receipt.storedFileName
                    receipt.generatedMealAllowance -> "Ajout automatique • sans justificatif"
                    else -> "Saisie manuelle • sans justificatif"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                receipt.reimbursableAmount.euros(),
                fontWeight = FontWeight.SemiBold,
                color = if (receipt.isCapped) LimitRed else MaterialTheme.colorScheme.onSurface
            )
            if (receipt.isCapped) {
                Text(
                    "sur ${receipt.amount.euros()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!receipt.generatedMealAllowance) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalDate, MealZone) -> Unit
) {
    val today = LocalDate.now()
    var name by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today) }
    var end by remember { mutableStateOf(today) }
    var mealZone by remember { mutableStateOf(MealZone.PROVINCE) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau déplacement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nom (ex. Les Clayes)") }, singleLine = true
                )
                CalendarDateField(
                    label = "Date de début",
                    value = start,
                    onValueChange = {
                        start = it
                        if (end.isBefore(it)) end = it
                    }
                )
                CalendarDateField(
                    label = "Date de fin",
                    value = end,
                    onValueChange = { end = it },
                    minDate = start
                )
                MealZoneDropdown(
                    selected = mealZone,
                    onSelected = { mealZone = it }
                )
                Text(
                    "Plafond repas calculé automatiquement : ${mealZone.dailyAllowance.euros()} par jour",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    name.isBlank() -> "Donnez un nom au déplacement."
                    end.isBefore(start) -> "La fin doit être après le début."
                    else -> null
                }
                if (error == null) onConfirm(name, start, end, mealZone)
            }) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealZoneDropdown(
    selected: MealZone,
    onSelected: (MealZone) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Zone des repas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MealZone.entries.forEach { zone ->
                DropdownMenuItem(
                    text = {
                        Text("${zone.label} — ${zone.dailyAllowance.euros()} / jour")
                    },
                    onClick = {
                        onSelected(zone)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripMealZoneDialog(
    trip: Trip,
    onDismiss: () -> Unit,
    onConfirm: (MealZone) -> Unit
) {
    var mealZone by remember(trip.id) { mutableStateOf(trip.mealZone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Localisation du déplacement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    trip.name,
                    fontWeight = FontWeight.SemiBold,
                    color = FoNavy
                )
                MealZoneDropdown(
                    selected = mealZone,
                    onSelected = { mealZone = it }
                )
                Text(
                    "Le plafond repas passera à ${mealZone.dailyAllowance.euros()} par jour. " +
                        "Les types de dîner, les montants remboursables et les annotations " +
                        "seront recalculés.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mealZone) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripTrackingDialog(
    trip: Trip,
    onDismiss: () -> Unit,
    onConfirm: (TripStatus, LocalDate?) -> Unit
) {
    val today = LocalDate.now()
    var status by remember(trip.id) { mutableStateOf(trip.status) }
    var submittedDate by remember(trip.id) { mutableStateOf(trip.submittedDate) }
    var error by remember(trip.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suivi de la note de frais") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    trip.name,
                    fontWeight = FontWeight.SemiBold,
                    color = FoNavy
                )
                TripStatusDropdown(
                    selected = status,
                    onSelected = { selected ->
                        status = selected
                        submittedDate = if (selected == TripStatus.DRAFT) {
                            null
                        } else {
                            submittedDate ?: today
                        }
                        error = null
                    }
                )
                if (status != TripStatus.DRAFT) {
                    CalendarDateField(
                        label = "Date de soumission",
                        value = submittedDate ?: today,
                        onValueChange = {
                            submittedDate = it
                            error = null
                        },
                        minDate = trip.startDate,
                        maxDate = today,
                        supportingText = "Date d’envoi de la note de frais"
                    )
                } else {
                    Text(
                        "La date de soumission sera demandée lorsque la note sera envoyée.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    status != TripStatus.DRAFT && submittedDate == null ->
                        "Choisissez la date de soumission."
                    submittedDate?.isAfter(today) == true ->
                        "La date de soumission ne peut pas être dans le futur."
                    submittedDate?.isBefore(trip.startDate) == true ->
                        "La date de soumission ne peut pas précéder le déplacement."
                    else -> null
                }
                if (error == null) onConfirm(status, submittedDate)
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripStatusDropdown(
    selected: TripStatus,
    onSelected: (TripStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Statut") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TripStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onFile: () -> Unit,
    onManual: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ReceiptLong, null) },
        title = { Text("Ajouter une dépense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Prendre une photo")
                }
                OutlinedButton(onClick = onFile, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Importer PDF ou image")
                }
                OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Saisie sans justificatif")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun RouteAddressField(
    value: String,
    place: fr.notedefrais.app.remote.RoutePlace?,
    label: String,
    enabled: Boolean,
    onChange: (String, fr.notedefrais.app.remote.RoutePlace?) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<fr.notedefrais.app.remote.RoutePlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(value, place, focused, enabled) {
        suggestions = emptyList()
        searchError = null
        searching = false
        if (!focused || !enabled || place != null || value.trim().length < 3) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        searching = true
        try {
            suggestions = RouteDistance.searchPlaces(value)
            if (suggestions.isEmpty()) searchError = "Aucune adresse trouvée."
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            searchError = "Recherche indisponible. Réessayez ou saisissez une adresse complète."
        } finally {
            searching = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it, null) },
            label = { Text(label) },
            enabled = enabled,
            singleLine = true,
            trailingIcon = if (searching) {
                { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
            } else null,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
        )
        if (focused && enabled && place == null) {
            suggestions.forEach { suggestion ->
                TextButton(
                    onClick = {
                        suggestions = emptyList()
                        onChange(suggestion.label, suggestion)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(suggestion.label, modifier = Modifier.fillMaxWidth())
                }
            }
            searchError?.let { Text(it, fontSize = 12.sp) }
            if (suggestions.isNotEmpty()) {
                Text("Photon · © OpenStreetMap contributors", fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MileageTripSection(onResult: (String?, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var outFrom by remember { mutableStateOf("") }
    var outTo by remember { mutableStateOf("") }
    var backFrom by remember { mutableStateOf("") }
    var backTo by remember { mutableStateOf("") }
    var outFromPlace by remember { mutableStateOf<fr.notedefrais.app.remote.RoutePlace?>(null) }
    var outToPlace by remember { mutableStateOf<fr.notedefrais.app.remote.RoutePlace?>(null) }
    var backFromPlace by remember { mutableStateOf<fr.notedefrais.app.remote.RoutePlace?>(null) }
    var backToPlace by remember { mutableStateOf<fr.notedefrais.app.remote.RoutePlace?>(null) }
    var backEdited by remember { mutableStateOf(false) }
    var horsepower by remember { mutableStateOf(FiscalHorsepower.THREE_OR_LESS) }
    var horsepowerExpanded by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf<Pair<BigDecimal, BigDecimal>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun publish(outward: BigDecimal, inward: BigDecimal, power: FiscalHorsepower) {
        val total = outward + inward
        val reimbursement = power.amountFor(total).toFrenchAmount()
        val rate = power.ratePerKm.toPlainString().replace('.', ',')
        onResult(
            reimbursement,
            "Distance aller : ${outward.toFrenchAmount()} Km\n" +
                "Distance retour : ${inward.toFrenchAmount()} Km\n" +
                "Distance totale : ${total.toFrenchAmount()} Km\n" +
                "Puissance fiscale : ${power.label}\n" +
                "Calcul : ${total.toFrenchAmount()} Km x $rate €/km = $reimbursement €"
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Trajet kilométrique", fontWeight = FontWeight.SemiBold, color = FoRed)
        Text("Aller", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RouteAddressField(
            value = outFrom,
            place = outFromPlace,
            enabled = !loading,
            onChange = { text, selected ->
                outFrom = text
                outFromPlace = selected
                route = null
                if (!backEdited) {
                    backTo = text
                    backToPlace = selected
                }
            },
            label = "Départ aller (ex. Atos Aix)"
        )
        RouteAddressField(
            value = outTo,
            place = outToPlace,
            enabled = !loading,
            onChange = { text, selected ->
                outTo = text
                outToPlace = selected
                route = null
                if (!backEdited) {
                    backFrom = text
                    backFromPlace = selected
                }
            },
            label = "Arrivée aller (ex. Gare TGV Aix)"
        )
        Text("Retour", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RouteAddressField(
            value = backFrom,
            place = backFromPlace,
            enabled = !loading,
            onChange = { text, selected ->
                backFrom = text
                backFromPlace = selected
                backEdited = true
                route = null
            },
            label = "Départ retour (ex. Gare TGV Aix)"
        )
        RouteAddressField(
            value = backTo,
            place = backToPlace,
            enabled = !loading,
            onChange = { text, selected ->
                backTo = text
                backToPlace = selected
                backEdited = true
                route = null
            },
            label = "Arrivée retour (ex. Atos Aix)"
        )
        ExposedDropdownMenuBox(
            expanded = horsepowerExpanded,
            onExpandedChange = { horsepowerExpanded = !horsepowerExpanded }
        ) {
            OutlinedTextField(
                value = horsepower.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Puissance fiscale") },
                supportingText = {
                    Text(
                        "Barème 2026 jusqu'à 5 000 km : " +
                            "${horsepower.ratePerKm.toPlainString().replace('.', ',')} €/km"
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = horsepowerExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = horsepowerExpanded,
                onDismissRequest = { horsepowerExpanded = false }
            ) {
                FiscalHorsepower.entries.forEach { power ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${power.label} — " +
                                    "${power.ratePerKm.toPlainString().replace('.', ',')} €/km"
                            )
                        },
                        onClick = {
                            horsepower = power
                            horsepowerExpanded = false
                            route?.let { (outward, inward) ->
                                publish(outward, inward, power)
                            }
                        }
                    )
                }
            }
        }
        OutlinedButton(
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val places = listOf(outFrom, outTo, backFrom, backTo).map { it.trim() }
                if (places.any { it.isEmpty() }) {
                    error = "Renseignez les quatre champs du trajet."
                    return@OutlinedButton
                }
                error = null
                route = null
                loading = true
                scope.launch {
                    runCatching {
                        val outward = RouteDistance.kilometers(
                            places[0], places[1], outFromPlace, outToPlace
                        )
                        val inward = RouteDistance.kilometers(
                            places[2], places[3], backFromPlace, backToPlace
                        )
                        outward to inward
                    }.onSuccess { (outward, inward) ->
                        route = outward to inward
                        publish(outward, inward, horsepower)
                    }.onFailure {
                        error = it.message ?: "Calcul impossible. Vérifiez la connexion."
                    }
                    loading = false
                }
            }
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (loading) "Calcul en cours…" else "Calculer les kilomètres")
        }
        route?.let { (outward, inward) ->
            Text(
                "Aller ${outward.toFrenchAmount()} km + retour ${inward.toFrenchAmount()} km = " +
                    "${(outward + inward).toFrenchAmount()} km",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
    }
}

@Composable
private fun AddReceiptDialog(
    trip: Trip,
    source: PendingSource,
    receipts: List<Receipt>,
    expenseLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, BigDecimal, ExpenseType, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val defaultDate = LocalDate.now().coerceIn(trip.startDate, trip.endDate)
    var date by remember { mutableStateOf(defaultDate) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.MEAL) }
    var expenseType by remember { mutableStateOf(ExpenseType.LUNCH) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var combinedAdvice by remember {
        mutableStateOf<Triple<LocalDate, BigDecimal, ExpenseType>?>(null)
    }
    var allowanceAdvice by remember {
        mutableStateOf<MealAllowanceReplacementAdvice?>(null)
    }
    var ocrLoading by remember(source.uri) { mutableStateOf(source.uri != null) }
    var ocrCompleted by remember(source.uri) { mutableStateOf(source.uri == null) }
    var ocrCandidates by remember(source.uri) {
        mutableStateOf<List<OcrAmountCandidate>>(emptyList())
    }
    var ocrDateCandidates by remember(source.uri) {
        mutableStateOf<List<OcrDateCandidate>>(emptyList())
    }
    val fixedAllowanceWithoutAttachment = source.uri == null && expenseType.isMealAllowance

    LaunchedEffect(source.uri, expenseType, expenseLimits) {
        if (fixedAllowanceWithoutAttachment) {
            effectiveExpenseLimit(expenseType, expenseLimits)?.let { allowance ->
                amount = allowance.toFrenchAmount()
            }
        }
    }

    LaunchedEffect(date, category, receipts) {
        if (category == ExpenseCategory.MEAL) {
            val existingMeals = receipts.filter {
                it.tripId == trip.id && it.date == date
            }
            if (
                expenseType.isLunchDinner &&
                existingMeals.any {
                    it.expenseType.isLunch ||
                        it.expenseType.isDinner ||
                        it.expenseType.isMealAllowance
                }
            ) {
                expenseType = if (existingMeals.any { it.expenseType.isLunch }) {
                    trip.mealZone.dinnerType()
                } else {
                    ExpenseType.LUNCH
                }
            } else if (
                expenseType.isMealAllowance &&
                existingMeals.any { it.expenseType.isLunchDinner }
            ) {
                expenseType = ExpenseType.LUNCH
            }
        }
    }

    LaunchedEffect(source.uri) {
        val sourceUri = source.uri ?: return@LaunchedEffect
        runCatching {
            ReceiptOcr.detect(context.applicationContext, sourceUri, source.mimeType)
        }.onSuccess {
            ocrCandidates = it.amounts
            ocrDateCandidates = it.dates.filter { candidate ->
                !candidate.date.isBefore(trip.startDate) &&
                    !candidate.date.isAfter(trip.endDate)
            }
        }
        ocrLoading = false
        ocrCompleted = true
    }

    fun saveOrOfferCombined(
        selectedDate: LocalDate,
        selectedAmount: BigDecimal,
        selectedType: ExpenseType
    ) {
        if (
            shouldOfferCombinedCalculation(
                receipts = receipts,
                trip = trip,
                date = selectedDate,
                amount = selectedAmount,
                expenseType = selectedType,
                expenseLimits = expenseLimits,
                mealVoucherEmployerContribution = mealVoucherEmployerContribution
            )
        ) {
            combinedAdvice = Triple(selectedDate, selectedAmount, selectedType)
        } else {
            onConfirm(selectedDate, selectedAmount, selectedType, comment, false)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (source.uri == null) "Détails de la dépense" else "Détails du justificatif")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            source.uri == null -> Icons.Default.Add
                            source.mimeType == "application/pdf" -> Icons.Default.Description
                            else -> Icons.Default.InsertDriveFile
                        },
                        null, tint = FoRed
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            source.uri == null -> "Dépense sans justificatif"
                            source.mimeType == "application/pdf" -> "Document PDF"
                            else -> "Image"
                        },
                        color = FoRed
                    )
                }
                if (source.uri != null) {
                    OcrAmountSelector(
                        loading = ocrLoading,
                        completed = ocrCompleted,
                        candidates = ocrCandidates,
                        selectedAmount = amount,
                        onAmountSelected = { amount = it.toFrenchAmount() }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = {
                        Text(
                            if (fixedAllowanceWithoutAttachment) {
                                "Montant du forfait (€)"
                            } else {
                                "Montant TTC (€)"
                            }
                        )
                    },
                    supportingText = if (fixedAllowanceWithoutAttachment) {
                        { Text("Montant automatique selon le paramétrage du forfait.") }
                    } else {
                        null
                    },
                    enabled = !fixedAllowanceWithoutAttachment,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                CalendarDateField(
                    label = "Date du justificatif",
                    value = date,
                    onValueChange = { date = it },
                    minDate = trip.startDate,
                    maxDate = trip.endDate,
                    supportingText = "Entre ${trip.startDate.shortDateLabel()} et ${trip.endDate.shortDateLabel()}"
                )
                if (source.uri != null) {
                    OcrDateSelector(
                        candidates = ocrDateCandidates,
                        selectedDate = date,
                        onDateSelected = { date = it }
                    )
                }
                ExpenseCategoryDropdown(
                    selected = category,
                    onSelected = {
                        category = it
                        expenseType = if (it == ExpenseCategory.MEAL) {
                            ExpenseType.LUNCH
                        } else {
                            ExpenseType.defaultFor(it)
                        }
                    }
                )
                ExpenseTypeDropdown(
                    category = category,
                    selected = expenseType,
                    trip = trip,
                    date = date,
                    receipts = receipts,
                    onSelected = { expenseType = it }
                )
                if (expenseType == ExpenseType.MILEAGE_ALLOWANCES_BULL) {
                    MileageTripSection(
                        onResult = { amountText, description ->
                            comment = description
                            amountText?.let { amount = it }
                        }
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire / description ATOS") },
                    supportingText = {
                        Text("Facultatif : sinon le libellé et la date seront utilisés.")
                    },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                val configuredLimit = expenseLimits[expenseType]
                Text(
                    when {
                        configuredLimit != null ->
                            "Plafond configuré : ${configuredLimit.euros()}"
                        expenseType.defaultLimit != null ->
                            "Plafond par défaut : ${expenseType.defaultLimit!!.euros()}"
                        category == ExpenseCategory.MEAL ->
                            "Règle par défaut : ${trip.dailyMealAllowance.euros()} par jour pour les repas"
                        else ->
                            "Règle par défaut : aucun plafond spécifique"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val employerContributionAlreadyApplied = receipts.any {
                    it.tripId == trip.id &&
                        it.date == date &&
                        it.expenseType.receivesMealVoucherDeduction
                }
                if (
                    mealVoucherEmployerContribution.signum() > 0 &&
                    expenseType.receivesMealVoucherDeduction &&
                    !employerContributionAlreadyApplied
                ) {
                    Text(
                        "Participation employeur déduite sur ce justificatif : " +
                            "-${mealVoucherEmployerContribution.euros()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LimitRed
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amount.toMoneyOrNull()
                error = when {
                    parsedAmount == null || parsedAmount.signum() <= 0 -> "Saisissez un montant supérieur à 0."
                    date.isBefore(trip.startDate) || date.isAfter(trip.endDate) ->
                        "La date doit appartenir au déplacement."
                    else -> null
                }
                if (error == null) {
                    val normalizedType = expenseType.forMealZone(trip.mealZone)
                    val advice = mealAllowanceReplacementAdvice(
                        receipts = receipts,
                        trip = trip,
                        date = date,
                        amount = parsedAmount!!,
                        expenseType = normalizedType,
                        expenseLimits = expenseLimits,
                        mealVoucherEmployerContribution = mealVoucherEmployerContribution
                    )
                    if (advice != null) {
                        allowanceAdvice = advice
                    } else {
                        saveOrOfferCombined(date, parsedAmount, normalizedType)
                    }
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )

    allowanceAdvice?.let { advice ->
        MealAllowanceReplacementDialog(
            advice = advice,
            onDismiss = { allowanceAdvice = null },
            onAccept = {
                allowanceAdvice = null
                onConfirm(
                    advice.date,
                    advice.allowanceAmount,
                    advice.allowanceType,
                    comment,
                    false
                )
            },
            onDecline = {
                allowanceAdvice = null
                saveOrOfferCombined(advice.date, advice.originalAmount, advice.originalType)
            }
        )
    }

    combinedAdvice?.let { (adviceDate, adviceAmount, adviceType) ->
        CombinedMealAdviceDialog(
            trip = trip,
            combinedLimit = effectiveExpenseLimit(
                trip.mealZone.lunchDinnerType(),
                expenseLimits
            ) ?: trip.mealZone.dailyAllowance,
            onDismiss = { combinedAdvice = null },
            onApply = {
                combinedAdvice = null
                onConfirm(adviceDate, adviceAmount, adviceType, comment, true)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReceiptDialog(
    trip: Trip,
    receipt: Receipt,
    receipts: List<Receipt>,
    expenseLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, BigDecimal, ExpenseType, String, Boolean) -> Unit
) {
    var date by remember(receipt.id) { mutableStateOf(receipt.date) }
    var amount by remember(receipt.id) { mutableStateOf(receipt.amount.toFrenchAmount()) }
    var category by remember(receipt.id) { mutableStateOf(receipt.category) }
    var expenseType by remember(receipt.id) { mutableStateOf(receipt.expenseType) }
    var comment by remember(receipt.id) { mutableStateOf(receipt.comment) }
    var error by remember(receipt.id) { mutableStateOf<String?>(null) }
    var combinedAdvice by remember(receipt.id) {
        mutableStateOf<Triple<LocalDate, BigDecimal, ExpenseType>?>(null)
    }
    var allowanceAdvice by remember(receipt.id) {
        mutableStateOf<MealAllowanceReplacementAdvice?>(null)
    }
    val fixedAllowanceWithoutAttachment = !receipt.hasAttachment && expenseType.isMealAllowance

    LaunchedEffect(receipt.id, expenseType, expenseLimits) {
        if (fixedAllowanceWithoutAttachment) {
            effectiveExpenseLimit(expenseType, expenseLimits)?.let { allowance ->
                amount = allowance.toFrenchAmount()
            }
        }
    }

    LaunchedEffect(date, category, receipts) {
        if (category == ExpenseCategory.MEAL) {
            val existingMeals = receipts.filter {
                it.id != receipt.id &&
                    it.tripId == trip.id &&
                    it.date == date
            }
            if (
                expenseType.isLunchDinner &&
                existingMeals.any {
                    it.expenseType.isLunch ||
                        it.expenseType.isDinner ||
                        it.expenseType.isMealAllowance
                }
            ) {
                expenseType = if (existingMeals.any { it.expenseType.isLunch }) {
                    trip.mealZone.dinnerType()
                } else {
                    ExpenseType.LUNCH
                }
            } else if (
                expenseType.isMealAllowance &&
                existingMeals.any { it.expenseType.isLunchDinner }
            ) {
                expenseType = ExpenseType.LUNCH
            }
        }
    }

    fun saveOrOfferCombined(
        selectedDate: LocalDate,
        selectedAmount: BigDecimal,
        selectedType: ExpenseType
    ) {
        if (
            shouldOfferCombinedCalculation(
                receipts = receipts,
                trip = trip,
                date = selectedDate,
                amount = selectedAmount,
                expenseType = selectedType,
                expenseLimits = expenseLimits,
                mealVoucherEmployerContribution = mealVoucherEmployerContribution,
                editingReceiptId = receipt.id
            )
        ) {
            combinedAdvice = Triple(selectedDate, selectedAmount, selectedType)
        } else {
            onConfirm(selectedDate, selectedAmount, selectedType, comment, false)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le justificatif") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalendarDateField(
                    label = "Date du justificatif",
                    value = date,
                    onValueChange = { date = it },
                    minDate = trip.startDate,
                    maxDate = trip.endDate,
                    supportingText = "Entre ${trip.startDate.shortDateLabel()} et ${trip.endDate.shortDateLabel()}"
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    label = {
                        Text(
                            if (fixedAllowanceWithoutAttachment) {
                                "Montant du forfait (€)"
                            } else {
                                "Montant TTC (€)"
                            }
                        )
                    },
                    supportingText = if (fixedAllowanceWithoutAttachment) {
                        { Text("Montant automatique selon le paramétrage du forfait.") }
                    } else {
                        null
                    },
                    enabled = !fixedAllowanceWithoutAttachment,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                ExpenseCategoryDropdown(
                    selected = category,
                    onSelected = {
                        category = it
                        expenseType = if (it == ExpenseCategory.MEAL) {
                            ExpenseType.LUNCH
                        } else {
                            ExpenseType.defaultFor(it)
                        }
                    }
                )
                ExpenseTypeDropdown(
                    category = category,
                    selected = expenseType,
                    trip = trip,
                    date = date,
                    receipts = receipts,
                    editingReceiptId = receipt.id,
                    onSelected = { expenseType = it }
                )
                if (expenseType == ExpenseType.MILEAGE_ALLOWANCES_BULL) {
                    MileageTripSection(
                        onResult = { amountText, description ->
                            comment = description
                            amountText?.let { amount = it }
                        }
                    )
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire / description ATOS") },
                    supportingText = {
                        Text("Facultatif : sinon le libellé et la date seront utilisés.")
                    },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                val configuredLimit = expenseLimits[expenseType]
                Text(
                    when {
                        configuredLimit != null ->
                            "Plafond configuré : ${configuredLimit.euros()}"
                        expenseType.defaultLimit != null ->
                            "Plafond par défaut : ${expenseType.defaultLimit!!.euros()}"
                        category == ExpenseCategory.MEAL ->
                            "Règle par défaut : ${trip.dailyMealAllowance.euros()} par jour pour les repas"
                        else ->
                            "Règle par défaut : aucun plafond spécifique"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val employerContributionAlreadyApplied = receipts.any {
                    it.id != receipt.id &&
                        it.tripId == trip.id &&
                        it.date == date &&
                        it.expenseType.receivesMealVoucherDeduction
                }
                if (
                    mealVoucherEmployerContribution.signum() > 0 &&
                    expenseType.receivesMealVoucherDeduction &&
                    !employerContributionAlreadyApplied
                ) {
                    Text(
                        "Participation employeur déduite sur ce justificatif : " +
                            "-${mealVoucherEmployerContribution.euros()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LimitRed
                    )
                }
                Text(
                    "Le montant remboursable et l’annotation du fichier seront recalculés.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amount.toMoneyOrNull()
                error = when {
                    parsedAmount == null || parsedAmount.signum() <= 0 ->
                        "Saisissez un montant supérieur à 0."
                    date.isBefore(trip.startDate) || date.isAfter(trip.endDate) ->
                        "La date doit appartenir au déplacement."
                    else -> null
                }
                if (error == null) {
                    val normalizedType = expenseType.forMealZone(trip.mealZone)
                    val advice = mealAllowanceReplacementAdvice(
                        receipts = receipts,
                        trip = trip,
                        date = date,
                        amount = parsedAmount!!,
                        expenseType = normalizedType,
                        expenseLimits = expenseLimits,
                        mealVoucherEmployerContribution = mealVoucherEmployerContribution,
                        editingReceiptId = receipt.id
                    )
                    if (advice != null) {
                        allowanceAdvice = advice
                    } else {
                        saveOrOfferCombined(date, parsedAmount, normalizedType)
                    }
                }
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )

    allowanceAdvice?.let { advice ->
        MealAllowanceReplacementDialog(
            advice = advice,
            onDismiss = { allowanceAdvice = null },
            onAccept = {
                allowanceAdvice = null
                onConfirm(
                    advice.date,
                    advice.allowanceAmount,
                    advice.allowanceType,
                    comment,
                    false
                )
            },
            onDecline = {
                allowanceAdvice = null
                saveOrOfferCombined(advice.date, advice.originalAmount, advice.originalType)
            }
        )
    }

    combinedAdvice?.let { (adviceDate, adviceAmount, adviceType) ->
        CombinedMealAdviceDialog(
            trip = trip,
            combinedLimit = effectiveExpenseLimit(
                trip.mealZone.lunchDinnerType(),
                expenseLimits
            ) ?: trip.mealZone.dailyAllowance,
            onDismiss = { combinedAdvice = null },
            onApply = {
                combinedAdvice = null
                onConfirm(adviceDate, adviceAmount, adviceType, comment, true)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseCategoryDropdown(
    selected: ExpenseCategory,
    onSelected: (ExpenseCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Catégorie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ExpenseCategory.entries
                .filterNot { it == ExpenseCategory.OTHER }
                .forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTypeDropdown(
    category: ExpenseCategory,
    selected: ExpenseType,
    trip: Trip,
    date: LocalDate,
    receipts: List<Receipt>,
    editingReceiptId: String? = null,
    onSelected: (ExpenseType) -> Unit
) {
    var expanded by remember(category) { mutableStateOf(false) }
    val hasSeparateMeal = receipts.any {
        it.id != editingReceiptId &&
            it.tripId == trip.id &&
            it.date == date &&
            (it.expenseType.isLunch || it.expenseType.isDinner)
    }
    val hasLunchDinner = receipts.any {
        it.id != editingReceiptId &&
            it.tripId == trip.id &&
            it.date == date &&
            it.expenseType.isLunchDinner
    }
    val hasMealAllowance = receipts.any {
        it.id != editingReceiptId &&
            it.tripId == trip.id &&
            it.date == date &&
            it.expenseType.isMealAllowance
    }
    val options = ExpenseType.forCategory(category)
        .filter { type ->
            if (category != ExpenseCategory.MEAL) {
                true
            } else {
                val normalized = type.forMealZone(trip.mealZone)
                normalized == type &&
                    (!type.isLunchDinner || (!hasSeparateMeal && !hasMealAllowance)) &&
                    (!type.isMealAllowance || !hasLunchDinner)
            }
        }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.displayLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type de frais ATOS") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayLabel) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CombinedMealAdviceDialog(
    trip: Trip,
    combinedLimit: BigDecimal,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = FoRed
            )
        },
        title = { Text("Calcul lunch + dinner plus avantageux") },
        text = {
            Text(
                "Le calcul cumulé rembourse davantage que les plafonds séparés. " +
                    "Le plafond journalier applicable est de ${combinedLimit.euros()} " +
                    "pour ${trip.mealZone.label}. Le calcul cumulé va être appliqué aux deux repas."
            )
        },
        confirmButton = {
            TextButton(onClick = onApply) {
                Text("Appliquer le cumul")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun shouldOfferCombinedCalculation(
    receipts: List<Receipt>,
    trip: Trip,
    date: LocalDate,
    amount: BigDecimal,
    expenseType: ExpenseType,
    expenseLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    editingReceiptId: String? = null
): Boolean {
    if (!expenseType.isLunch && !expenseType.isDinner) return false
    if (
        receipts.any {
            it.id != editingReceiptId &&
                it.tripId == trip.id &&
                it.date == date &&
                it.expenseType.isMealAllowance
        }
    ) return false

    val normalizedType = expenseType.forMealZone(trip.mealZone)
    val entries = receipts
        .filter {
            it.id != editingReceiptId &&
                it.tripId == trip.id &&
                it.date == date &&
                it.expenseType.isStructuredMeal
        }
        .map { receipt ->
            val type = when {
                receipt.expenseType.isLunchDinner && normalizedType.isLunch ->
                    trip.mealZone.dinnerType()
                receipt.expenseType.isLunchDinner && normalizedType.isDinner ->
                    ExpenseType.LUNCH
                else -> receipt.expenseType.forMealZone(trip.mealZone)
            }
            MealEntry(receipt.amount, type)
        } + MealEntry(amount, normalizedType)

    return isCombinedMealCalculationBeneficial(
        entries = entries,
        zone = trip.mealZone,
        customLimits = expenseLimits,
        employerContribution = mealVoucherEmployerContribution
    )
}

private data class MealAllowanceReplacementAdvice(
    val date: LocalDate,
    val originalAmount: BigDecimal,
    val originalType: ExpenseType,
    val allowanceType: ExpenseType,
    val allowanceAmount: BigDecimal,
    val currentReimbursement: BigDecimal,
    val allowanceReimbursement: BigDecimal
) {
    val gain: BigDecimal get() = allowanceReimbursement - currentReimbursement
}

private fun mealAllowanceReplacementAdvice(
    receipts: List<Receipt>,
    trip: Trip,
    date: LocalDate,
    amount: BigDecimal,
    expenseType: ExpenseType,
    expenseLimits: Map<ExpenseType, BigDecimal>,
    mealVoucherEmployerContribution: BigDecimal,
    editingReceiptId: String? = null
): MealAllowanceReplacementAdvice? {
    if (!expenseType.isStructuredMeal) return null
    if (
        receipts.any {
            it.id != editingReceiptId &&
                it.tripId == trip.id &&
                it.date == date &&
                it.expenseType.isLunchDinner
        }
    ) return null
    val allowanceType = trip.mealZone.mealAllowanceType()
    val allowanceAmount = (expenseLimits[allowanceType] ?: allowanceType.defaultLimit)
        ?.takeIf { it.signum() > 0 }
        ?: return null
    if (amount >= allowanceAmount) return null

    val previousReceipts = receipts.filter {
        it.id != editingReceiptId &&
            it.tripId == trip.id &&
            it.date == date
    }
    val currentReimbursement = calculateReceiptReimbursement(
        amount = amount,
        expenseType = expenseType,
        trip = trip,
        date = date,
        previousReceipts = previousReceipts,
        customLimits = expenseLimits,
        mealVoucherEmployerContribution = mealVoucherEmployerContribution
    ).reimbursableAmount
    val allowanceReimbursement = calculateReceiptReimbursement(
        amount = allowanceAmount,
        expenseType = allowanceType,
        trip = trip,
        date = date,
        previousReceipts = previousReceipts,
        customLimits = expenseLimits,
        mealVoucherEmployerContribution = mealVoucherEmployerContribution
    ).reimbursableAmount
    if (allowanceReimbursement <= currentReimbursement) return null

    return MealAllowanceReplacementAdvice(
        date = date,
        originalAmount = amount,
        originalType = expenseType,
        allowanceType = allowanceType,
        allowanceAmount = allowanceAmount,
        currentReimbursement = currentReimbursement,
        allowanceReimbursement = allowanceReimbursement
    )
}

@Composable
private fun MealAllowanceReplacementDialog(
    advice: MealAllowanceReplacementAdvice,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restaurant, contentDescription = null, tint = FoRed) },
        title = { Text("Meal allowance plus avantageux") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Le repas saisi est de ${advice.originalAmount.euros()}, alors que le " +
                        "meal allowance disponible est de ${advice.allowanceAmount.euros()}."
                )
                Text(
                    "En conservant le ticket : ${advice.currentReimbursement.euros()} remboursables."
                )
                Text(
                    "En utilisant ${advice.allowanceType.displayLabel} : " +
                        "${advice.allowanceReimbursement.euros()}, soit +${advice.gain.euros()}.",
                    color = FoDarkRed,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Le remplacement est facultatif.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Utiliser le meal allowance") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("Conserver le repas saisi") }
        }
    )
}

@Composable
private fun MealAllowanceChoiceDialog(
    suggestions: List<SingleMealAllowanceSuggestion>,
    onDismiss: () -> Unit,
    onAccept: (Set<LocalDate>) -> Unit,
    onDecline: () -> Unit
) {
    val plural = suggestions.size > 1
    var selectedDates by remember(suggestions) {
        mutableStateOf<Set<LocalDate>>(suggestions.mapTo(mutableSetOf()) { it.date })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = FoRed)
        },
        title = {
            Text(if (plural) "Ajouter des repas sans justificatif ?" else "Ajouter un repas sans justificatif ?")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Cet ajout est facultatif. Fo Notes le propose uniquement parce qu’il " +
                        "augmente le remboursement estimé."
                )
                suggestions.forEach { suggestion ->
                    val missingMeal = if (suggestion.missingMealLabel.startsWith("Lunch")) {
                        "déjeuner manquant"
                    } else {
                        "dîner manquant"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = if (plural) {
                            Modifier.clickable {
                                selectedDates = if (suggestion.date in selectedDates) {
                                    selectedDates - suggestion.date
                                } else {
                                    selectedDates + suggestion.date
                                }
                            }
                        } else {
                            Modifier
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (plural) {
                                Checkbox(
                                    checked = suggestion.date in selectedDates,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "${suggestion.date.shortDateLabel()} — $missingMeal",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Sans ajout : ${suggestion.reimbursementWithoutAddition.euros()} remboursables."
                                )
                                Text(
                                    "Avec le repas manquant en meal allowance de " +
                                        "${suggestion.allowanceAmount.euros()} : " +
                                        "${suggestion.reimbursementWithAllowance.euros()}, soit " +
                                        "+${suggestion.reimbursementGain.euros()}.",
                                    color = FoDarkRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Text(
                    "Aucune facturette ne sera associée au repas ajouté.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAccept(
                        if (plural) selectedDates else setOf(suggestions.single().date)
                    )
                }
            ) {
                Text(if (plural) "Continuer avec la sélection" else "Ajouter le repas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Continuer sans ajout")
            }
        }
    )
}

@Composable
private fun OcrDateSelector(
    candidates: List<OcrDateCandidate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    if (candidates.isEmpty()) return

    Column {
        Text(
            "Date détectée sur la facture",
            fontWeight = FontWeight.Medium,
            color = FoRed,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidates, key = { it.date.toString() }) { candidate ->
                AssistChip(
                    onClick = { onDateSelected(candidate.date) },
                    label = {
                        Text(
                            candidate.date.shortDateLabel(),
                            fontWeight = if (selectedDate == candidate.date) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OcrAmountSelector(
    loading: Boolean,
    completed: Boolean,
    candidates: List<OcrAmountCandidate>,
    selectedAmount: String,
    onAmountSelected: (BigDecimal) -> Unit
) {
    when {
        loading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().background(
                    FoPaleRed,
                    RoundedCornerShape(10.dp)
                ).padding(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text("Recherche des montants…", fontSize = 13.sp, color = FoRed)
            }
        }

        candidates.isNotEmpty() -> {
            Column {
                Text(
                    "Montants détectés",
                    fontWeight = FontWeight.Medium,
                    color = FoRed
                )
                Text(
                    "Touchez le montant TTC de la facture.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(candidates, key = { "${it.amount}-${it.sourceLine}" }) { candidate ->
                        val label = candidate.amount.toFrenchAmount()
                        AssistChip(
                            onClick = { onAmountSelected(candidate.amount) },
                            label = {
                                Text(
                                    "$label €",
                                    fontWeight = if (selectedAmount == label) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        completed -> {
            Text(
                "Aucun montant détecté. Vous pouvez le saisir manuellement.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDateField(
    label: String,
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    supportingText: String? = null
) {
    var showCalendar by remember { mutableStateOf(false) }

    Column {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedButton(
            onClick = { showCalendar = true },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(
                value.fullDateLabel(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = "Ouvrir le calendrier"
            )
        }
        supportingText?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }

    if (showCalendar) {
        val selectableDates = remember(minDate, maxDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = utcTimeMillis.toLocalDateUtc()
                    return (minDate == null || !date.isBefore(minDate)) &&
                        (maxDate == null || !date.isAfter(maxDate))
                }

                override fun isSelectableYear(year: Int): Boolean =
                    (minDate == null || year >= minDate.year) &&
                        (maxDate == null || year <= maxDate.year)
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toUtcMillis(),
            selectableDates = selectableDates
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            onValueChange(it.toLocalDateUtc())
                        }
                        showCalendar = false
                    },
                    enabled = pickerState.selectedDateMillis != null
                ) {
                    Text("Choisir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private data class PendingSource(val uri: Uri?, val mimeType: String)

private data class PendingTrackingUpdate(
    val trip: Trip,
    val status: TripStatus,
    val submittedDate: LocalDate?,
    val suggestions: List<SingleMealAllowanceSuggestion> = emptyList()
)

private fun exportTripByEmail(
    context: android.content.Context,
    trip: Trip,
    receipts: List<Receipt>,
    fileFor: (Receipt) -> File
) {
    TripEmailExporter.send(
        context = context,
        trip = trip,
        receipts = receipts,
        sourceFile = fileFor
    ).onFailure { error ->
        Toast.makeText(
            context,
            error.message ?: "L’export du déplacement est impossible.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun openReceipt(context: android.content.Context, receipt: Receipt, file: File) {
    if (!file.exists()) {
        Toast.makeText(context, "Le fichier n’existe plus.", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, receipt.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "Aucune application ne peut ouvrir ce fichier.", Toast.LENGTH_LONG).show()
        }
}

private fun categoryIcon(category: ExpenseCategory): ImageVector = when (category) {
    ExpenseCategory.MEAL -> Icons.Default.Restaurant
    ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
    ExpenseCategory.HOTEL -> Icons.Default.Hotel
    ExpenseCategory.HOUSING -> Icons.Default.Hotel
    ExpenseCategory.TELECOM -> Icons.Default.MoreHoriz
    ExpenseCategory.PROFESSIONAL -> Icons.Default.ReceiptLong
    ExpenseCategory.MOBILITY -> Icons.Default.DirectionsCar
    ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
}

private fun categoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.MEAL -> FoPaleRed
    ExpenseCategory.TRANSPORT -> Color(0xFFE8EFFB)
    ExpenseCategory.HOTEL -> Color(0xFFFFF0D8)
    ExpenseCategory.HOUSING -> Color(0xFFF2EAF8)
    ExpenseCategory.TELECOM -> Color(0xFFE8EFFB)
    ExpenseCategory.PROFESSIONAL -> Color(0xFFFFF1DF)
    ExpenseCategory.MOBILITY -> Color(0xFFE9EDF4)
    ExpenseCategory.OTHER -> Color(0xFFF3F0E8)
}

private fun tripStatusColor(status: TripStatus): Color = when (status) {
    TripStatus.DRAFT -> Color(0xFFF1ECEE)
    TripStatus.SENT -> Color(0xFFE8EFFB)
    TripStatus.VALIDATED -> Color(0xFFFFF0D8)
    TripStatus.REIMBURSED -> Color(0xFFE4F3E8)
}

private fun tripStatusTextColor(status: TripStatus): Color = when (status) {
    TripStatus.DRAFT -> Color(0xFF665B5D)
    TripStatus.SENT -> Color(0xFF234B83)
    TripStatus.VALIDATED -> Color(0xFF805500)
    TripStatus.REIMBURSED -> Color(0xFF1E6334)
}

private fun BigDecimal.euros(): String =
    "${setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')} €"

private fun String.toMoneyOrNull(): BigDecimal? =
    trim().replace(',', '.').toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)

private fun BigDecimal.toFrenchAmount(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')

private fun Trip.dateRangeLabel(): String {
    val short = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.FRANCE)
    return "Du ${startDate.format(short)} au ${endDate.format(short)}"
}

private fun LocalDate.fullDateLabel(): String =
    format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
        .replaceFirstChar { it.titlecase(Locale.FRANCE) }

private fun LocalDate.shortDateLabel(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.FRANCE))

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
