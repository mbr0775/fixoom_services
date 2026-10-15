package com.fixoom.marketplace.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixoom.core.data.repository.MarketplaceViewModel
import com.fixoom.core.model.*
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.UUID

@Composable
fun ConnectedProviderList(
    category: String,
    vm: MarketplaceViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val service =
        state.data.categories.firstOrNull {
            it.name.equals(category, true) || it.slug.equals(category, true)
        }
    val professionals =
        state.data.professionals.filter {
            (category.isBlank() || it.categorySlug == service?.slug) &&
                (it.displayName + " " + it.serviceArea).contains(search, true)
        }
    MarketplacePage(
        if (category.isBlank()) "Service professionals" else category,
        state,
        vm::refresh,
        onBack,
    ) {
        item {
            OutlinedTextField(
                search,
                { search = it },
                Modifier.fillMaxWidth(),
                label = { Text("Search name or service area") },
                singleLine = true,
            )
        }
        if (!state.loading && professionals.isEmpty())
            item {
                InfoCard(
                    "No available professionals",
                    if (service == null && category.isNotBlank())
                        "This service is not currently open for requests."
                    else
                        "Approved, available providers will appear here. Try another area or check again later.",
                )
            }
        items(professionals, key = { it.id }) { p ->
            ProfessionalSummary(
                p,
                state.data.categories.find { it.slug == p.categorySlug }?.name.orEmpty(),
            ) {
                onOpen(p.id)
            }
        }
    }
}

@Composable
fun ConnectedProviderDetails(
    id: String,
    vm: MarketplaceViewModel,
    onBack: () -> Unit,
    onBook: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val provider = state.data.professionals.find { it.id == id }
    MarketplacePage("Professional profile", state, vm::refresh, onBack) {
        if (provider != null) {
            item {
                InfoCard(
                    provider.displayName,
                    state.data.categories.find { it.slug == provider.categorySlug }?.name.orEmpty(),
                ) {
                    Text("Service area: ${provider.serviceArea}")
                    Text(
                        provider.bio.ifBlank { "This professional has not added an introduction." }
                    )
                }
            }
            item {
                InfoCard(
                    "Pay directly after service",
                    "Discuss the scope and price with your provider before work starts. Fixoom does not collect payment in the app.",
                )
            }
            item {
                Button({ onBook(id) }, Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text("Request a service")
                }
            }
        } else if (!state.loading)
            item {
                InfoCard(
                    "Provider unavailable",
                    "This professional is not currently accepting new requests.",
                )
            }
    }
}

