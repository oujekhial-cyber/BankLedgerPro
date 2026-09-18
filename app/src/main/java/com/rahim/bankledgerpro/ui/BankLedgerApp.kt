package com.rahim.bankledgerpro.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahim.bankledgerpro.data.TransactionEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BankLedgerApp(vm: AppViewModel, restored: Boolean) {
    BankLedgerTheme {
        var drawer by remember { mutableStateOf(false) }
        var showManual by remember { mutableStateOf(false) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        LaunchedEffect(drawer) {
            if (drawer) drawerState.open() else drawerState.close()
        }
        var edit by remember { mutableStateOf<TransactionEntity?>(null) }
        val pending = vm.pending

        if (pending != null) {
            TransactionDialog(
                initial = pending,
                onSave = { d ->
                    vm.add(pending.amount, pending.type, pending.bank, d, pending.balanceAfter, pending.rawSms)
                    vm.consumePending()
                },
                onDismiss = { vm.consumePending() }
            )
        }
        if (showManual) {
            TransactionDialog(
                initial = null,
                onSave = { d -> vm.add(d.amount, d.type, d.bank, d.description, d.balanceAfter); showManual = false },
                onDismiss = { showManual = false }
            )
        }
        edit?.let { item ->
            TransactionDialog(
                initial = item,
                onSave = { d -> vm.update(d.copy(id = item.id, timestamp = item.timestamp)); edit = null },
                onDismiss = { edit = null }
            )
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = Color(0xFF151821)
                ) {
                    Spacer(Modifier.height(36.dp))
                    Text("دفتر بانک", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Text("مدیریت ساده و هوشمند پول", color = Color(0xFF9CA3AF), modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                    Spacer(Modifier.height(28.dp))
                    DrawerItem("داشبورد", Icons.Default.Home, vm.screen == Screen.DASHBOARD) { vm.screen = Screen.DASHBOARD; drawer = false }
                    DrawerItem("تراکنش‌ها", Icons.Default.List, vm.screen == Screen.TRANSACTIONS) { vm.screen = Screen.TRANSACTIONS; drawer = false }
                    DrawerItem("بانک‌ها و پیامک", Icons.Default.AccountBalance, vm.screen == Screen.BANKS) { vm.screen = Screen.BANKS; drawer = false }
                    DrawerItem("تنظیمات", Icons.Default.Settings, vm.screen == Screen.SETTINGS) { vm.screen = Screen.SETTINGS; drawer = false }
                    Spacer(Modifier.weight(1f))
                    Text("نسخه 1.0", color = Color.Gray, modifier = Modifier.padding(24.dp))
                }
            }
        ) {
            Scaffold(
                containerColor = Color(0xFF11131A),
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showManual = true },
                        containerColor = Color(0xFF42D3B2),
                        contentColor = Color(0xFF0E1715)
                    ) { Icon(Icons.Default.Add, "ثبت تراکنش") }
                }
            ) { pad ->
                Column(Modifier.fillMaxSize().padding(pad)) {
                    TopBar(vm.screen.name, onMenu = { drawer = true })
                    AnimatedContent(vm.screen, label = "screen") { screen ->
                        when (screen) {
                            Screen.DASHBOARD -> Dashboard(vm)
                            Screen.TRANSACTIONS -> Transactions(vm, onEdit = { edit = it })
                            Screen.BANKS -> BanksScreen(vm)
                            Screen.SETTINGS -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, click: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(title) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = click,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun TopBar(screenName: String, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "منو") }
        Spacer(Modifier.width(8.dp))
        Text(
            when (screenName) {
                "DASHBOARD" -> "داشبورد"
                "TRANSACTIONS" -> "تراکنش‌ها"
                "BANKS" -> "بانک‌ها و پیامک"
                else -> "تنظیمات"
            },
            fontSize = 23.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Dashboard(vm: AppViewModel) {
    val list by vm.transactions.collectAsState()
    val now = Calendar.getInstance()
    val dayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val weekStart = Calendar.getInstance().apply { firstDayOfWeek = Calendar.SATURDAY; set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    val today = list.filter { it.timestamp >= dayStart }
    val week = list.filter { it.timestamp >= weekStart }
    val month = list.filter { it.timestamp >= monthStart }
    val income = month.filter { it.type == "IN" }.sumOf { it.amount }
    val expense = month.filter { it.type == "OUT" }.sumOf { it.amount }

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2528)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("خلاصه این ماه", color = Color(0xFFB9C0C8))
                    Text(formatMoney(income - expense, vm.currency), fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("خالص جریان پول", color = Color(0xFF42D3B2), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("امروز", today.size, today.filter { it.type == "IN" }.sumOf { it.amount }, vm.currency, Modifier.weight(1f))
                StatCard("این هفته", week.size, week.filter { it.type == "OUT" }.sumOf { it.amount }, vm.currency, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("واریز ماه", month.count { it.type == "IN" }, income, vm.currency, Modifier.weight(1f))
                StatCard("برداشت ماه", month.count { it.type == "OUT" }, expense, vm.currency, Modifier.weight(1f))
            }
        }
        item { Text("آخرین تراکنش‌ها", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
        items(list.take(6), key = { it.id }) { TransactionRow(it, vm.currency, null, null) }
    }
}

@Composable
private fun StatCard(title: String, count: Int, amount: Long, currency: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF9CA3AF))
            Text("$count تراکنش", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            Text(formatMoney(amount, currency), fontSize = 15.sp)
        }
    }
}

@Composable
private fun Transactions(vm: AppViewModel, onEdit: (TransactionEntity) -> Unit) {
    val list by vm.transactions.collectAsState()
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("هنوز تراکنشی ثبت نشده") }
        return
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { t ->
            TransactionRow(t, vm.currency, { onEdit(t) }, { vm.delete(t) })
        }
    }
}

@Composable
private fun TransactionRow(t: TransactionEntity, currency: String, edit: (() -> Unit)?, delete: (() -> Unit)?) {
    var menu by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (t.type == "IN") Color(0xFF203B35) else Color(0xFF3A2529)), contentAlignment = Alignment.Center) {
                Icon(if (t.type == "IN") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (t.type == "IN") Color(0xFF42D3B2) else Color(0xFFFF7785))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.description.ifBlank { if (t.type == "IN") "واریز بانکی" else "برداشت بانکی" }, fontWeight = FontWeight.SemiBold)
                Text("${t.bank} • ${dateText(t.timestamp)}", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            }
            Text((if (t.type == "IN") "+" else "-") + formatMoney(t.amount, currency), fontWeight = FontWeight.Bold)
            if (edit != null) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "گزینه‌ها") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("ویرایش") }, onClick = { menu = false; edit() })
                        DropdownMenuItem(text = { Text("حذف") }, onClick = { menu = false; delete?.invoke() })
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDialog(initial: TransactionEntity?, onSave: (DialogData) -> Unit, onDismiss: () -> Unit) {
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "OUT") }
    var bank by remember { mutableStateOf(initial?.bank ?: "") }
    var balanceText by remember { mutableStateOf(initial?.balanceAfter?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ثبت تراکنش" else "ثبت توضیح تراکنش", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (initial == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(type == "IN", { type = "IN" }, label = { Text("واریز") })
                        FilterChip(type == "OUT", { type = "OUT" }, label = { Text("برداشت") })
                    }
                    OutlinedTextField(amountText, { amountText = it }, label = { Text("مبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bank, { bank = it }, label = { Text("بانک") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(desc, { desc = it }, label = { Text("برای چی بود؟") }, placeholder = { Text("مثلاً خرید مواد غذایی، حقوق، اجاره...") }, modifier = Modifier.fillMaxWidth())
                if (initial == null) {
                    OutlinedTextField(balanceText, { balanceText = it }, label = { Text("موجودی بعد از تراکنش (اختیاری)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.replace(",", "").toLongOrNull() ?: initial?.amount ?: 0
                if (amount > 0) onSave(DialogData(amount, type, bank.ifBlank { "نامشخص" }, desc, balanceText.toLongOrNull()))
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بعداً") } }
    )
}

private data class DialogData(val amount: Long, val type: String, val bank: String, val description: String, val balanceAfter: Long?)

@Composable
private fun BanksScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var customBank by remember { mutableStateOf("") }
    var sample by remember { mutableStateOf("") }
    val profiles by vm.banks.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("بانک‌ها و تشخیص پیامک", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("بانک را انتخاب کن یا نام بانک جدید را دستی وارد کن. می‌توانی آخرین پیامک تراکنش را هم کپی کنی تا الگوی بانک ذخیره شود.", color = Color(0xFF9CA3AF))
                OutlinedTextField(customBank, { customBank = it }, label = { Text("نام بانک") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sample, { sample = it }, label = { Text("آخرین پیامک تراکنش (اختیاری)") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (customBank.isNotBlank()) {
                        vm.saveBank(customBank.trim(), sample)
                        Toast.makeText(context, "بانک ذخیره شد", Toast.LENGTH_SHORT).show()
                        customBank = ""; sample = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره بانک") }
            }
        }
        Text("بانک‌های شناخته‌شده", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        vm.detectedBanks().forEach { name ->
            AssistChip(onClick = { customBank = name }, label = { Text(name) }, leadingIcon = { Icon(Icons.Default.AccountBalance, null) })
        }
        if (profiles.isNotEmpty()) {
            Text("بانک‌های شخصی‌سازی‌شده", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
            profiles.forEach { p ->
                ListItem(headlineContent = { Text(p.bankName) }, supportingContent = { Text(if (p.sampleSms.isBlank()) "نمونه پیامک ثبت نشده" else "نمونه پیامک ذخیره شده") })
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26))) {
            Column(Modifier.padding(20.dp)) {
                Text("واحد پول", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("نمایش مبالغ در کل برنامه", color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(vm.currency == "تومان", { vm.currency = "تومان" }, label = { Text("تومان") })
                    FilterChip(vm.currency == "ریال", { vm.currency = "ریال" }, label = { Text("ریال") })
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26))) {
            Column(Modifier.padding(20.dp)) {
                Text("دسترسی پیامک", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("برای تشخیص خودکار تراکنش‌ها باید اجازه دریافت پیامک فعال باشد.", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun formatMoney(amount: Long, currency: String): String {
    val value = if (currency == "تومان") amount / 10 else amount
    return "${DecimalFormat("#,###").format(value)} $currency"
}
private fun dateText(ms: Long): String = SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(ms))
