package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Simple local model representing a Farm Product
data class FarmProduct(
    val id: String,
    val name: String,
    val description: String,
    val imageResId: Int,
    val defaultPricePlaceholder: String,
    val unit: String,
    val category: String,
    val rating: Double,
    val reviewsCount: Int
)

data class FarmOrder(
    val id: String,
    val customerUsername: String,
    val itemsSummary: String,
    val totalPrice: String,
    val status: String, // Pending, Processing, Dispatched, Delivered
    val timestamp: Long
)

// Simple robust custom serialization for offline reliability
fun saveOrders(sharedPref: android.content.SharedPreferences, orders: List<FarmOrder>) {
    val encoded = orders.joinToString("###") { order ->
        val safeSummary = order.itemsSummary.replace("\n", "[NEWLINE]").replace("|", "[PIPE]")
        "${order.id}|${order.customerUsername}|$safeSummary|${order.totalPrice}|${order.status}|${order.timestamp}"
    }
    sharedPref.edit().putString("persisted_orders", encoded).apply()
}

fun loadOrders(sharedPref: android.content.SharedPreferences): List<FarmOrder> {
    val raw = sharedPref.getString("persisted_orders", "") ?: ""
    if (raw.isEmpty()) {
        return listOf(
            FarmOrder(
                id = "CF-8392",
                customerUsername = "customer",
                itemsSummary = "• 2 Litres Fresh Cow Milk\n• 1 Dozen Country Chicken Eggs",
                totalPrice = "320",
                status = "Dispatched",
                timestamp = System.currentTimeMillis() - 3600000
            )
        )
    }
    return try {
        raw.split("###").mapNotNull { part ->
            if (part.isEmpty()) return@mapNotNull null
            val fields = part.split("|")
            if (fields.size >= 6) {
                val decodedSummary = fields[2].replace("[NEWLINE]", "\n").replace("[PIPE]", "|")
                FarmOrder(
                    id = fields[0],
                    customerUsername = fields[1],
                    itemsSummary = decodedSummary,
                    totalPrice = fields[3],
                    status = fields[4],
                    timestamp = fields[5].toLongOrNull() ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// Simple robust custom serialization for offline reliability of products list
fun saveProducts(sharedPref: android.content.SharedPreferences, products: List<FarmProduct>) {
    val encoded = products.joinToString("###") { prod ->
        val safeDesc = prod.description.replace("\n", "[NEWLINE]").replace("|", "[PIPE]")
        "${prod.id}|${prod.name}|$safeDesc|${prod.imageResId}|${prod.defaultPricePlaceholder}|${prod.unit}|${prod.category}|${prod.rating}|${prod.reviewsCount}"
    }
    sharedPref.edit().putString("persisted_products", encoded).apply()
}

fun loadProducts(sharedPref: android.content.SharedPreferences): List<FarmProduct> {
    val raw = sharedPref.getString("persisted_products", "") ?: ""
    if (raw.isEmpty()) {
        return listOf(
            FarmProduct(
                id = "milk",
                name = "Fresh Cow Milk",
                description = "Pure, unadulterated, raw milk directly from our grass-fed cows. Chilled and delivered within hours.",
                imageResId = R.drawable.img_milk,
                defaultPricePlaceholder = "____ / Litre",
                unit = "Litre",
                category = "Milk",
                rating = 4.9,
                reviewsCount = 142
            ),
            FarmProduct(
                id = "chicken",
                name = "Country Chicken",
                description = "100% free-range country chicken. Raised on organic grains and greens. Tender and nutrient-dense.",
                imageResId = R.drawable.img_chicken,
                defaultPricePlaceholder = "____ / Kg",
                unit = "Kg",
                category = "Chicken",
                rating = 4.8,
                reviewsCount = 98
            ),
            FarmProduct(
                id = "duck",
                name = "Fresh Duck",
                description = "Plump, premium free-range farm ducks. Reared ethically near clean water ponds. Rich flavorful meat.",
                imageResId = R.drawable.img_duck,
                defaultPricePlaceholder = "____ / Kg",
                unit = "Kg",
                category = "Ducks",
                rating = 4.7,
                reviewsCount = 64
            ),
            FarmProduct(
                id = "eggs",
                name = "Country Chicken Eggs",
                description = "Freshly collected brown eggs with high yolk density. From healthy, free-roaming country hens.",
                imageResId = R.drawable.img_eggs,
                defaultPricePlaceholder = "____ / Dozen",
                unit = "Dozen",
                category = "Eggs",
                rating = 4.9,
                reviewsCount = 215
            )
        )
    }
    return try {
        raw.split("###").mapNotNull { part ->
            if (part.isEmpty()) return@mapNotNull null
            val fields = part.split("|")
            if (fields.size >= 9) {
                val decodedDesc = fields[2].replace("[NEWLINE]", "\n").replace("[PIPE]", "|")
                FarmProduct(
                    id = fields[0],
                    name = fields[1],
                    description = decodedDesc,
                    imageResId = fields[3].toIntOrNull() ?: 0,
                    defaultPricePlaceholder = fields[4],
                    unit = fields[5],
                    category = fields[6],
                    rating = fields[7].toDoubleOrNull() ?: 4.8,
                    reviewsCount = fields[8].toIntOrNull() ?: 12
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChinduFarmsApp()
            }
        }
    }
}

@Composable
fun ChinduFarmsApp() {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("ChinduFarmsPrefs", Context.MODE_PRIVATE) }
    
    // Core App State
    var currentTab by remember { mutableStateOf("Home") } // Home, Categories, Orders, Wishlist, Profile
    var searchQueries by remember { mutableStateOf("") }
    
    // Cart State
    var cartItems by remember { mutableStateOf(mapOf<String, Int>()) } // ProductID -> Quantity
    var activeFilterCategory by remember { mutableStateOf("All") }
    
    // Wishlist State (persisted simply in memory)
    var wishlist by remember { mutableStateOf(setOf<String>()) }
    
    // Editable Prices (Persisted in SharedPreferences)
    var milkPrice by remember { mutableStateOf(sharedPref.getString("price_milk", "") ?: "") }
    var chickenPrice by remember { mutableStateOf(sharedPref.getString("price_chicken", "") ?: "") }
    var duckPrice by remember { mutableStateOf(sharedPref.getString("price_duck", "") ?: "") }
    var eggsPrice by remember { mutableStateOf(sharedPref.getString("price_eggs", "") ?: "") }
    
    // Price Editing State
    var showEditPriceDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<FarmProduct?>(null) }
    var tempPriceText by remember { mutableStateOf("") }
    
    // Authentication States
    var isLoggedIn by remember { mutableStateOf(sharedPref.getBoolean("is_logged_in", false)) }
    var loggedInUsername by remember { mutableStateOf(sharedPref.getString("logged_in_username", "") ?: "") }
    var loggedInRole by remember { mutableStateOf(sharedPref.getString("logged_in_role", "") ?: "") }
    
    // Admin mode computed from role
    val isAdminMode = loggedInRole == "admin"
    
    // Orders List State (persisted in SharedPreferences)
    var ordersList by remember { mutableStateOf(loadOrders(sharedPref)) }
    
    // Confetti/Checkout Dialog state
    var showCheckoutSuccess by remember { mutableStateOf(false) }
    var showCartOverlay by remember { mutableStateOf(false) }

    // Dynamic Farm Products
    var productsList by remember { mutableStateOf(loadProducts(sharedPref)) }

    // Dynamic price mapping backing state
    var productPrices by remember(productsList) {
        mutableStateOf(
            productsList.associate { it.id to (sharedPref.getString("price_${it.id}", "") ?: "") }
        )
    }

    // Helper to get current custom price
    fun getProductPrice(productId: String): String {
        return productPrices[productId] ?: ""
    }

    // Save Price function
    fun saveProductPrice(productId: String, price: String) {
        sharedPref.edit().putString("price_$productId", price).apply()
        productPrices = productPrices + (productId to price)
        when (productId) {
            "milk" -> milkPrice = price
            "chicken" -> chickenPrice = price
            "duck" -> duckPrice = price
            "eggs" -> eggsPrice = price
        }
    }

    // Dynamic Product Creation states
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProdName by remember { mutableStateOf("") }
    var newProdDesc by remember { mutableStateOf("") }
    var newProdCategory by remember { mutableStateOf("Milk") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdUnit by remember { mutableStateOf("Kg") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ChinduBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        floatingActionButton = {
            if (isAdminMode && currentTab == "Home") {
                FloatingActionButton(
                    onClick = {
                        newProdName = ""
                        newProdDesc = ""
                        newProdCategory = "Milk"
                        newProdPrice = ""
                        newProdUnit = "Kg"
                        showAddProductDialog = true
                    },
                    containerColor = ForestGreen,
                    contentColor = PureWhite,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Product",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Harvest", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        containerColor = CreamyWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    "Home" -> HomeScreen(
                        products = productsList,
                        searchQuery = searchQueries,
                        onSearchChange = { searchQueries = it },
                        activeCategory = activeFilterCategory,
                        onCategorySelect = { activeFilterCategory = it },
                        getPrice = { getProductPrice(it.id) },
                        onEditPriceClick = { product ->
                            editingProduct = product
                            tempPriceText = getProductPrice(product.id)
                            showEditPriceDialog = true
                        },
                        wishlist = wishlist,
                        onToggleWishlist = { id ->
                            wishlist = if (wishlist.contains(id)) wishlist - id else wishlist + id
                        },
                        onAddToCart = { id ->
                            cartItems = cartItems.toMutableMap().apply {
                                put(id, (get(id) ?: 0) + 1)
                            }
                            Toast.makeText(context, "${productsList.find { it.id == id }?.name} added to basket", Toast.LENGTH_SHORT).show()
                        },
                        onCartOpen = { showCartOverlay = true },
                        cartCount = cartItems.values.sum(),
                        onProfileClick = { currentTab = "Profile" },
                        isAdminMode = isAdminMode
                    )
                    "Categories" -> CategoriesScreen(
                        products = productsList,
                        onSelectCategory = {
                            activeFilterCategory = it
                            currentTab = "Home"
                        }
                    )
                    "Orders" -> {
                        if (!isLoggedIn) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CreamyWhite)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(SoftSage),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Track Orders",
                                        tint = ForestGreen,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Track Your Farm Orders",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Log in or create a Chindu Farms account to view your purchase history, live order statuses, and delivery updates.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextLight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { currentTab = "Profile" },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Log In / Register", color = PureWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OrdersScreen(
                                orders = ordersList,
                                currentUser = loggedInUsername,
                                isAdmin = isAdminMode,
                                onUpdateOrderStatus = { orderId, newStatus ->
                                    val updated = ordersList.map {
                                        if (it.id == orderId) it.copy(status = newStatus) else it
                                    }
                                    ordersList = updated
                                    saveOrders(sharedPref, updated)
                                }
                            )
                        }
                    }
                    "Wishlist" -> WishlistScreen(
                        products = productsList,
                        wishlist = wishlist,
                        getPrice = { getProductPrice(it.id) },
                        onAddToCart = { id ->
                            cartItems = cartItems.toMutableMap().apply {
                                put(id, (get(id) ?: 0) + 1)
                            }
                        },
                        onRemoveFromWishlist = { id -> wishlist = wishlist - id }
                    )
                    "Profile" -> {
                        if (!isLoggedIn) {
                            LoginScreen(
                                onLoginSuccess = { username, role ->
                                    loggedInUsername = username
                                    loggedInRole = role
                                    isLoggedIn = true
                                    sharedPref.edit().apply {
                                        putBoolean("is_logged_in", true)
                                        putString("logged_in_username", username)
                                        putString("logged_in_role", role)
                                    }.apply()
                                    Toast.makeText(context, "Logged In as ${username.replaceFirstChar { it.uppercase() }}! 🎉", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            ProfileScreen(
                                milkPrice = milkPrice,
                                chickenPrice = chickenPrice,
                                duckPrice = duckPrice,
                                eggsPrice = eggsPrice,
                                onUpdatePrice = { id, newVal -> saveProductPrice(id, newVal) },
                                loggedInUsername = loggedInUsername,
                                loggedInRole = loggedInRole,
                                onLogout = {
                                    isLoggedIn = false
                                    loggedInUsername = ""
                                    loggedInRole = ""
                                    currentTab = "Home"
                                    sharedPref.edit().apply {
                                        putBoolean("is_logged_in", false)
                                        putString("logged_in_username", "")
                                        putString("logged_in_role", "")
                                    }.apply()
                                    Toast.makeText(context, "Logged Out 🔒", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Edit Price Dialog
            if (showEditPriceDialog && editingProduct != null) {
                AlertDialog(
                    onDismissRequest = { showEditPriceDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = PureWhite,
                    title = {
                        Column {
                            Text(
                                text = "Set Selling Price",
                                style = MaterialTheme.typography.titleLarge,
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = editingProduct!!.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextLight
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = tempPriceText,
                                onValueChange = { tempPriceText = it },
                                label = { Text("Price in Rupees (₹)") },
                                placeholder = { Text("e.g. 75, 240, 310") },
                                leadingIcon = {
                                    Text(
                                        text = "₹",
                                        style = TextStyle(
                                            color = GoldenSun,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    )
                                },
                                suffix = {
                                    Text(
                                        text = "/ ${editingProduct!!.unit}",
                                        color = TextLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SoftSage.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedIndicatorColor = EmeraldGreen,
                                    unfocusedIndicatorColor = CardBorder
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Leave blank to display the manual placeholder: ₹____ / ${editingProduct!!.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight,
                                modifier = Modifier.alpha(0.8f)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                saveProductPrice(editingProduct!!.id, tempPriceText.trim())
                                showEditPriceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Price", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditPriceDialog = false }) {
                            Text("Cancel", color = TextLight)
                        }
                    }
                )
            }

            // Cart Bottom Sheet Overlay
            if (showCartOverlay) {
                CartOverlayScreen(
                    products = productsList,
                    cartItems = cartItems,
                    getPrice = { getProductPrice(it.id) },
                    onClose = { showCartOverlay = false },
                    onUpdateQuantity = { id, qty ->
                        cartItems = cartItems.toMutableMap().apply {
                            if (qty <= 0) remove(id) else put(id, qty)
                        }
                    },
                    onCheckout = {
                        if (!isLoggedIn) {
                            showCartOverlay = false
                            currentTab = "Profile"
                            Toast.makeText(context, "Please log in or sign up to complete your checkout! 🔑", Toast.LENGTH_LONG).show()
                        } else {
                            val itemsSummary = cartItems.map { (prodId, qty) ->
                                val prod = productsList.find { it.id == prodId }
                                val prodName = prod?.name ?: "Farm Item"
                                "• $qty x $prodName"
                            }.joinToString("\n")
                            
                            val totalPrice = cartItems.entries.sumOf { (prodId, qty) ->
                                val priceStr = getProductPrice(prodId)
                                val numericPrice = priceStr.toDoubleOrNull() ?: 100.0
                                numericPrice * qty
                            }
                            
                            val newOrder = FarmOrder(
                                id = "CF-${(1000..9999).random()}",
                                customerUsername = loggedInUsername,
                                itemsSummary = itemsSummary,
                                totalPrice = totalPrice.toInt().toString(),
                                status = "Pending",
                                timestamp = System.currentTimeMillis()
                            )
                            
                            val updatedList = ordersList + newOrder
                            ordersList = updatedList
                            saveOrders(sharedPref, updatedList)
                            
                            cartItems = emptyMap()
                            showCartOverlay = false
                            showCheckoutSuccess = true
                        }
                    }
                )
            }

            // Checkout Success Celebratory Dialog
            if (showCheckoutSuccess) {
                AlertDialog(
                    onDismissRequest = { showCheckoutSuccess = false },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = PureWhite,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SoftSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = ForestGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Order Placed Successfully!",
                            style = MaterialTheme.typography.titleLarge,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = "Thank you for supporting sustainable farming! Your organic harvest is being hand-picked. Chindu Farms will deliver it fresh at your doorstep by tomorrow morning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCheckoutSuccess = false
                                currentTab = "Orders"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Track Fresh Order", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Dynamic Custom Product Addition Dialog
            if (showAddProductDialog) {
                AlertDialog(
                    onDismissRequest = { showAddProductDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = PureWhite,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Add Item",
                                tint = ForestGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Add Farm Harvest",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Add a fresh item or dynamic category to the Chindu Farms live directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextLight,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            OutlinedTextField(
                                value = newProdName,
                                onValueChange = { newProdName = it },
                                label = { Text("Product Name") },
                                placeholder = { Text("e.g. Premium Buffalo Ghee") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SoftSage.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedIndicatorColor = ForestGreen,
                                    unfocusedIndicatorColor = CardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newProdDesc,
                                onValueChange = { newProdDesc = it },
                                label = { Text("Product Description") },
                                placeholder = { Text("e.g. 100% pure organic ghee...") },
                                maxLines = 3,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SoftSage.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedIndicatorColor = ForestGreen,
                                    unfocusedIndicatorColor = CardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newProdPrice,
                                    onValueChange = { newProdPrice = it },
                                    label = { Text("Price (₹)") },
                                    placeholder = { Text("650") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = SoftSage.copy(alpha = 0.15f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = TextDark,
                                        unfocusedTextColor = TextDark,
                                        focusedIndicatorColor = ForestGreen,
                                        unfocusedIndicatorColor = CardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = newProdUnit,
                                    onValueChange = { newProdUnit = it },
                                    label = { Text("Unit") },
                                    placeholder = { Text("Kg / Litre") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = SoftSage.copy(alpha = 0.15f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = TextDark,
                                        unfocusedTextColor = TextDark,
                                        focusedIndicatorColor = ForestGreen,
                                        unfocusedIndicatorColor = CardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = newProdCategory,
                                onValueChange = { newProdCategory = it },
                                label = { Text("Department (Category)") },
                                placeholder = { Text("e.g. Milk, Chicken, Ghee, Veggies") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SoftSage.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedIndicatorColor = ForestGreen,
                                    unfocusedIndicatorColor = CardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val name = newProdName.trim()
                                val desc = newProdDesc.trim()
                                val cat = newProdCategory.trim()
                                val price = newProdPrice.trim()
                                val unit = newProdUnit.trim()

                                if (name.isEmpty() || desc.isEmpty() || cat.isEmpty() || price.isEmpty() || unit.isEmpty()) {
                                    Toast.makeText(context, "All fields are required!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val numericPrice = price.toDoubleOrNull()
                                if (numericPrice == null || numericPrice <= 0) {
                                    Toast.makeText(context, "Please enter a valid price!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Generate unique dynamic ID
                                val newId = "custom_${name.lowercase().replace(" ", "_")}_${System.currentTimeMillis() % 10000}"
                                val newProduct = FarmProduct(
                                    id = newId,
                                    name = name,
                                    description = desc,
                                    imageResId = 0, // fallback vector/icon representation
                                    defaultPricePlaceholder = "____ / $unit",
                                    unit = unit,
                                    category = cat,
                                    rating = 4.8,
                                    reviewsCount = 1
                                )

                                // Update dynamic products list & save
                                val updated = productsList + newProduct
                                productsList = updated
                                saveProducts(sharedPref, updated)

                                // Save the custom price
                                saveProductPrice(newId, price)

                                showAddProductDialog = false
                                Toast.makeText(context, "$name Added to Chindu Farms! 🌾", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Publish Live", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddProductDialog = false }) {
                            Text("Cancel", color = TextLight)
                        }
                    }
                )
            }
        }
    }
}

// ==================== HOME TAB SCREEN ====================
@Composable
fun HomeScreen(
    products: List<FarmProduct>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    activeCategory: String,
    onCategorySelect: (String) -> Unit,
    getPrice: (FarmProduct) -> String,
    onEditPriceClick: (FarmProduct) -> Unit,
    wishlist: Set<String>,
    onToggleWishlist: (String) -> Unit,
    onAddToCart: (String) -> Unit,
    onCartOpen: () -> Unit,
    cartCount: Int,
    onProfileClick: () -> Unit,
    isAdminMode: Boolean = false
) {
    val filteredProducts = products.filter {
        val matchesCategory = activeCategory == "All" || it.category.equals(activeCategory, ignoreCase = true)
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) || 
                            it.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Editorial Header Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamyWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForestGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            color = PureWhite,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Chindu Farms",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ForestGreen,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                // Header Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Basket / Shopping Cart Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .clickable { onCartOpen() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Shopping Cart",
                            tint = ForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        if (cartCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-1).dp, y = 1.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(GoldenAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cartCount.toString(),
                                    color = PureWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // User Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Profile",
                            tint = ForestGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { 
                    Text(
                        "Search organic milk, eggs, chicken...", 
                        color = TextLight, 
                        fontSize = 14.sp
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = EmeraldGreen
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedIndicatorColor = SoftSage,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(28.dp), ambientColor = SoftSage, spotColor = ForestGreen),
                singleLine = true
            )
        }

        // Main Content Area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Hero Banner styled with the Editorial gradient blend & serif typography
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ForestGreen, EmeraldGreen, GoldenAmber)
                            )
                        )
                        .shadow(2.dp, RoundedCornerShape(24.dp))
                ) {
                    // Loaded Hero Image from Drawable with low opacity to blend with gradient
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Beautiful organic farm backdrop",
                        modifier = Modifier.fillMaxSize().alpha(0.35f),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Premium high-contrast gradient scrim overlay for maximum legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )
                    
                    // Editorial Text Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "FARM FRESH • PURE QUALITY",
                            color = GoldenSun,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Morning Harvest,\nDelivered Daily.",
                            color = PureWhite,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Trust Badges Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        TrustBadge(title = "Farm Fresh", icon = Icons.Default.Check, tint = ForestGreen)
                    }
                    item {
                        TrustBadge(title = "100% Natural", icon = Icons.Default.Star, tint = GoldenSun)
                    }
                    item {
                        TrustBadge(title = "No Chemicals", icon = Icons.Default.Info, tint = Color(0xFFE07A5F))
                    }
                    item {
                        TrustBadge(title = "Daily Fresh Delivery", icon = Icons.Default.Place, tint = EmeraldGreen)
                    }
                }
            }

            // Small Categories Grid
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Browse Fresh Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    val categories = listOf("All", "Milk", "Chicken", "Ducks", "Eggs")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = activeCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) ForestGreen else SoftSage)
                                    .clickable { onCategorySelect(cat) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GoldenSun else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val catEmoji = when (cat) {
                                        "Milk" -> "🐄"
                                        "Chicken" -> "🐔"
                                        "Ducks" -> "🦆"
                                        "Eggs" -> "🥚"
                                        else -> "🌿"
                                    }
                                    Text(
                                        text = catEmoji,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat,
                                        color = if (isSelected) PureWhite else TextDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Products Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Organic Harvest Today",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    Text(
                        text = "${filteredProducts.size} Items Available",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Products list rendering in beautiful Glassmorphic cards
            if (filteredProducts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No results",
                            tint = TextLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No farm products match your search",
                            color = TextLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredProducts.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (product in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProductCard(
                                    product = product,
                                    priceVal = getPrice(product),
                                    onEditPriceClick = { onEditPriceClick(product) },
                                    isFavorite = wishlist.contains(product.id),
                                    onToggleFavorite = { onToggleWishlist(product.id) },
                                    onAddToCart = { onAddToCart(product.id) },
                                    isAdminMode = isAdminMode
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

// ==================== COMPONENT: TRUST BADGE ====================
@Composable
fun TrustBadge(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PureWhite)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = TextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==================== COMPONENT: GLASSMORPHIC PRODUCT CARD ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCard(
    product: FarmProduct,
    priceVal: String,
    onEditPriceClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToCart: () -> Unit,
    isAdminMode: Boolean = false
) {
    val deptBgColor = when (product.id) {
        "milk" -> SoftSage
        "chicken" -> ChickenBg
        "duck" -> DuckBg
        "eggs" -> EggsBg
        else -> SoftSage
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = ForestGreen.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            // Image frame with department backdrop and heart icon overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(deptBgColor)
            ) {
                if (product.imageResId != 0) {
                    Image(
                        painter = painterResource(id = product.imageResId),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(deptBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (product.category) {
                                "Milk" -> "🥛"
                                "Chicken" -> "🍗"
                                "Ducks" -> "🦆"
                                "Eggs" -> "🥚"
                                else -> "🌾"
                            },
                            fontSize = 44.sp
                        )
                    }
                }
                
                // Favorite Heart Overlay
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .background(PureWhite.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Wishlist",
                        tint = if (isFavorite) Color.Red else ForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Category Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ForestGreen.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.category.uppercase(),
                        color = PureWhite,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Product Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = GoldenSun,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${product.rating} (${product.reviewsCount})",
                        color = TextLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = product.description,
                    color = TextLight,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .alpha(0.85f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price Editable Area (Rupee Symbol, Value or Underscores)
                Row(
                    modifier = if (isAdminMode) {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(deptBgColor.copy(alpha = 0.45f))
                            .clickable { onEditPriceClick() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(deptBgColor.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPriceSet = priceVal.isNotEmpty()
                        Text(
                            text = if (isPriceSet) "₹$priceVal" else "₹____",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (isPriceSet) TextDark else TextLight
                            )
                        )
                        
                        Text(
                            text = " / ${product.unit}",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.5.sp,
                                color = TextLight
                            )
                        )
                    }

                    if (isAdminMode) {
                        // Simple pencil edit button
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Price",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buy Now / Add to Cart Button
                Button(
                    onClick = onAddToCart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth().height(34.dp)
                ) {
                    Text(
                        text = "BUY NOW",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ==================== CATEGORIES TAB SCREEN ====================
@Composable
fun CategoriesScreen(products: List<FarmProduct>, onSelectCategory: (String) -> Unit) {
    val categoryList = remember(products) {
        val baseList = mutableListOf(
            Pair("Milk", "Fresh cow & buffalo milk, cream, pure organic ghee."),
            Pair("Chicken", "Free-range country chicken meat, organic country breeds."),
            Pair("Ducks", "Plump pond-reared ducks, ethically harvested and dressed."),
            Pair("Eggs", "Farm collected brown country eggs, nutrient dense.")
        )
        val baseNames = baseList.map { it.first.lowercase() }.toSet()
        products.forEach { prod ->
            val catName = prod.category
            if (catName.isNotEmpty() && !baseNames.contains(catName.lowercase())) {
                baseList.add(Pair(catName, "Fresh organic harvests of farm-sourced ${catName.lowercase()}."))
            }
        }
        baseList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamyWhite)
            .padding(16.dp)
    ) {
        Text(
            text = "Farm Departments",
            fontFamily = FontFamily.SansSerif,
            style = MaterialTheme.typography.headlineMedium,
            color = ForestGreen,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select a department to view organic harvests",
            color = TextLight,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categoryList) { item ->
                val (cat, desc) = item
                val deptBgColor = when (cat) {
                    "Milk" -> SoftSage
                    "Chicken" -> ChickenBg
                    "Ducks" -> DuckBg
                    "Eggs" -> EggsBg
                    else -> SoftSage
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCategory(cat) }
                        .shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = SoftSage),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(deptBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val catEmoji = when (cat) {
                                "Milk" -> "🐄"
                                "Chicken" -> "🐔"
                                "Ducks" -> "🦆"
                                "Eggs" -> "🥚"
                                else -> "🌿"
                            }
                            Text(
                                text = catEmoji,
                                fontSize = 20.sp
                            )
                        }
                        
                        Column {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                color = TextLight,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== ORDERS TAB SCREEN ====================
@Composable
fun OrdersScreen(
    orders: List<FarmOrder>,
    currentUser: String,
    isAdmin: Boolean,
    onUpdateOrderStatus: (String, String) -> Unit
) {
    val displayOrders = remember(orders, currentUser, isAdmin) {
        if (isAdmin) orders.sortedByDescending { it.timestamp }
        else orders.filter { it.customerUsername.equals(currentUser, ignoreCase = true) }.sortedByDescending { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamyWhite)
            .padding(16.dp)
    ) {
        Text(
            text = if (isAdmin) "Admin Order Desk" else "Your Farm Deliveries",
            fontFamily = FontFamily.SansSerif,
            style = MaterialTheme.typography.headlineMedium,
            color = ForestGreen,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isAdmin) "Manage and update live customer dispatch logs below." else "Direct delivery from Chindu Farms organic pastures to you.",
            color = TextLight,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (displayOrders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "No Orders",
                    tint = TextLight,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isAdmin) "No orders received yet." else "No farm orders placed yet.",
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (isAdmin) "Customer checkouts will populate this console." else "Browse our fresh selections and place your first order!",
                    color = TextLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayOrders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = SoftSage),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Order header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SoftSage)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = order.id,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "by @${order.customerUsername}",
                                            color = TextLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                
                                val statusColor = when (order.status) {
                                    "Pending" -> Color(0xFFF57C00)
                                    "Processing" -> ForestGreen
                                    "Dispatched" -> GoldenSun
                                    "Delivered" -> EmeraldGreen
                                    else -> TextLight
                                }
                                
                                Text(
                                    text = order.status,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Items Ordered:",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 13.sp
                            )
                            Text(
                                text = order.itemsSummary,
                                color = TextLight,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Price:",
                                    color = TextLight,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "₹${order.totalPrice}",
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                            if (isAdmin) {
                                // Admin status update buttons
                                Text(
                                    text = "Update Dispatch Progress:",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (order.status == "Pending") {
                                        Button(
                                            onClick = { onUpdateOrderStatus(order.id, "Processing") },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Process", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (order.status == "Processing") {
                                        Button(
                                            onClick = { onUpdateOrderStatus(order.id, "Dispatched") },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldenSun),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        }
                                    }
                                    if (order.status == "Dispatched") {
                                        Button(
                                            onClick = { onUpdateOrderStatus(order.id, "Delivered") },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (order.status == "Delivered") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SoftSage)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Completed & Delivered Fresh ✓",
                                                color = ForestGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Customer tracking timeline
                                Text(
                                    text = "Delivery Progress",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val isHarvested = order.status in listOf("Pending", "Processing", "Dispatched", "Delivered")
                                val isChecked = order.status in listOf("Processing", "Dispatched", "Delivered")
                                val isOnWay = order.status in listOf("Dispatched", "Delivered")
                                val isDelivered = order.status == "Delivered"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TimelineItem("Pending", isHarvested)
                                    TimelineItem("Processing", isChecked)
                                    TimelineItem("Dispatched", isOnWay)
                                    TimelineItem("Delivered", isDelivered)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                                // Delivery partner info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(SoftSage),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Rider",
                                                tint = ForestGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Ramesh Kumar",
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Your Farm Valet",
                                                color = TextLight,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { /* Phone dial trigger */ },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftSage),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = ForestGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", color = ForestGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineItem(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (active) ForestGreen else CardBorder),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoldenSun)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) TextDark else TextLight
        )
    }
}

// ==================== WISHLIST TAB SCREEN ====================
@Composable
fun WishlistScreen(
    products: List<FarmProduct>,
    wishlist: Set<String>,
    getPrice: (FarmProduct) -> String,
    onAddToCart: (String) -> Unit,
    onRemoveFromWishlist: (String) -> Unit
) {
    val favProducts = products.filter { wishlist.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamyWhite)
            .padding(16.dp)
    ) {
        Text(
            text = "Your Farm Wishlist",
            fontFamily = FontFamily.SansSerif,
            style = MaterialTheme.typography.headlineMedium,
            color = ForestGreen,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Products you have selected for direct organic delivery.",
            color = TextLight,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favProducts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Empty Wishlist",
                    tint = TextLight,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No farm items wishlisted yet.",
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Go to the Home tab and tap the heart icon to save products.",
                    color = TextLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favProducts) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (product.imageResId != 0) {
                                Image(
                                    painter = painterResource(id = product.imageResId),
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SoftSage),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (product.category) {
                                            "Milk" -> "🥛"
                                            "Chicken" -> "🍗"
                                            "Ducks" -> "🦆"
                                            "Eggs" -> "🥚"
                                            else -> "🌾"
                                        },
                                        fontSize = 32.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${getPrice(product).ifEmpty { "____" }} / ${product.unit}",
                                    color = GoldenSun,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Row {
                                IconButton(onClick = { onAddToCart(product.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Buy Now",
                                        tint = ForestGreen
                                    )
                                }
                                IconButton(onClick = { onRemoveFromWishlist(product.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFE07A5F)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== PROFILE & ADMIN TAB SCREEN ====================
@Composable
fun ProfileScreen(
    milkPrice: String,
    chickenPrice: String,
    duckPrice: String,
    eggsPrice: String,
    onUpdatePrice: (String, String) -> Unit,
    loggedInUsername: String,
    loggedInRole: String,
    onLogout: () -> Unit
) {
    var editMilk by remember { mutableStateOf(milkPrice) }
    var editChicken by remember { mutableStateOf(chickenPrice) }
    var editDuck by remember { mutableStateOf(duckPrice) }
    var editEggs by remember { mutableStateOf(eggsPrice) }

    val isAdminMode = loggedInRole.equals("admin", ignoreCase = true)

    // Sync state if external changes happen
    LaunchedEffect(milkPrice, chickenPrice, duckPrice, eggsPrice) {
        editMilk = milkPrice
        editChicken = chickenPrice
        editDuck = duckPrice
        editEggs = eggsPrice
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamyWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "My Profile",
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your fresh farm account and settings.",
                    color = TextLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Account Details Card with Dynamic Credentials and Logout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(SoftSage),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = loggedInUsername.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                fontSize = 20.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loggedInUsername.replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 18.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isAdminMode) ForestGreen else SoftSage)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isAdminMode) "ADMIN PANEL" else "CUSTOMER ACCOUNT",
                                        color = if (isAdminMode) PureWhite else ForestGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out of Account", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Consolidated Prices Admin Dashboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = SoftSage),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Farmer's Control Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (!isAdminMode) {
                        // Restricted Section Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF9E6))
                                .border(1.dp, Color(0xFFFFE0B2), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Restricted Access",
                                    tint = Color(0xFFF57C00),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pricing Controls Restricted",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Only Chindu Farm administrators can modify live selling prices. Log in to an admin account to proceed.",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Display Current Price Summary (Read-Only Preview)
                        Text(
                            text = "Current Active Prices (Read-Only Preview):",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CreamyWhite)
                                .padding(12.dp)
                        ) {
                            PriceSummaryRow("Fresh Cow Milk", milkPrice.ifEmpty { "Not Set" }, "Litre")
                            PriceSummaryRow("Country Chicken", chickenPrice.ifEmpty { "Not Set" }, "Kg")
                            PriceSummaryRow("Fresh Duck", duckPrice.ifEmpty { "Not Set" }, "Kg")
                            PriceSummaryRow("Country Eggs", eggsPrice.ifEmpty { "Not Set" }, "Dozen")
                        }
                    } else {
                        // Authorized Section - Editable Inputs
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftSage.copy(alpha = 0.3f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Admin Access Active. Set prices below:",
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Price Input: Milk
                        OutlinedPriceInputField(
                            label = "Fresh Cow Milk (Litre)",
                            value = editMilk,
                            onValueChange = {
                                editMilk = it
                                onUpdatePrice("milk", it)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Price Input: Chicken
                        OutlinedPriceInputField(
                            label = "Country Chicken (Kg)",
                            value = editChicken,
                            onValueChange = {
                                editChicken = it
                                onUpdatePrice("chicken", it)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Price Input: Duck
                        OutlinedPriceInputField(
                            label = "Fresh Duck (Kg)",
                            value = editDuck,
                            onValueChange = {
                                editDuck = it
                                onUpdatePrice("duck", it)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Price Input: Eggs
                        OutlinedPriceInputField(
                            label = "Country Eggs (Dozen)",
                            value = editEggs,
                            onValueChange = {
                                editEggs = it
                                onUpdatePrice("eggs", it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriceSummaryRow(label: String, price: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextLight, fontSize = 12.sp)
        Text(
            text = if (price == "") "Manual Placeholder" else "₹$price / $unit",
            color = if (price == "") TextLight else ForestGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun OutlinedPriceInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Enter manual price (e.g. 70)") },
            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = GoldenSun) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SoftSage.copy(alpha = 0.2f),
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = ForestGreen,
                unfocusedIndicatorColor = CardBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== CART SLIDE-UP OVERLAY SHEET ====================
@Composable
fun CartOverlayScreen(
    products: List<FarmProduct>,
    cartItems: Map<String, Int>,
    getPrice: (FarmProduct) -> String,
    onClose: () -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onCheckout: () -> Unit
) {
    val cartProducts = products.filter { cartItems.containsKey(it.id) }
    
    // Compute Dynamic Total if pricing is entered
    var dynamicTotal = 0.0
    var containsPlaceholders = false
    cartProducts.forEach { prod ->
        val priceStr = getPrice(prod)
        val qty = cartItems[prod.id] ?: 0
        val numericPrice = priceStr.toDoubleOrNull()
        if (numericPrice != null) {
            dynamicTotal += numericPrice * qty
        } else {
            containsPlaceholders = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() }
    ) {
        // Slide up modal container
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clickable(enabled = false) {}, // prevent click-through
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header of Cart sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Basket",
                            tint = ForestGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your Organic Basket",
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close",
                            tint = TextLight
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)

                if (cartProducts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Empty Basket",
                            tint = SoftSage,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your basket is empty",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Browse direct-from-farm milk, poultry, and eggs on the storefront to load your kitchen.",
                            color = TextLight,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cartProducts) { product ->
                            val qty = cartItems[product.id] ?: 0
                            val pVal = getPrice(product)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CreamyWhite),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (product.imageResId != 0) {
                                            Image(
                                                painter = painterResource(id = product.imageResId),
                                                contentDescription = product.name,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SoftSage),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = when (product.category) {
                                                        "Milk" -> "🥛"
                                                        "Chicken" -> "🍗"
                                                        "Ducks" -> "🦆"
                                                        "Eggs" -> "🥚"
                                                        else -> "🌾"
                                                    },
                                                    fontSize = 22.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = product.name,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark,
                                                fontSize = 13.sp
                                            )
                                            val numericPrice = pVal.toDoubleOrNull()
                                            if (numericPrice != null) {
                                                Text(
                                                    text = "₹$numericPrice / ${product.unit}",
                                                    color = GoldenSun,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "Price Placeholder",
                                                    color = TextLight,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    // Counter Row
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(product.id, qty - 1) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(PureWhite, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Minus",
                                                tint = ForestGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = qty.toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                        IconButton(
                                            onClick = { onUpdateQuantity(product.id, qty + 1) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(PureWhite, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Plus",
                                                tint = ForestGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Calculation Summary Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee", color = TextLight, fontSize = 13.sp)
                            Text("FREE (Direct Deliveries)", color = ForestGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                            
                            if (containsPlaceholders) {
                                Text(
                                    text = "₹____",
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            } else {
                                Text(
                                    text = "₹$dynamicTotal",
                                    color = GoldenSun,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        if (containsPlaceholders) {
                            Text(
                                text = "💡 Note: Setting manual prices in Profile/Control Dashboard will compute dynamic checkout totals.",
                                color = TextLight,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Button(
                            onClick = onCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Checkout Direct Delivery",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== PREMIUM BOTTOM NAVIGATION BAR ====================
@Composable
fun ChinduBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf("Home", "Categories", "Orders", "Wishlist", "Profile")
    
    NavigationBar(
        containerColor = PureWhite,
        tonalElevation = 8.dp,
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        items.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    val icon = when (tab) {
                        "Home" -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                        "Categories" -> if (isSelected) Icons.Filled.Star else Icons.Outlined.Star
                        "Orders" -> if (isSelected) Icons.Filled.List else Icons.Outlined.List
                        "Wishlist" -> if (isSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                        "Profile" -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
                        else -> Icons.Default.Home
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = tab,
                        tint = if (isSelected) ForestGreen else TextLight
                    )
                },
                label = {
                    Text(
                        text = tab,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ForestGreen else TextLight
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SoftSage
                )
            )
        }
    }
}

// ==================== AUTHENTICATION: LOGIN & SIGN UP SYSTEM ====================
@Composable
fun LoginScreen(
    onLoginSuccess: (username: String, role: String) -> Unit
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("ChinduFarmsPrefs", Context.MODE_PRIVATE) }
    
    var isSignUpMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var chosenRole by remember { mutableStateOf("customer") } // customer, admin
    
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamyWhite)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Branded Organic Hero Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SoftSage),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Chindu Farms",
                tint = ForestGreen,
                modifier = Modifier.size(44.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Chindu Farms",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = ForestGreen
        )
        
        Text(
            text = "Direct delivery of fresh cow milk, eggs & poultry",
            style = MaterialTheme.typography.bodyMedium,
            color = TextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Custom Segmented Switcher for Log In / Sign Up
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBorder.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val loginTabBg = if (!isSignUpMode) ForestGreen else Color.Transparent
            val loginTabTextColor = if (!isSignUpMode) PureWhite else TextDark
            val signUpTabBg = if (isSignUpMode) ForestGreen else Color.Transparent
            val signUpTabTextColor = if (isSignUpMode) PureWhite else TextDark
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(loginTabBg)
                    .clickable { isSignUpMode = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = loginTabTextColor
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(signUpTabBg)
                    .clickable { isSignUpMode = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = signUpTabTextColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Input Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isSignUpMode) "Register your credentials" else "Access your farm feed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight,
                    fontWeight = FontWeight.SemiBold
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ForestGreen) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftSage.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedIndicatorColor = ForestGreen,
                        unfocusedIndicatorColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreen) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftSage.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedIndicatorColor = ForestGreen,
                        unfocusedIndicatorColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isSignUpMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreen) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SoftSage.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedIndicatorColor = ForestGreen,
                            unfocusedIndicatorColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "Register Account Type:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Customer Radio
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { chosenRole = "customer" },
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (chosenRole == "customer") ForestGreen else CardBorder
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (chosenRole == "customer") SoftSage.copy(alpha = 0.25f) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                Text("Customer", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        
                        // Admin Radio
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { chosenRole = "admin" },
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (chosenRole == "admin") ForestGreen else CardBorder
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (chosenRole == "admin") SoftSage.copy(alpha = 0.25f) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                Text("Admin", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = {
                        val u = username.trim()
                        val p = password.trim()
                        
                        if (u.isEmpty() || p.isEmpty()) {
                            Toast.makeText(context, "Please enter username and password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (isSignUpMode) {
                            if (p != confirmPassword.trim()) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val success = registerUser(sharedPref, u, p, chosenRole)
                            if (success) {
                                Toast.makeText(context, "Registration Successful! Log in now.", Toast.LENGTH_SHORT).show()
                                isSignUpMode = false
                                confirmPassword = ""
                            } else {
                                Toast.makeText(context, "Username already exists!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val verifiedRole = verifyUser(sharedPref, u, p)
                            if (verifiedRole != null) {
                                onLoginSuccess(u, verifiedRole)
                            } else {
                                Toast.makeText(context, "Invalid username or password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSignUpMode) "Sign Up" else "Secure Log In",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Help note showing default admin credentials for convenience
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SoftSage.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "💡 Quick Access Defaults:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Admin: username 'admin' | password 'admin'\nCustomer: username 'customer' | password 'customer'",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

// User credentials persistence helper functions
fun registerUser(sharedPref: android.content.SharedPreferences, username: String, password: String, role: String): Boolean {
    val normUser = username.trim().lowercase()
    if (normUser.isEmpty() || password.trim().isEmpty()) return false
    val exists = sharedPref.getString("pwd_$normUser", null)
    if (exists != null || normUser == "admin") return false
    
    sharedPref.edit().apply {
        putString("pwd_$normUser", password.trim())
        putString("role_$normUser", role)
    }.apply()
    return true
}

fun verifyUser(sharedPref: android.content.SharedPreferences, username: String, password: String): String? {
    val normUser = username.trim().lowercase()
    val p = password.trim()
    if (normUser == "admin" && p == "admin") {
        return "admin"
    }
    if (normUser == "customer" && p == "customer") {
        return "customer"
    }
    val storedPwd = sharedPref.getString("pwd_$normUser", null)
    if (storedPwd != null && storedPwd == p) {
        return sharedPref.getString("role_$normUser", "customer")
    }
    return null
}

