package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiAdminUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "buyer",
    val status: String = "active",
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("managed_city") val managedCity: String? = null
)

@Serializable
data class ApiAdminUsersResponse(val users: List<ApiAdminUser>)

@Serializable
data class ApiOnlineUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "buyer",
    val avatar: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("seconds_ago") val secondsAgo: Int = 0
)

@Serializable
data class ApiOnlineUsersResponse(
    val success: Boolean = false,
    @SerialName("online_users") val onlineUsers: List<ApiOnlineUser> = emptyList(),
    @SerialName("total_online") val totalOnline: Int = 0
)

@Serializable
data class ApiAdminShop(
    val id: Int = 0,
    val name: String = "",
    val logo: String = "",
    val location: String = "",
    val phone: String = "",
    @SerialName("vendor_name") val vendorName: String = "",
    @SerialName("vendor_email") val vendorEmail: String = "",
    @SerialName("vendor_phone") val vendorPhone: String = "",
    val category: String = "",
    val status: String = "active",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("total_sales") val totalSales: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ApiAdminShopsResponse(val shops: List<ApiAdminShop>)

@Serializable
data class ApiSuperAdminResponse(
    val stats: ApiSuperStats,
    val reports: List<ApiReport>,
    val config: ApiSystemConfig
)

@Serializable
data class ApiSuperStats(
    val users: List<ApiStatItem>,
    val shops: List<ApiStatItem>,
    val products: List<ApiStatItem>,
    val orders: List<ApiStatItem>,
    val revenue: List<ApiRevenueItem>
)

@Serializable
data class ApiStatItem(
    val role: String? = null,
    val status: String? = null,
    @SerialName("is_verified") val isVerified: Int? = null,
    @SerialName("is_active") val isActive: Int? = null,
    val count: Int
)

@Serializable
data class ApiRevenueItem(
    val month: String,
    val total: Double
)

@Serializable
data class ApiReport(
    val id: Int,
    @SerialName("reporter_id") val reporterId: Int,
    @SerialName("reporter_name") val reporterName: String,
    val type: String,
    @SerialName("target_id") val targetId: Int,
    val reason: String,
    val comment: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiSystemConfig(
    @SerialName("maintenance_mode") val maintenanceMode: Boolean,
    @SerialName("app_version") val appVersion: String,
    @SerialName("min_version") val minVersion: String,
    @SerialName("commission_rate") val commissionRate: Double
)

@Serializable
data class ApiAdminDashboardKpis(
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("total_vendors") val totalVendors: Int = 0,
    @SerialName("online_users") val onlineUsers: Int = 0,
    @SerialName("new_users_30d") val newUsers30d: Int = 0,
    @SerialName("total_shops") val totalShops: Int = 0,
    @SerialName("pending_shops") val pendingShops: Int = 0,
    @SerialName("banned_shops") val bannedShops: Int = 0,
    @SerialName("total_products") val totalProducts: Int = 0,
    @SerialName("total_orders") val totalOrders: Int = 0,
    @SerialName("orders_today") val ordersToday: Int = 0,
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("revenue_today") val revenueToday: Double = 0.0
)

@Serializable
data class ApiAdminDashboardDay(
    val day: String = "",
    val count: Int = 0
)

@Serializable
data class ApiAdminDashboardMonth(
    val month: String = "",
    val revenue: Double = 0.0
)

@Serializable
data class ApiAdminDashboardTopVendor(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    @SerialName("shop_id") val shopId: Int = 0,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("order_count") val orderCount: Int = 0,
    val revenue: Double = 0.0
)

@Serializable
data class ApiAdminDashboardTopProduct(
    val id: Int = 0,
    val title: String = "",
    val price: Double = 0.0,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("total_sold") val totalSold: Int = 0,
    @SerialName("total_generated") val totalGenerated: Double = 0.0
)

@Serializable
data class ApiAdminDashboardResponse(
    val success: Boolean = false,
    val kpis: ApiAdminDashboardKpis = ApiAdminDashboardKpis(),
    val registrations: List<ApiAdminDashboardDay> = emptyList(),
    @SerialName("monthly_revenue") val monthlyRevenue: List<ApiAdminDashboardMonth> = emptyList(),
    @SerialName("top_vendors") val topVendors: List<ApiAdminDashboardTopVendor> = emptyList(),
    @SerialName("top_products") val topProducts: List<ApiAdminDashboardTopProduct> = emptyList(),
    @SerialName("orders_by_status") val ordersByStatus: Map<String, Int> = emptyMap(),
    @SerialName("users_by_role") val usersByRole: Map<String, Int> = emptyMap()
)