@Composable
fun ConnectedBookingForm(
    id: String,
    vm: MarketplaceViewModel,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val provider = state.data.professionals.find { it.id == id }
    val customer =
        state.data.profile?.let {
            it.role == AppRole.CUSTOMER && it.accountStatus == AccountStatus.ACTIVE
        } == true
    var address by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var scheduled by rememberSaveable {
        mutableLongStateOf(
            Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Colombo"))
                .apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .timeInMillis
        )
    }
    val requestKey = rememberSaveable { UUID.randomUUID().toString() }
    var validation by remember { mutableStateOf<String?>(null) }
    MarketplacePage("Request a service", state, vm::refresh, onBack) {
        if (!customer)
            item {
                InfoCard(
                    "Sign in to book",
                    "Use a customer account so your booking and messages are saved securely.",
                ) {
                    Button(onSignIn) { Text("Sign in or create account") }
                }
            }
        else if (provider == null && !state.loading)
            item {
                InfoCard(
                    "Provider unavailable",
                    "Refresh the directory and select an available professional.",
                )
            }
        else if (provider != null) {
            item {
                InfoCard(
                    provider.displayName,
                    "${provider.serviceArea} · Payment directly after service",
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedButton(
                        onClick = {
                            val date =
                                Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Colombo"))
                                    .apply { timeInMillis = scheduled }
                            DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        scheduled =
                                            Calendar.getInstance(
                                                    java.util.TimeZone.getTimeZone("Asia/Colombo")
                                                )
                                                .apply {
                                                    timeInMillis = scheduled
                                                    set(y, m, d)
                                                }
                                                .timeInMillis
                                    },
                                    date.get(Calendar.YEAR),
                                    date.get(Calendar.MONTH),
                                    date.get(Calendar.DAY_OF_MONTH),
                                )
                                .apply {
                                    datePicker.minDate = System.currentTimeMillis()
                                    show()
                                }
                        },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Date: " +
                                Instant.ofEpochMilli(scheduled)
                                    .atZone(ZoneId.of("Asia/Colombo"))
                                    .toLocalDate()
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val date =
                                Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Colombo"))
                                    .apply { timeInMillis = scheduled }
                            TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        scheduled =
                                            Calendar.getInstance(
                                                    java.util.TimeZone.getTimeZone("Asia/Colombo")
                                                )
                                                .apply {
                                                    timeInMillis = scheduled
                                                    set(Calendar.HOUR_OF_DAY, h)
                                                    set(Calendar.MINUTE, m)
                                                }
                                                .timeInMillis
                                    },
                                    date.get(Calendar.HOUR_OF_DAY),
                                    date.get(Calendar.MINUTE),
                                    false,
                                )
                                .show()
                        },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Time: " +
                                serviceTime(
                                    Instant.ofEpochMilli(scheduled)
                                        .atOffset(java.time.ZoneOffset.UTC)
                                        .toString()
                                )
                        )
                    }
                    OutlinedTextField(
                        address,
                        { if (it.length <= 500) address = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Service address") },
                        enabled = !state.saving,
                        minLines = 2,
                    )
                    OutlinedTextField(
                        description,
                        { if (it.length <= 4000) description = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("What needs fixing?") },
                        enabled = !state.saving,
                        minLines = 4,
                    )
                    validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text(
                        "Times are shown in Sri Lanka time. Your provider must accept the request before the job is confirmed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            validation =
                                when {
                                    scheduled <= System.currentTimeMillis() ->
                                        "Choose a future date and time."
                                    address.trim().length < 5 -> "Enter a complete service address."
                                    description.trim().length < 5 -> "Describe the work you need."
                                    else -> null
                                }
                            if (validation == null)
                                vm.runAction {
                                    val booking =
                                        createBooking(
                                            requestKey,
                                            id,
                                            Instant.ofEpochMilli(scheduled).toString(),
                                            address,
                                            description,
                                        )
                                    onCreated(booking.id)
                                }
                        },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        Text("Send booking request")
                    }
                    Text(
                        "If the connection is interrupted, retry this request with the same details. Your request key prevents duplicate bookings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectedAccount(vm: MarketplaceViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profile = state.data.profile
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.fullName.orEmpty()) }
    var phone by rememberSaveable(profile?.id) { mutableStateOf(profile?.phone.orEmpty()) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profile?.id) { if (profile != null) password = "" }
    MarketplacePage(
        if (profile == null) "Your Fixoom account" else "Account details",
        state,
        vm::refresh,
        onBack,
    ) {
        if (profile == null) {
            item {
                InfoCard(
                    if (register) "Create a customer account" else "Welcome back",
                    "Save your bookings, talk to professionals, and follow each service from request to completion.",
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (register) {
                        OutlinedTextField(
                            name,
                            { if (it.length <= 100) name = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("Full name") },
                            singleLine = true,
                            enabled = !state.saving,
                        )
                        OutlinedTextField(
                            phone,
                            { phone = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("Phone, e.g. +94771234567 (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = !state.saving,
                        )
                    }
                    OutlinedTextField(
                        email,
                        { email = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Email address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    OutlinedTextField(
                        password,
                        { password = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        visualTransformation =
                            if (visible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton({ visible = !visible }) {
                                Text(if (visible) "Hide" else "Show")
                            }
                        },
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            validation =
                                when {
                                    !email.contains('@') -> "Enter your email address."
                                    password.length < 8 ->
                                        "Use a password with at least eight characters."
                                    register && name.trim().length < 2 -> "Enter your full name."
                                    register &&
                                        phone.isNotBlank() &&
                                        !phone.matches(Regex("\\+[1-9][0-9]{7,14}")) ->
                                        "Enter a phone number with country code."
                                    else -> null
                                }
                            if (validation == null)
                                vm.authenticate(register, name, email, phone, password)
                        },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        Text(if (register) "Create account" else "Sign in")
                    }
                    TextButton(
                        {
                            register = !register
                            password = ""
                        },
                        enabled = !state.saving,
                    ) {
                        Text(
                            if (register) "Already have an account? Sign in"
                            else "New to Fixoom? Create account"
                        )
                    }
                }
            }
        } else {
            item {
                InfoCard(
                    profile.fullName.orEmpty(),
                    "${profile.role.name.lowercase()} · ${profile.accountStatus.name.lowercase()}",
                )
            }
            if (profile.role == AppRole.CUSTOMER && profile.accountStatus == AccountStatus.ACTIVE)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            name,
                            { if (it.length <= 100) name = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("Full name") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            phone,
                            { phone = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("Phone with country code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                        )
                        Button(
                            {
                                if (
                                    name.trim().length >= 2 &&
                                        (phone.isBlank() ||
                                            phone.matches(Regex("\\+[1-9][0-9]{7,14}")))
                                )
                                    vm.runAction { saveAccount(name, phone) }
                                else validation = "Check your name and phone number."
                            },
                            enabled = !state.saving,
                        ) {
                            Text("Save account")
                        }
                        validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            item {
                OutlinedButton(
                    vm::signOut,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out")
                }
            }
        }
    }
}
