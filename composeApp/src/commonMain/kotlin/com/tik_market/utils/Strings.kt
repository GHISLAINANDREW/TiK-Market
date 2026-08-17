package com.tik_market.utils

import androidx.compose.runtime.staticCompositionLocalOf

/** CompositionLocal exposing the app-wide strings for the current language. */
val LocalAppStrings = staticCompositionLocalOf<AppStrings> { getStrings("fr") }

/**
 * Multiplatform replacement for String.format()
 * Supports %s and %d placeholders.
 */
fun String.format(vararg args: Any?): String {
    if (args.isEmpty()) return this
    var res = this
    for (arg in args) {
        val sIdx = res.indexOf("%s")
        val dIdx = res.indexOf("%d")
        val finalIdx = if (sIdx != -1 && (dIdx == -1 || sIdx < dIdx)) sIdx else dIdx
        
        if (finalIdx != -1) {
            res = res.substring(0, finalIdx) + arg.toString() + res.substring(finalIdx + 2)
        }
    }
    return res
}

// ── SUB-CLASSES TO AVOID HUGE CONSTRUCTOR ──

data class AppBaseStrings(
    val appName: String, val home: String, val search: String, val cart: String, val profile: String,
    val messages: String, val notifications: String, val settings: String, val orders: String, val wishlist: String,
    val login: String, val register: String, val vendor: String, val shop: String, val seller: String,
    val loading: String, val loadingMore: String, val pullToRefresh: String, val error: String, val retry: String,
    val logout: String, val back: String, val confirm: String, val cancel: String, val submit: String,
    val account: String, val pressBackToExit: String, val language: String, val french: String, val english: String,
    val darkMode: String, val unknown: String, val myPosition: String, val openInMaps: String,
    val payment: String, val recap: String, val chooseOperator: String, val phoneNumber: String,
    val offlineBanner: String, val connectionRestored: String, val connectionLost: String, val sessionExpired: String
)

data class ProductActionStrings(
    val productDetail: String, val addToCart: String, val buyNow: String, val chat: String, val share: String,
    val report: String, val reportProduct: String, val category: String, val allCategories: String, val priceRange: String,
    val min: String, val max: String, val clear: String, val noProducts: String, val noReviews: String,
    val giveReview: String, val publishReview: String, val yourComment: String, val verifiedVendor: String,
    val whatsapp: String, val call: String, val reviews: String, val searchProducts: String
)

data class StatusStrings(
    val orderStatusPending: String, val orderStatusPaid: String, val orderStatusProcessing: String,
    val orderStatusShipped: String, val orderStatusDelivered: String, val orderStatusCancelled: String,
    val total: String, val items: String, val shippingAddress: String, val notes: String,
    val createOrder: String, val checkout: String
)

data class ChatMediaStrings(
    val send: String, val voice: String, val file: String, val image: String, val photo: String,
    val location: String, val emoji: String, val typeMessage: String, val noConversations: String
)

data class AuthStrings(
    val signIn: String, val signUp: String, val authOtpTitle: String, val phoneLogin: String, val loginContinue: String,
    val createYourAccount: String, val continueWithGoogle: String, val useEmailPassword: String, val or: String, val noAccountSignup: String,
    val alreadyAccountLogin: String, val createAccountTitle: String, val fullName: String, val phone: String, val email: String,
    val passwordLabel: String, val confirmPasswordLabel: String, val acceptTerms: String, val andPrivacy: String, val loginBtn: String,
    val signupBtn: String, val chooseYourCity: String, val cityLabel: String, val selectYourCity: String, val receiveCodeSms: String,
    val sentTo: String, val sixDigitCode: String, val codeValidFor: String, val verify: String, val resendCode: String,
    val changeNumber: String, val other: String, val sendCode: String, val chooseCityRequired: String, val nameRequired: String,
    val phoneRequired: String, val invalidPhone: String, val emailRequired: String, val invalidEmail: String, val passwordRequired: String,
    val passwordTooShort: String, val confirmRequired: String, val passwordsDontMatch: String, val acceptTermsRequired: String, val invalidNumberExample: String,
    val codeTooShort: String, val incorrectCode: String, val serverLoginFailed: String, val googleLoginFailed: String, val sendingError: String,
    val authErrorPrefix: String
)

data class CheckoutStrings(
    val cartTitle: String, val removeFromCart: String, val removeFromCartConfirm: String, val remove: String, val emptyCartTitle: String,
    val emptyCartHint: String, val finalizeOrder: String, val addressLabel: String, val contactPhone: String, val paymentMode: String,
    val payOnDelivery: String, val payOnDeliveryHint: String, val directToVendor: String, val directToVendorHint: String, val paymentInstructions: String,
    val transferInstructions: String, val amountToTransfer: String, val payAndOrder: String, val paymentMethod: String, val cashOnDelivery: String,
    val promoCode: String, val enterCode: String, val apply: String, val promoInvalid: String, val promoValidationError: String,
    val promoApplied: String, val articles: String, val subtotal: String, val discount: String, val deliveryFee: String,
    val instructions: String, val notesForVendor: String, val copy: String
)

data class OrderStrings(
    val myOrders: String, val noOrders: String, val noOrdersHint: String, val orderCancelled: String, val orderActionError: String,
    val receptionConfirmed: String, val cancelOrderTitle: String, val cancelOrderConfirmText: String, val confirmReceptionTitle: String, val confirmReceptionText: String,
    val estimatedDelivery: String, val waitingForVendor: String, val transferWait: String, val orderCreated: String, val paymentValidated: String,
    val orderConfirmed: String, val orderPreparing: String, val orderDelivering: String, val orderTracking: String, val awaitingSellerConfirmation: String,
    val confirmReception: String, val cancelOrder: String, val contactSeller: String, val no: String, val yes: String,
    val deleteConfirm: String, val delivery: String, val amount: String, val orderDelivered: String
)

data class HomeStrings(
    val registerShort: String, val products: String, val shops: String, val compareCta: String, val comparateur: String,
    val compareSelection: String, val compareEmptyHint: String, val compareEmptyHint2: String, val searchCompareHint: String, val addCompareHint: String,
    val alreadyAdded: String, val price: String, val stock: String, val units: String, val outOfStock: String,
    val bestPrice: String, val bestChoice: String, val optimalValue: String, val view: String, val arrivalsToday: String,
    val addStory: String, val addStoryHint: String, val video: String, val textOnly: String, val addCaption: String,
    val addCaptionHint: String, val yourMessage: String, val publish: String, val skip: String, val textStory: String,
    val whatDoYouWantToSay: String, val backgroundColor: String, val filters: String, val reset: String, val sortBy: String,
    val newest: String, val priceAsc: String, val priceDesc: String, val noProductsFound: String, val noProductsFoundHint: String,
    val favorites: String, val articlesCount: String, val add: String, val story: String
)

data class ProfileStrings(
    val myAccount: String, val logoutConfirmTitle: String, val logoutConfirmText: String, val defaultUser: String, val editProfile: String,
    val seeAll: String, val myWallet: String, val myServices: String, val connect: String, val loginRequiredHint: String,
    val points: String, val cashback: String, val level: String, val toPay: String, val toShip: String,
    val toReceive: String, val myFavorites: String, val followed: String, val coupons: String, val groupBuys: String,
    val vendorSpace: String, val vendorSpaceSubtitle: String, val admin: String, val adminSubtitle: String, val balance: String
)

data class ProductDetailStrings(
    val productDetails: String, val soldCount: String, val alreadyBought: String, val onlyLeft: String, val bestSeller: String,
    val color: String, val reviewsAndRatings: String, val similarProducts: String, val noSimilarProducts: String, val reportSent: String,
    val reportCommentPlaceholder: String, val close: String, val sendReport: String, val rating: String, val sales: String,
    val orderItemsCount: String, val backToOrders: String, val markAllRead: String
)

data class ChatNotifStrings(
    val deleteMessage: String, val deleteMessageConfirm: String, val you: String, val productLabel: String, val reply: String,
    val react: String, val reactToMessage: String, val shareLocation: String, val currentPosition: String, val map: String,
    val gallery: String, val camera: String, val localization: String, val messageCenter: String, val noMessages: String,
    val noResultsFor: String, val deleteConversation: String, val deleteConversationConfirm: String, val searchContacts: String
)

data class LoyaltyStrings(
    val loyaltyProgram: String, val dataUpdated: String, val redeemPoints: String, val recharge: String, val history: String,
    val myCoupons: String, val noTransactions: String, val noCoupons: String, val advantagesPerTier: String, val cardLabel: String,
    val cashbackBalanceLabel: String, val pointsUsable: String, val pointsAccumulated: String, val nextLevel: String, val pointsToReach: String,
    val earned: String, val spent: String, val rechargeLabel: String, val cashbackLabel: String, val bonusLabel: String, val refund: String,
    val fcfaDiscount: String, val pctDiscount: String, val expiresOn: String, val redeemMyPoints: String, val youHavePoints: String,
    val pointsToExchange: String, val pointsValue: String, val exchange: String, val rechargeWallet: String, val amountFcfa: String,
    val couponGenerated: String, val errorRedeem: String, val rechargeDone: String, val errorRecharge: String, val active: String
)

data class NotifPrefsStrings(
    val notifPrefsTitle: String, val pushNotifs: String, val pushNotifsDesc: String, val newProducts: String, val newProductsDesc: String,
    val orderUpdates: String, val orderUpdatesDesc: String, val promoOffers: String, val promoOffersDesc: String, val messagesToggle: String,
    val messagesToggleDesc: String, val systemToggle: String, val systemToggleDesc: String, val pushEnabled: String, val pushEnabledDesc: String,
    val prefsSaved: String, val errorSaving: String, val save: String
)

data class SettingsMiscStrings(
    val legalMentions: String, val termsOfUse: String, val downloads: String, val androidApk: String, val installApk: String,
    val iosApp: String, val comingSoon: String, val about: String, val next: String, val start: String,
    val allShops: String, val shopsIn: String, val noShopsFound: String, val follow: String, val followers: String,
    val followersCount: String, val verified: String, val shopNotFound: String, val sold: String, val noProductsForNow: String,
    val followShop: String, val unfollow: String, val featuredProducts: String, val customerReviews: String,
    val noStory: String, val storyDeleted: String, val storyError: String, val replyToSeller: String, val msgSentToSeller: String,
    val vendorNotFound: String, val delete: String, val version: String, val lastUpdate: String, val ourMissionTitle: String,
    val contactSupport: String, val allRights: String, val comparatif: String, val noProductsCompare: String, val unit: String,
    val inStock: String, val description: String, val shopsMapTitle: String, val searchShop: String, val shopClickTip: String,
    val mapOpensTip: String, val scanProduct: String, val cameraInit: String, val barcodeHint: String, val scanBarcodeTitle: String,
    val useCameraHint: String, val orTypeManually: String, val barcodeLabel: String, val barcodeExample: String, val searchAction: String,
    val errorPrefix: String, val noNotifications: String, val noNotificationsHint: String, val emptyFavorites: String, val emptyFavoritesHint: String,
    val negotiate: String, val paymentTitle: String, val amountToPay: String, val phoneNumberPrefix: String, val confirmPayment: String,
    val howItWorks: String, val step1Send: String, val step1SendDesc: String, val step2Confirm: String, val step2ConfirmDesc: String,
    val step3Validate: String, val step3ValidateDesc: String, val processingPayment: String, val confirmOnPhone: String, val paymentDone: String,
    val paymentAlreadyDone: String, val paymentSuccess: String, val orderConfirmedFmt: String, val returnToHome: String, val myGroupBuys: String,
    val myParticipations: String, val participantsStats: String, val allFilter: String, val activePlural: String, val completedPlural: String,
    val cancelledPlural: String, val noParticipation: String, val joinGroupHint: String, val originalPrice: String, val reduction: String,
    val myPrice: String, val groupLabel: String, val participantsCount: String, val anonymous: String, val filled: String,
    val completed: String, val cancelled: String, val profileUpdated: String, val saveError: String, val personalInfo: String,
    val locationLabel: String, val locationPlaceholder: String, val security: String, val newPassword: String, val passwordPlaceholder: String,
    val updateProfile: String, val avatarError: String, val coverError: String, val followedShops: String, val noFollowedShops: String,
    val unfollowedMsg: String, val unfollowError: String, val productsCount: String, val salesCount: String, val unsubscribe: String,
    val failed: String
)

data class VendorStrings(
    val createMyShop: String, val addShopPhoto: String, val shopInfo: String, val shopNameRequired: String, val shopPhoneRequired: String,
    val locationRequiredField: String, val chooseOnMap: String, val suggestions: String, val categoryRequired: String, val errShopName: String,
    val errPhoneField: String, val errLocationField: String, val errCategoryField: String, val errGeneric: String, val editProductTitle: String,
    val newProduct: String, val productPhotos: String, val productInfo: String, val productTitleField: String, val oldPrice: String,
    val publishStory: String, val publishStoryDesc: String, val mySubscribers: String, val searchSubscriber: String, val noSubscriberFound: String,
    val subscriber: String, val revenue: String, val noStatsAvailable: String, val stockAlerts: String, val lowStock: String,
    val outOfStockCount: String, val updateStock: String, val revenue7d: String, val monthlyRevenue: String, val ordersByStatus: String,
    val quickActions: String, val addPlus: String, val topProducts: String, val addFirstProduct: String, val allMyProducts: String,
    val noProductsListed: String, val manageShop: String, val viewOrders: String, val myGroupBuysMenus: String, val viewSubscribers: String,
    val exportCsv: String, val csvOrders: String, val csvRevenue: String, val addNewLineProduct: String, val dashboardTitle: String,
    val manageOrders: String, val unknownError: String, val updateError: String, val noOrdersNow: String, val customerInfo: String,
    val shopShare: String, val confirmOrder: String, val startPrep: String, val readyForDelivery: String, val confirmFinalDelivery: String,
    val awaitingReceipt: String, val telPrefix: String, val deliveryPrefix: String, val manageShopTitle: String, val done: String,
    val edit: String, val editShopName: String, val editShopLocation: String, val shopUpdated: String, val shopUpdateError: String,
    val addFirstProductHint: String, val inStockShort: String, val productsCountLabel: String, val interactionsCustomers: String, val noLikes: String,
    val noSubscribers: String, val groupBuysTitle: String, val launchGroup: String, val noGroupBuys: String, val noGroupBuysHint: String,
    val summary: String, val filledPlural: String, val participantsLabel: String, val groupBuyCancelled: String, val groupBuyDeleted: String,
    val groupBuyLaunched: String, val errorCreatingGroup: String, val notificationSentParticipants: String, val productFallback: String, val byCreator: String,
    val creatorAnonymous: String, val participantCountFmt: String, val progress: String, val groupPrice: String, val groupExpiry: String,
    val cancelGroup: String, val notifyAll: String, val newGroupBuy: String, val chooseProductOffer: String, val selectProduct: String,
    val minParticipants: String, val discountPercent: String, val offerDuration: String, val finalClientPrice: String, val launchOffer: String,
    val notifyParticipantsTitle: String, val sendNotificationParticipants: String, val notifTitle: String, val messageLabel: String, val groupBuyNotifTitle: String
)

data class AdminStrings(
    val manageAccounts: String, val activeUsers: String, val verifyManage: String, val broadcastMessages: String, val statsKPIs: String,
    val ephemeralContent: String, val homeBanners: String, val totalControl: String, val usersLabel: String, val onlineLabel: String,
    val storiesLabel: String, val promoHeroLabel: String, val superAdminLabel: String, val adminConnError: String, val addUserTitle: String,
    val role: String, val clientLabel: String, val adminLabel: String, val superLabel: String, val managedCityOptional: String,
    val globalAdminHint: String, val allFieldsRequired: String, val userCreatedSuccess: String, val adminErrPrefix: String, val promoCodeRequired: String,
    val promoAtShop: String, val promoReductPct: String, val promoFixedFcfa: String, val promoMinAmountFcfa: String, val promoCreatedNotified: String,
    val createNotify: String, val roleChangeError: String, val deleteErrorPrefix: String, val notifSent: String, val sendFailed: String,
    val sendErrPrefix: String, val sendHistory: String, val noHistory: String, val andOthers: String, val systemNotif: String,
    val receivedByAll: String, val individualNotif: String, val sendToSpecific: String, val searchUserMin: String, val noUserFound: String,
    val change: String, val notifBroadcastAll: String, val broadcastAll: String, val sendTo: String, val bannedLabel: String,
    val sendNotifMenuItem: String, val roleSuperAdmin: String, val roleAdmin: String, val roleVendor: String, val roleClient: String,
    val reactivate: String, val ban: String, val unverify: String, val verifyAction: String, val removePromo: String,
    val featureShop: String, val createPromo: String, val deleteShopTitle: String, val deleteShopConfirm: String, val filterByCity: String,
    val overview: String, val clientsLabel: String, val vendorsLabel: String, val ordersLabel: String, val totalRevenueLabel: String,
    val todayLabel: String, val alerts: String, val pendingVerifyShops: String, val goToShopsVerify: String, val bannedShopsLabel: String,
    val checkShopsDetails: String, val registrations30: String, val newUsersMonth: String, val monthlyRevenue12: String, val topVendorsCA: String,
    val orderCountFmt: String, val topProductsSold: String, val usersByRole: String, val onlineUsersTitle: String, val refreshLabel: String,
    val onlineUsersNow: String, val noOneOnline: String, val secondsAgo: String, val allStories: String, val noStoryNow: String,
    val deleteStoryError: String, val newStoryAdmin: String, val mediaPickLabel: String, val mediaAdjustNote: String, val selectMedia: String,
    val captionOptional: String, val replyCountFmt: String, val heroSectionMgmt: String, val heroModifyHint: String, val addPromotion: String,
    val heroTitleExample: String, val heroSubtitleExample: String, val heroMediaLabel: String, val orDirectUrl: String, val shopToPromote: String,
    val addToHome: String, val activeBanners: String, val noCustomBanner: String, val linkPrefix: String, val mediaUploadError: String,
    val noShopSelected: String, val noneLabel: String, val create: String, val notifyUser: String, val promoNotifBody: String,
    val superAdminPanel: String, val systemConfig: String, val globalStats: String, val reportsCount: String, val globalBroadcast: String,
    val broadcast: String, val appVersionLabel: String, val minVersionRequired: String, val commissionRate: String, val maintenanceMode: String,
    val activeProducts: String, val globalCA: String, val reportTypeLabel: String, val byReporter: String, val reasonPrefix: String,
    val resolve: String, val ignore: String
)

data class AppStrings(
    val baseS: AppBaseStrings,
    val productS: ProductActionStrings,
    val statusS: StatusStrings,
    val chatMediaS: ChatMediaStrings,
    val authS: AuthStrings,
    val checkoutS: CheckoutStrings,
    val orderS: OrderStrings,
    val homeS: HomeStrings,
    val profileS: ProfileStrings,
    val detailS: ProductDetailStrings,
    val chatNotifS: ChatNotifStrings,
    val loyaltyS: LoyaltyStrings,
    val notifPrefsS: NotifPrefsStrings,
    val miscS: SettingsMiscStrings,
    val vendorS: VendorStrings,
    val adminS: AdminStrings
) {
    // ── BRIDGE PROPERTIES FOR BACKWARD COMPATIBILITY ──

    val appName get() = baseS.appName
    val home get() = baseS.home
    val search get() = baseS.search
    val cart get() = baseS.cart
    val profile get() = baseS.profile
    val messages get() = baseS.messages
    val notifications get() = baseS.notifications
    val settings get() = baseS.settings
    val orders get() = baseS.orders
    val wishlist get() = baseS.wishlist
    val login get() = baseS.login
    val register get() = baseS.register
    val vendor get() = baseS.vendor
    val shop get() = baseS.shop
    val seller get() = baseS.seller
    val loading get() = baseS.loading
    val loadingMore get() = baseS.loadingMore
    val pullToRefresh get() = baseS.pullToRefresh
    val error get() = baseS.error
    val retry get() = baseS.retry
    val logout get() = baseS.logout
    val back get() = baseS.back
    val confirm get() = baseS.confirm
    val cancel get() = baseS.cancel
    val submit get() = baseS.submit
    val account get() = baseS.account
    val pressBackToExit get() = baseS.pressBackToExit
    val language get() = baseS.language
    val french get() = baseS.french
    val english get() = baseS.english
    val darkMode get() = baseS.darkMode
    val unknown get() = baseS.unknown
    val myPosition get() = baseS.myPosition
    val openInMaps get() = baseS.openInMaps
    val offlineBanner get() = baseS.offlineBanner
    val connectionRestored get() = baseS.connectionRestored
    val connectionLost get() = baseS.connectionLost
    val sessionExpired get() = baseS.sessionExpired
    val payment get() = baseS.payment
    val recap get() = baseS.recap
    val chooseOperator get() = baseS.chooseOperator
    val phoneNumber get() = baseS.phoneNumber

    val productDetail get() = productS.productDetail
    val addToCart get() = productS.addToCart
    val buyNow get() = productS.buyNow
    val chat get() = productS.chat
    val share get() = productS.share
    val report get() = productS.report
    val reportProduct get() = productS.reportProduct
    val category get() = productS.category
    val allCategories get() = productS.allCategories
    val priceRange get() = productS.priceRange
    val min get() = productS.min
    val max get() = productS.max
    val clear get() = productS.clear
    val noProducts get() = productS.noProducts
    val noReviews get() = productS.noReviews
    val giveReview get() = productS.giveReview
    val publishReview get() = productS.publishReview
    val yourComment get() = productS.yourComment
    val verifiedVendor get() = productS.verifiedVendor
    val whatsapp get() = productS.whatsapp
    val call get() = productS.call
    val reviews get() = productS.reviews
    val searchProducts get() = productS.searchProducts

    val orderStatusPending get() = statusS.orderStatusPending
    val orderStatusPaid get() = statusS.orderStatusPaid
    val orderStatusProcessing get() = statusS.orderStatusProcessing
    val orderStatusShipped get() = statusS.orderStatusShipped
    val orderStatusDelivered get() = statusS.orderStatusDelivered
    val orderStatusCancelled get() = statusS.orderStatusCancelled
    val total get() = statusS.total
    val items get() = statusS.items
    val shippingAddress get() = statusS.shippingAddress
    val notes get() = statusS.notes
    val createOrder get() = statusS.createOrder
    val checkout get() = statusS.checkout

    val send get() = chatMediaS.send
    val voice get() = chatMediaS.voice
    val file get() = chatMediaS.file
    val image get() = chatMediaS.image
    val photo get() = chatMediaS.photo
    val location get() = chatMediaS.location
    val emoji get() = chatMediaS.emoji
    val typeMessage get() = chatMediaS.typeMessage
    val noConversations get() = chatMediaS.noConversations

    val signIn get() = authS.signIn
    val signUp get() = authS.signUp
    val authOtpTitle get() = authS.authOtpTitle
    val phoneLogin get() = authS.phoneLogin
    val loginContinue get() = authS.loginContinue
    val createYourAccount get() = authS.createYourAccount
    val continueWithGoogle get() = authS.continueWithGoogle
    val useEmailPassword get() = authS.useEmailPassword
    val or get() = authS.or
    val noAccountSignup get() = authS.noAccountSignup
    val alreadyAccountLogin get() = authS.alreadyAccountLogin
    val createAccountTitle get() = authS.createAccountTitle
    val fullName get() = authS.fullName
    val phone get() = authS.phone
    val email get() = authS.email
    val passwordLabel get() = authS.passwordLabel
    val confirmPasswordLabel get() = authS.confirmPasswordLabel
    val acceptTerms get() = authS.acceptTerms
    val andPrivacy get() = authS.andPrivacy
    val loginBtn get() = authS.loginBtn
    val signupBtn get() = authS.signupBtn
    val chooseYourCity get() = authS.chooseYourCity
    val cityLabel get() = authS.cityLabel
    val selectYourCity get() = authS.selectYourCity
    val receiveCodeSms get() = authS.receiveCodeSms
    val sentTo get() = authS.sentTo
    val sixDigitCode get() = authS.sixDigitCode
    val codeValidFor get() = authS.codeValidFor
    val verify get() = authS.verify
    val resendCode get() = authS.resendCode
    val changeNumber get() = authS.changeNumber
    val other get() = authS.other
    val sendCode get() = authS.sendCode
    val chooseCityRequired get() = authS.chooseCityRequired
    val nameRequired get() = authS.nameRequired
    val phoneRequired get() = authS.phoneRequired
    val invalidPhone get() = authS.invalidPhone
    val emailRequired get() = authS.emailRequired
    val invalidEmail get() = authS.invalidEmail
    val passwordRequired get() = authS.passwordRequired
    val passwordTooShort get() = authS.passwordTooShort
    val confirmRequired get() = authS.confirmRequired
    val passwordsDontMatch get() = authS.passwordsDontMatch
    val acceptTermsRequired get() = authS.acceptTermsRequired
    val invalidNumberExample get() = authS.invalidNumberExample
    val codeTooShort get() = authS.codeTooShort
    val incorrectCode get() = authS.incorrectCode
    val serverLoginFailed get() = authS.serverLoginFailed
    val googleLoginFailed get() = authS.googleLoginFailed
    val sendingError get() = authS.sendingError
    val authErrorPrefix get() = authS.authErrorPrefix

    val cartTitle get() = checkoutS.cartTitle
    val removeFromCart get() = checkoutS.removeFromCart
    val removeFromCartConfirm get() = checkoutS.removeFromCartConfirm
    val remove get() = checkoutS.remove
    val emptyCartTitle get() = checkoutS.emptyCartTitle
    val emptyCartHint get() = checkoutS.emptyCartHint
    val finalizeOrder get() = checkoutS.finalizeOrder
    val addressLabel get() = checkoutS.addressLabel
    val contactPhone get() = checkoutS.contactPhone
    val paymentMode get() = checkoutS.paymentMode
    val payOnDelivery get() = checkoutS.payOnDelivery
    val payOnDeliveryHint get() = checkoutS.payOnDeliveryHint
    val directToVendor get() = checkoutS.directToVendor
    val directToVendorHint get() = checkoutS.directToVendorHint
    val paymentInstructions get() = checkoutS.paymentInstructions
    val transferInstructions get() = checkoutS.transferInstructions
    val amountToTransfer get() = checkoutS.amountToTransfer
    val payAndOrder get() = checkoutS.payAndOrder
    val paymentMethod get() = checkoutS.paymentMethod
    val cashOnDelivery get() = checkoutS.cashOnDelivery
    val promoCode get() = checkoutS.promoCode
    val enterCode get() = checkoutS.enterCode
    val apply get() = checkoutS.apply
    val promoInvalid get() = checkoutS.promoInvalid
    val promoValidationError get() = checkoutS.promoValidationError
    val promoApplied get() = checkoutS.promoApplied
    val articles get() = checkoutS.articles
    val subtotal get() = checkoutS.subtotal
    val discount get() = checkoutS.discount
    val deliveryFee get() = checkoutS.deliveryFee
    val instructions get() = checkoutS.instructions
    val notesForVendor get() = checkoutS.notesForVendor
    val copy get() = checkoutS.copy

    val myOrders get() = orderS.myOrders
    val noOrders get() = orderS.noOrders
    val noOrdersHint get() = orderS.noOrdersHint
    val orderCancelled get() = orderS.orderCancelled
    val orderActionError get() = orderS.orderActionError
    val receptionConfirmed get() = orderS.receptionConfirmed
    val cancelOrderTitle get() = orderS.cancelOrderTitle
    val cancelOrderConfirmText get() = orderS.cancelOrderConfirmText
    val confirmReceptionTitle get() = orderS.confirmReceptionTitle
    val confirmReceptionText get() = orderS.confirmReceptionText
    val estimatedDelivery get() = orderS.estimatedDelivery
    val waitingForVendor get() = orderS.waitingForVendor
    val transferWait get() = orderS.transferWait
    val orderCreated get() = orderS.orderCreated
    val paymentValidated get() = orderS.paymentValidated
    val orderConfirmed get() = orderS.orderConfirmed
    val orderPreparing get() = orderS.orderPreparing
    val orderDelivering get() = orderS.orderDelivering
    val orderTracking get() = orderS.orderTracking
    val awaitingSellerConfirmation get() = orderS.awaitingSellerConfirmation
    val confirmReception get() = orderS.confirmReception
    val cancelOrder get() = orderS.cancelOrder
    val contactSeller get() = orderS.contactSeller
    val no get() = orderS.no
    val yes get() = orderS.yes
    val deleteConfirm get() = orderS.deleteConfirm
    val delivery get() = orderS.delivery
    val amount get() = orderS.amount
    val orderDelivered get() = orderS.orderDelivered

    val registerShort get() = homeS.registerShort
    val products get() = homeS.products
    val shops get() = homeS.shops
    val compareCta get() = homeS.compareCta
    val comparateur get() = homeS.comparateur
    val compareSelection get() = homeS.compareSelection
    val compareEmptyHint get() = homeS.compareEmptyHint
    val compareEmptyHint2 get() = homeS.compareEmptyHint2
    val searchCompareHint get() = homeS.searchCompareHint
    val addCompareHint get() = homeS.addCompareHint
    val alreadyAdded get() = homeS.alreadyAdded
    val price get() = homeS.price
    val stock get() = homeS.stock
    val units get() = homeS.units
    val outOfStock get() = homeS.outOfStock
    val bestPrice get() = homeS.bestPrice
    val bestChoice get() = homeS.bestChoice
    val optimalValue get() = homeS.optimalValue
    val view get() = homeS.view
    val arrivalsToday get() = homeS.arrivalsToday
    val addStory get() = homeS.addStory
    val addStoryHint get() = homeS.addStoryHint
    val video get() = homeS.video
    val textOnly get() = homeS.textOnly
    val addCaption get() = homeS.addCaption
    val addCaptionHint get() = homeS.addCaptionHint
    val yourMessage get() = homeS.yourMessage
    val publish get() = homeS.publish
    val skip get() = homeS.skip
    val textStory get() = homeS.textStory
    val whatDoYouWantToSay get() = homeS.whatDoYouWantToSay
    val backgroundColor get() = homeS.backgroundColor
    val filters get() = homeS.filters
    val reset get() = homeS.reset
    val sortBy get() = homeS.sortBy
    val newest get() = homeS.newest
    val priceAsc get() = homeS.priceAsc
    val priceDesc get() = homeS.priceDesc
    val noProductsFound get() = homeS.noProductsFound
    val noProductsFoundHint get() = homeS.noProductsFoundHint
    val favorites get() = homeS.favorites
    val articlesCount get() = homeS.articlesCount
    val add get() = homeS.add
    val story get() = homeS.story

    val myAccount get() = profileS.myAccount
    val logoutConfirmTitle get() = profileS.logoutConfirmTitle
    val logoutConfirmText get() = profileS.logoutConfirmText
    val defaultUser get() = profileS.defaultUser
    val editProfile get() = profileS.editProfile
    val seeAll get() = profileS.seeAll
    val myWallet get() = profileS.myWallet
    val myServices get() = profileS.myServices
    val connect get() = profileS.connect
    val loginRequiredHint get() = profileS.loginRequiredHint
    val points get() = profileS.points
    val cashback get() = profileS.cashback
    val level get() = profileS.level
    val toPay get() = profileS.toPay
    val toShip get() = profileS.toShip
    val toReceive get() = profileS.toReceive
    val myFavorites get() = profileS.myFavorites
    val followed get() = profileS.followed
    val coupons get() = profileS.coupons
    val groupBuys get() = profileS.groupBuys
    val vendorSpace get() = profileS.vendorSpace
    val vendorSpaceSubtitle get() = profileS.vendorSpaceSubtitle
    val admin get() = profileS.admin
    val adminSubtitle get() = profileS.adminSubtitle
    val balance get() = profileS.balance

    val productDetails get() = detailS.productDetails
    val soldCount get() = detailS.soldCount
    val alreadyBought get() = detailS.alreadyBought
    val onlyLeft get() = detailS.onlyLeft
    val bestSeller get() = detailS.bestSeller
    val color get() = detailS.color
    val reviewsAndRatings get() = detailS.reviewsAndRatings
    val similarProducts get() = detailS.similarProducts
    val noSimilarProducts get() = detailS.noSimilarProducts
    val reportSent get() = detailS.reportSent
    val reportCommentPlaceholder get() = detailS.reportCommentPlaceholder
    val close get() = detailS.close
    val sendReport get() = detailS.sendReport
    val rating get() = detailS.rating
    val sales get() = detailS.sales
    val orderItemsCount get() = detailS.orderItemsCount
    val backToOrders get() = detailS.backToOrders
    val markAllRead get() = detailS.markAllRead

    val deleteMessage get() = chatNotifS.deleteMessage
    val deleteMessageConfirm get() = chatNotifS.deleteMessageConfirm
    val you get() = chatNotifS.you
    val productLabel get() = chatNotifS.productLabel
    val reply get() = chatNotifS.reply
    val react get() = chatNotifS.react
    val reactToMessage get() = chatNotifS.reactToMessage
    val shareLocation get() = chatNotifS.shareLocation
    val currentPosition get() = chatNotifS.currentPosition
    val map get() = chatNotifS.map
    val gallery get() = chatNotifS.gallery
    val camera get() = chatNotifS.camera
    val localization get() = chatNotifS.localization
    val messageCenter get() = chatNotifS.messageCenter
    val noMessages get() = chatNotifS.noMessages
    val noResultsFor get() = chatNotifS.noResultsFor
    val deleteConversation get() = chatNotifS.deleteConversation
    val deleteConversationConfirm get() = chatNotifS.deleteConversationConfirm
    val searchContacts get() = chatNotifS.searchContacts

    val loyaltyProgram get() = loyaltyS.loyaltyProgram
    val dataUpdated get() = loyaltyS.dataUpdated
    val redeemPoints get() = loyaltyS.redeemPoints
    val recharge get() = loyaltyS.recharge
    val history get() = loyaltyS.history
    val myCoupons get() = loyaltyS.myCoupons
    val noTransactions get() = loyaltyS.noTransactions
    val noCoupons get() = loyaltyS.noCoupons
    val advantagesPerTier get() = loyaltyS.advantagesPerTier
    val cardLabel get() = loyaltyS.cardLabel
    val cashbackBalanceLabel get() = loyaltyS.cashbackBalanceLabel
    val pointsUsable get() = loyaltyS.pointsUsable
    val pointsAccumulated get() = loyaltyS.pointsAccumulated
    val nextLevel get() = loyaltyS.nextLevel
    val pointsToReach get() = loyaltyS.pointsToReach
    val earned get() = loyaltyS.earned
    val spent get() = loyaltyS.spent
    val rechargeLabel get() = loyaltyS.rechargeLabel
    val cashbackLabel get() = loyaltyS.cashbackLabel
    val bonusLabel get() = loyaltyS.bonusLabel
    val refund get() = loyaltyS.refund
    val fcfaDiscount get() = loyaltyS.fcfaDiscount
    val pctDiscount get() = loyaltyS.pctDiscount
    val expiresOn get() = loyaltyS.expiresOn
    val redeemMyPoints get() = loyaltyS.redeemMyPoints
    val youHavePoints get() = loyaltyS.youHavePoints
    val pointsToExchange get() = loyaltyS.pointsToExchange
    val pointsValue get() = loyaltyS.pointsValue
    val exchange get() = loyaltyS.exchange
    val rechargeWallet get() = loyaltyS.rechargeWallet
    val amountFcfa get() = loyaltyS.amountFcfa
    val couponGenerated get() = loyaltyS.couponGenerated
    val errorRedeem get() = loyaltyS.errorRedeem
    val rechargeDone get() = loyaltyS.rechargeDone
    val errorRecharge get() = loyaltyS.errorRecharge
    val active get() = loyaltyS.active

    val notifPrefsTitle get() = notifPrefsS.notifPrefsTitle
    val pushNotifs get() = notifPrefsS.pushNotifs
    val pushNotifsDesc get() = notifPrefsS.pushNotifsDesc
    val newProducts get() = notifPrefsS.newProducts
    val newProductsDesc get() = notifPrefsS.newProductsDesc
    val orderUpdates get() = notifPrefsS.orderUpdates
    val orderUpdatesDesc get() = notifPrefsS.orderUpdatesDesc
    val promoOffers get() = notifPrefsS.promoOffers
    val promoOffersDesc get() = notifPrefsS.promoOffersDesc
    val messagesToggle get() = notifPrefsS.messagesToggle
    val messagesToggleDesc get() = notifPrefsS.messagesToggleDesc
    val systemToggle get() = notifPrefsS.systemToggle
    val systemToggleDesc get() = notifPrefsS.systemToggleDesc
    val pushEnabled get() = notifPrefsS.pushEnabled
    val pushEnabledDesc get() = notifPrefsS.pushEnabledDesc
    val prefsSaved get() = notifPrefsS.prefsSaved
    val errorSaving get() = notifPrefsS.errorSaving
    val save get() = notifPrefsS.save

    val legalMentions get() = miscS.legalMentions
    val termsOfUse get() = miscS.termsOfUse
    val downloads get() = miscS.downloads
    val androidApk get() = miscS.androidApk
    val installApk get() = miscS.installApk
    val iosApp get() = miscS.iosApp
    val comingSoon get() = miscS.comingSoon
    val about get() = miscS.about
    val next get() = miscS.next
    val start get() = miscS.start
    val allShops get() = miscS.allShops
    val shopsIn get() = miscS.shopsIn
    val noShopsFound get() = miscS.noShopsFound
    val follow get() = miscS.follow
    val followers get() = miscS.followers
    val followersCount get() = miscS.followersCount
    val verified get() = miscS.verified
    val shopNotFound get() = miscS.shopNotFound
    val sold get() = miscS.sold
    val noProductsForNow get() = miscS.noProductsForNow
    val followShop get() = miscS.followShop
    val unfollow get() = miscS.unfollow
    val featuredProducts get() = miscS.featuredProducts
    val customerReviews get() = miscS.customerReviews
    val noStory get() = miscS.noStory
    val storyDeleted get() = miscS.storyDeleted
    val storyError get() = miscS.storyError
    val replyToSeller get() = miscS.replyToSeller
    val msgSentToSeller get() = miscS.msgSentToSeller
    val vendorNotFound get() = miscS.vendorNotFound
    val delete get() = miscS.delete
    val version get() = miscS.version
    val lastUpdate get() = miscS.lastUpdate
    val ourMissionTitle get() = miscS.ourMissionTitle
    val contactSupport get() = miscS.contactSupport
    val allRights get() = miscS.allRights
    val comparatif get() = miscS.comparatif
    val noProductsCompare get() = miscS.noProductsCompare
    val unit get() = miscS.unit
    val inStock get() = miscS.inStock
    val description get() = miscS.description
    val shopsMapTitle get() = miscS.shopsMapTitle
    val searchShop get() = miscS.searchShop
    val shopClickTip get() = miscS.shopClickTip
    val mapOpensTip get() = miscS.mapOpensTip
    val scanProduct get() = miscS.scanProduct
    val cameraInit get() = miscS.cameraInit
    val barcodeHint get() = miscS.barcodeHint
    val scanBarcodeTitle get() = miscS.scanBarcodeTitle
    val useCameraHint get() = miscS.useCameraHint
    val orTypeManually get() = miscS.orTypeManually
    val barcodeLabel get() = miscS.barcodeLabel
    val barcodeExample get() = miscS.barcodeExample
    val searchAction get() = miscS.searchAction
    val errorPrefix get() = miscS.errorPrefix
    val noNotifications get() = miscS.noNotifications
    val noNotificationsHint get() = miscS.noNotificationsHint
    val emptyFavorites get() = miscS.emptyFavorites
    val emptyFavoritesHint get() = miscS.emptyFavoritesHint
    val negotiate get() = miscS.negotiate
    val paymentTitle get() = miscS.paymentTitle
    val amountToPay get() = miscS.amountToPay
    val phoneNumberPrefix get() = miscS.phoneNumberPrefix
    val confirmPayment get() = miscS.confirmPayment
    val howItWorks get() = miscS.howItWorks
    val step1Send get() = miscS.step1Send
    val step1SendDesc get() = miscS.step1SendDesc
    val step2Confirm get() = miscS.step2Confirm
    val step2ConfirmDesc get() = miscS.step2ConfirmDesc
    val step3Validate get() = miscS.step3Validate
    val step3ValidateDesc get() = miscS.step3ValidateDesc
    val processingPayment get() = miscS.processingPayment
    val confirmOnPhone get() = miscS.confirmOnPhone
    val paymentDone get() = miscS.paymentDone
    val paymentAlreadyDone get() = miscS.paymentAlreadyDone
    val paymentSuccess get() = miscS.paymentSuccess
    val orderConfirmedFmt get() = miscS.orderConfirmedFmt
    val returnToHome get() = miscS.returnToHome
    val myGroupBuys get() = miscS.myGroupBuys
    val myParticipations get() = miscS.myParticipations
    val participantsStats get() = miscS.participantsStats
    val allFilter get() = miscS.allFilter
    val activePlural get() = miscS.activePlural
    val completedPlural get() = miscS.completedPlural
    val cancelledPlural get() = miscS.cancelledPlural
    val noParticipation get() = miscS.noParticipation
    val joinGroupHint get() = miscS.joinGroupHint
    val originalPrice get() = miscS.originalPrice
    val reduction get() = miscS.reduction
    val myPrice get() = miscS.myPrice
    val groupLabel get() = miscS.groupLabel
    val participantsCount get() = miscS.participantsCount
    val anonymous get() = miscS.anonymous
    val filled get() = miscS.filled
    val completed get() = miscS.completed
    val cancelled get() = miscS.cancelled
    val profileUpdated get() = miscS.profileUpdated
    val saveError get() = miscS.saveError
    val personalInfo get() = miscS.personalInfo
    val locationLabel get() = miscS.locationLabel
    val locationPlaceholder get() = miscS.locationPlaceholder
    val security get() = miscS.security
    val newPassword get() = miscS.newPassword
    val passwordPlaceholder get() = miscS.passwordPlaceholder
    val updateProfile get() = miscS.updateProfile
    val avatarError get() = miscS.avatarError
    val coverError get() = miscS.coverError
    val followedShops get() = miscS.followedShops
    val noFollowedShops get() = miscS.noFollowedShops
    val unfollowedMsg get() = miscS.unfollowedMsg
    val unfollowError get() = miscS.unfollowError
    val productsCount get() = miscS.productsCount
    val salesCount get() = miscS.salesCount
    val unsubscribe get() = miscS.unsubscribe
    val failed get() = miscS.failed

    val createMyShop get() = vendorS.createMyShop
    val addShopPhoto get() = vendorS.addShopPhoto
    val shopInfo get() = vendorS.shopInfo
    val shopNameRequired get() = vendorS.shopNameRequired
    val shopPhoneRequired get() = vendorS.shopPhoneRequired
    val locationRequiredField get() = vendorS.locationRequiredField
    val chooseOnMap get() = vendorS.chooseOnMap
    val suggestions get() = vendorS.suggestions
    val categoryRequired get() = vendorS.categoryRequired
    val errShopName get() = vendorS.errShopName
    val errPhoneField get() = vendorS.errPhoneField
    val errLocationField get() = vendorS.errLocationField
    val errCategoryField get() = vendorS.errCategoryField
    val errGeneric get() = vendorS.errGeneric
    val editProductTitle get() = vendorS.editProductTitle
    val newProduct get() = vendorS.newProduct
    val productPhotos get() = vendorS.productPhotos
    val productInfo get() = vendorS.productInfo
    val productTitleField get() = vendorS.productTitleField
    val oldPrice get() = vendorS.oldPrice
    val publishStory get() = vendorS.publishStory
    val publishStoryDesc get() = vendorS.publishStoryDesc
    val mySubscribers get() = vendorS.mySubscribers
    val searchSubscriber get() = vendorS.searchSubscriber
    val noSubscriberFound get() = vendorS.noSubscriberFound
    val subscriber get() = vendorS.subscriber
    val revenue get() = vendorS.revenue
    val noStatsAvailable get() = vendorS.noStatsAvailable
    val stockAlerts get() = vendorS.stockAlerts
    val lowStock get() = vendorS.lowStock
    val outOfStockCount get() = vendorS.outOfStockCount
    val updateStock get() = vendorS.updateStock
    val revenue7d get() = vendorS.revenue7d
    val monthlyRevenue get() = vendorS.monthlyRevenue
    val ordersByStatus get() = vendorS.ordersByStatus
    val quickActions get() = vendorS.quickActions
    val addPlus get() = vendorS.addPlus
    val topProducts get() = vendorS.topProducts
    val addFirstProduct get() = vendorS.addFirstProduct
    val allMyProducts get() = vendorS.allMyProducts
    val noProductsListed get() = vendorS.noProductsListed
    val manageShop get() = vendorS.manageShop
    val viewOrders get() = vendorS.viewOrders
    val myGroupBuysMenus get() = vendorS.myGroupBuysMenus
    val viewSubscribers get() = vendorS.viewSubscribers
    val exportCsv get() = vendorS.exportCsv
    val csvOrders get() = vendorS.csvOrders
    val csvRevenue get() = vendorS.csvRevenue
    val addNewLineProduct get() = vendorS.addNewLineProduct
    val dashboardTitle get() = vendorS.dashboardTitle
    val manageOrders get() = vendorS.manageOrders
    val unknownError get() = vendorS.unknownError
    val updateError get() = vendorS.updateError
    val noOrdersNow get() = vendorS.noOrdersNow
    val customerInfo get() = vendorS.customerInfo
    val shopShare get() = vendorS.shopShare
    val confirmOrder get() = vendorS.confirmOrder
    val startPrep get() = vendorS.startPrep
    val readyForDelivery get() = vendorS.readyForDelivery
    val confirmFinalDelivery get() = vendorS.confirmFinalDelivery
    val awaitingReceipt get() = vendorS.awaitingReceipt
    val telPrefix get() = vendorS.telPrefix
    val deliveryPrefix get() = vendorS.deliveryPrefix
    val manageShopTitle get() = vendorS.manageShopTitle
    val done get() = vendorS.done
    val edit get() = vendorS.edit
    val editShopName get() = vendorS.editShopName
    val editShopLocation get() = vendorS.editShopLocation
    val shopUpdated get() = vendorS.shopUpdated
    val shopUpdateError get() = vendorS.shopUpdateError
    val addFirstProductHint get() = vendorS.addFirstProductHint
    val inStockShort get() = vendorS.inStockShort
    val productsCountLabel get() = vendorS.productsCountLabel
    val interactionsCustomers get() = vendorS.interactionsCustomers
    val noLikes get() = vendorS.noLikes
    val noSubscribers get() = vendorS.noSubscribers
    val groupBuysTitle get() = vendorS.groupBuysTitle
    val launchGroup get() = vendorS.launchGroup
    val noGroupBuys get() = vendorS.noGroupBuys
    val noGroupBuysHint get() = vendorS.noGroupBuysHint
    val summary get() = vendorS.summary
    val filledPlural get() = vendorS.filledPlural
    val participantsLabel get() = vendorS.participantsLabel
    val groupBuyCancelled get() = vendorS.groupBuyCancelled
    val groupBuyDeleted get() = vendorS.groupBuyDeleted
    val groupBuyLaunched get() = vendorS.groupBuyLaunched
    val errorCreatingGroup get() = vendorS.errorCreatingGroup
    val notificationSentParticipants get() = vendorS.notificationSentParticipants
    val productFallback get() = vendorS.productFallback
    val byCreator get() = vendorS.byCreator
    val creatorAnonymous get() = vendorS.creatorAnonymous
    val participantCountFmt get() = vendorS.participantCountFmt
    val progress get() = vendorS.progress
    val groupPrice get() = vendorS.groupPrice
    val groupExpiry get() = vendorS.groupExpiry
    val cancelGroup get() = vendorS.cancelGroup
    val notifyAll get() = vendorS.notifyAll
    val newGroupBuy get() = vendorS.newGroupBuy
    val chooseProductOffer get() = vendorS.chooseProductOffer
    val selectProduct get() = vendorS.selectProduct
    val minParticipants get() = vendorS.minParticipants
    val discountPercent get() = vendorS.discountPercent
    val offerDuration get() = vendorS.offerDuration
    val finalClientPrice get() = vendorS.finalClientPrice
    val launchOffer get() = vendorS.launchOffer
    val notifyParticipantsTitle get() = vendorS.notifyParticipantsTitle
    val sendNotificationParticipants get() = vendorS.sendNotificationParticipants
    val notifTitle get() = vendorS.notifTitle
    val messageLabel get() = vendorS.messageLabel
    val groupBuyNotifTitle get() = vendorS.groupBuyNotifTitle

    val manageAccounts get() = adminS.manageAccounts
    val activeUsers get() = adminS.activeUsers
    val verifyManage get() = adminS.verifyManage
    val broadcastMessages get() = adminS.broadcastMessages
    val statsKPIs get() = adminS.statsKPIs
    val ephemeralContent get() = adminS.ephemeralContent
    val homeBanners get() = adminS.homeBanners
    val totalControl get() = adminS.totalControl
    val usersLabel get() = adminS.usersLabel
    val onlineLabel get() = adminS.onlineLabel
    val storiesLabel get() = adminS.storiesLabel
    val promoHeroLabel get() = adminS.promoHeroLabel
    val superAdminLabel get() = adminS.superAdminLabel
    val adminConnError get() = adminS.adminConnError
    val addUserTitle get() = adminS.addUserTitle
    val role get() = adminS.role
    val clientLabel get() = adminS.clientLabel
    val adminLabel get() = adminS.adminLabel
    val superLabel get() = adminS.superLabel
    val managedCityOptional get() = adminS.managedCityOptional
    val globalAdminHint get() = adminS.globalAdminHint
    val allFieldsRequired get() = adminS.allFieldsRequired
    val userCreatedSuccess get() = adminS.userCreatedSuccess
    val adminErrPrefix get() = adminS.adminErrPrefix
    val promoCodeRequired get() = adminS.promoCodeRequired
    val promoAtShop get() = adminS.promoAtShop
    val promoReductPct get() = adminS.promoReductPct
    val promoFixedFcfa get() = adminS.promoFixedFcfa
    val promoMinAmountFcfa get() = adminS.promoMinAmountFcfa
    val promoCreatedNotified get() = adminS.promoCreatedNotified
    val createNotify get() = adminS.createNotify
    val roleChangeError get() = adminS.roleChangeError
    val deleteErrorPrefix get() = adminS.deleteErrorPrefix
    val notifSent get() = adminS.notifSent
    val sendFailed get() = adminS.sendFailed
    val sendErrPrefix get() = adminS.sendErrPrefix
    val sendHistory get() = adminS.sendHistory
    val noHistory get() = adminS.noHistory
    val andOthers get() = adminS.andOthers
    val systemNotif get() = adminS.systemNotif
    val receivedByAll get() = adminS.receivedByAll
    val individualNotif get() = adminS.individualNotif
    val sendToSpecific get() = adminS.sendToSpecific
    val searchUserMin get() = adminS.searchUserMin
    val noUserFound get() = adminS.noUserFound
    val change get() = adminS.change
    val notifBroadcastAll get() = adminS.notifBroadcastAll
    val broadcastAll get() = adminS.broadcastAll
    val sendTo get() = adminS.sendTo
    val bannedLabel get() = adminS.bannedLabel
    val sendNotifMenuItem get() = adminS.sendNotifMenuItem
    val roleSuperAdmin get() = adminS.roleSuperAdmin
    val roleAdmin get() = adminS.roleAdmin
    val roleVendor get() = adminS.roleVendor
    val roleClient get() = adminS.roleClient
    val reactivate get() = adminS.reactivate
    val ban get() = adminS.ban
    val unverify get() = adminS.unverify
    val verifyAction get() = adminS.verifyAction
    val removePromo get() = adminS.removePromo
    val featureShop get() = adminS.featureShop
    val createPromo get() = adminS.createPromo
    val deleteShopTitle get() = adminS.deleteShopTitle
    val deleteShopConfirm get() = adminS.deleteShopConfirm
    val filterByCity get() = adminS.filterByCity
    val overview get() = adminS.overview
    val clientsLabel get() = adminS.clientsLabel
    val vendorsLabel get() = adminS.vendorsLabel
    val ordersLabel get() = adminS.ordersLabel
    val totalRevenueLabel get() = adminS.totalRevenueLabel
    val todayLabel get() = adminS.todayLabel
    val alerts get() = adminS.alerts
    val pendingVerifyShops get() = adminS.pendingVerifyShops
    val goToShopsVerify get() = adminS.goToShopsVerify
    val bannedShopsLabel get() = adminS.bannedShopsLabel
    val checkShopsDetails get() = adminS.checkShopsDetails
    val registrations30 get() = adminS.registrations30
    val newUsersMonth get() = adminS.newUsersMonth
    val monthlyRevenue12 get() = adminS.monthlyRevenue12
    val topVendorsCA get() = adminS.topVendorsCA
    val orderCountFmt get() = adminS.orderCountFmt
    val topProductsSold get() = adminS.topProductsSold
    val usersByRole get() = adminS.usersByRole
    val onlineUsersTitle get() = adminS.onlineUsersTitle
    val refreshLabel get() = adminS.refreshLabel
    val onlineUsersNow get() = adminS.onlineUsersNow
    val noOneOnline get() = adminS.noOneOnline
    val secondsAgo get() = adminS.secondsAgo
    val allStories get() = adminS.allStories
    val noStoryNow get() = adminS.noStoryNow
    val deleteStoryError get() = adminS.deleteStoryError
    val newStoryAdmin get() = adminS.newStoryAdmin
    val mediaPickLabel get() = adminS.mediaPickLabel
    val mediaAdjustNote get() = adminS.mediaAdjustNote
    val selectMedia get() = adminS.selectMedia
    val captionOptional get() = adminS.captionOptional
    val replyCountFmt get() = adminS.replyCountFmt
    val heroSectionMgmt get() = adminS.heroSectionMgmt
    val heroModifyHint get() = adminS.heroModifyHint
    val addPromotion get() = adminS.addPromotion
    val heroTitleExample get() = adminS.heroTitleExample
    val heroSubtitleExample get() = adminS.heroSubtitleExample
    val heroMediaLabel get() = adminS.heroMediaLabel
    val orDirectUrl get() = adminS.orDirectUrl
    val shopToPromote get() = adminS.shopToPromote
    val addToHome get() = adminS.addToHome
    val activeBanners get() = adminS.activeBanners
    val noCustomBanner get() = adminS.noCustomBanner
    val linkPrefix get() = adminS.linkPrefix
    val mediaUploadError get() = adminS.mediaUploadError
    val noShopSelected get() = adminS.noShopSelected
    val noneLabel get() = adminS.noneLabel
    val create get() = adminS.create
    val notifyUser get() = adminS.notifyUser
    val promoNotifBody get() = adminS.promoNotifBody
    val superAdminPanel get() = adminS.superAdminPanel
    val systemConfig get() = adminS.systemConfig
    val globalStats get() = adminS.globalStats
    val reportsCount get() = adminS.reportsCount
    val globalBroadcast get() = adminS.globalBroadcast
    val broadcast get() = adminS.broadcast
    val appVersionLabel get() = adminS.appVersionLabel
    val minVersionRequired get() = adminS.minVersionRequired
    val commissionRate get() = adminS.commissionRate
    val maintenanceMode get() = adminS.maintenanceMode
    val activeProducts get() = adminS.activeProducts
    val globalCA get() = adminS.globalCA
    val reportTypeLabel get() = adminS.reportTypeLabel
    val byReporter get() = adminS.byReporter
    val reasonPrefix get() = adminS.reasonPrefix
    val resolve get() = adminS.resolve
    val ignore get() = adminS.ignore
}

fun getStrings(lang: String): AppStrings = if (lang == "en") EN else FR

private val FR = AppStrings(
    baseS = getAppBaseStrings("fr"),
    productS = getProductActionStrings("fr"),
    statusS = getStatusStrings("fr"),
    chatMediaS = getChatMediaStrings("fr"),
    authS = getAuthStrings("fr"),
    checkoutS = getCheckoutStrings("fr"),
    orderS = getOrderStrings("fr"),
    homeS = getHomeStrings("fr"),
    profileS = getProfileStrings("fr"),
    detailS = getProductDetailStrings("fr"),
    chatNotifS = getChatNotifStrings("fr"),
    loyaltyS = getLoyaltyStrings("fr"),
    notifPrefsS = getNotifPrefsStrings("fr"),
    miscS = getSettingsMiscStrings("fr"),
    vendorS = getVendorStrings("fr"),
    adminS = getAdminStrings("fr")
)

private val EN = AppStrings(
    baseS = getAppBaseStrings("en"),
    productS = getProductActionStrings("en"),
    statusS = getStatusStrings("en"),
    chatMediaS = getChatMediaStrings("en"),
    authS = getAuthStrings("en"),
    checkoutS = getCheckoutStrings("en"),
    orderS = getOrderStrings("en"),
    homeS = getHomeStrings("en"),
    profileS = getProfileStrings("en"),
    detailS = getProductDetailStrings("en"),
    chatNotifS = getChatNotifStrings("en"),
    loyaltyS = getLoyaltyStrings("en"),
    notifPrefsS = getNotifPrefsStrings("en"),
    miscS = getSettingsMiscStrings("en"),
    vendorS = getVendorStrings("en"),
    adminS = getAdminStrings("en")
)

private fun getAppBaseStrings(lang: String) = if (lang == "en") AppBaseStrings(
    appName = "TiK-Market", home = "Home", search = "Search", cart = "Cart", profile = "My Account",
    messages = "Messages", notifications = "Notifications", settings = "Settings", orders = "Orders", wishlist = "Wishlist",
    login = "Login", register = "Sign Up", vendor = "Seller", shop = "Shop", seller = "Seller",
    loading = "Loading...", loadingMore = "Loading...", pullToRefresh = "Pull to refresh", error = "Error", retry = "Retry",
    logout = "Logout", back = "Back", confirm = "Confirm", cancel = "Cancel", submit = "Submit",
    account = "Account", pressBackToExit = "Press back again to exit", language = "Language", french = "Français",
    english = "English", darkMode = "Dark mode", unknown = "Unknown", myPosition = "My location", openInMaps = "Open in Maps",
    payment = "Payment", recap = "Summary", chooseOperator = "Choose your operator", phoneNumber = "Mobile Money Number",
    offlineBanner = "🔴 Connection lost — some features may be limited", connectionRestored = "✅ Connection restored",
    connectionLost = "🔴 Connection lost", sessionExpired = "Session expired"
) else AppBaseStrings(
    appName = "TiK-Market", home = "Accueil", search = "Rechercher", cart = "Panier", profile = "Mon Compte",
    messages = "Messages", notifications = "Notifications", settings = "Paramètres", orders = "Commandes", wishlist = "Favoris",
    login = "Connexion", register = "Inscription", vendor = "Vendeur", shop = "Boutique", seller = "Vendeur",
    loading = "Chargement...", loadingMore = "Chargement...", pullToRefresh = "Tirer pour actualiser", error = "Erreur", retry = "Réessayer",
    logout = "Déconnexion", back = "Retour", confirm = "Confirmer", cancel = "Annuler", submit = "Envoyer",
    account = "Compte", pressBackToExit = "Appuyez encore pour quitter", language = "Langue", french = "Français",
    english = "English", darkMode = "Mode sombre", unknown = "Inconnu", myPosition = "Ma position", openInMaps = "Ouvrir sur Maps",
    payment = "Paiement", recap = "Récapitulatif", chooseOperator = "Choisir l'opérateur", phoneNumber = "Numéro Mobile Money",
    offlineBanner = "🔴 Connexion perdue — certaines fonctions sont limitées", connectionRestored = "✅ Connexion rétablie",
    connectionLost = "🔴 Connexion perdue", sessionExpired = "Session expirée"
)

private fun getProductActionStrings(lang: String) = if (lang == "en") ProductActionStrings(
    productDetail = "Product Details", addToCart = "Add to Cart", buyNow = "Buy Now", chat = "Chat", share = "Share",
    report = "Report", reportProduct = "Report this product", category = "Category", allCategories = "All", priceRange = "Price",
    min = "Min", max = "Max", clear = "Clear", noProducts = "No products found", noReviews = "No reviews yet",
    giveReview = "Give a review", publishReview = "Publish review", yourComment = "Your comment (optional)...",
    verifiedVendor = "Verified seller", whatsapp = "WhatsApp", call = "Call", reviews = "Reviews & Ratings",
    searchProducts = "Search products..."
) else ProductActionStrings(
    productDetail = "Détails Produit", addToCart = "Ajouter au panier", buyNow = "Acheter", chat = "Chat", share = "Partager",
    report = "Signaler", reportProduct = "Signaler ce produit", category = "Catégorie", allCategories = "Tout", priceRange = "Prix",
    min = "Min", max = "Max", clear = "Effacer", noProducts = "Aucun produit trouvé", noReviews = "Aucun avis pour le moment",
    giveReview = "Donner mon avis", publishReview = "Publier mon avis", yourComment = "Votre commentaire (optionnel)...",
    verifiedVendor = "Vendeur vérifié", whatsapp = "WhatsApp", call = "Call", reviews = "Avis & Notes",
    searchProducts = "Rechercher un produit..."
)

private fun getStatusStrings(lang: String) = if (lang == "en") StatusStrings(
    orderStatusPending = "Awaiting payment", orderStatusPaid = "Paid", orderStatusProcessing = "Processing",
    orderStatusShipped = "Shipped", orderStatusDelivered = "Delivered", orderStatusCancelled = "Cancelled",
    total = "Total", items = "item(s)", shippingAddress = "Shipping address", notes = "Notes",
    createOrder = "Place order", checkout = "Checkout"
) else StatusStrings(
    orderStatusPending = "En attente", orderStatusPaid = "Payé", orderStatusProcessing = "Traitement",
    orderStatusShipped = "Expédié", orderStatusDelivered = "Livré", orderStatusCancelled = "Annulé",
    total = "Total", items = "article(s)", shippingAddress = "Adresse de livraison", notes = "Notes",
    createOrder = "Commander", checkout = "Paiement"
)

private fun getChatMediaStrings(lang: String) = if (lang == "en") ChatMediaStrings(
    send = "Send", voice = "Voice", file = "File", image = "Image", photo = "Photo",
    location = "Location", emoji = "Emoji", typeMessage = "Type a message...", noConversations = "No conversations"
) else ChatMediaStrings(
    send = "Envoyer", voice = "Vocal", file = "Fichier", image = "Image", photo = "Photo",
    location = "Localisation", emoji = "Emoji", typeMessage = "Taper un message...", noConversations = "Aucune discussion"
)

private fun getAuthStrings(lang: String) = if (lang == "en") AuthStrings(
    signIn = "Sign in", signUp = "Sign up", authOtpTitle = "OTP Verification", phoneLogin = "Phone Login", loginContinue = "Log in to continue",
    createYourAccount = "Create your account", continueWithGoogle = "Continue with Google", useEmailPassword = "Use Email / Password", or = "or", noAccountSignup = "No account yet? Sign up",
    alreadyAccountLogin = "Already registered? Log in", createAccountTitle = "Create an account", fullName = "Full name", phone = "Phone", email = "Email",
    passwordLabel = "Password", confirmPasswordLabel = "Confirm password", acceptTerms = "I accept the Terms of Use", andPrivacy = "and the Privacy Policy", loginBtn = "Log in",
    signupBtn = "Sign up", chooseYourCity = "Choose your city", cityLabel = "City", selectYourCity = "Select your city", receiveCodeSms = "You will receive an SMS code",
    sentTo = "Sent to +237 %s", sixDigitCode = "6-digit code", codeValidFor = "Code valid for %ds", verify = "Verify", resendCode = "Resend code",
    changeNumber = "Change number", other = "Other", sendCode = "Send code", chooseCityRequired = "Please choose your city before continuing", nameRequired = "Name is required",
    phoneRequired = "Phone is required", invalidPhone = "Invalid number (8+ digits)", emailRequired = "Email is required", invalidEmail = "Invalid email", passwordRequired = "Password is required",
    passwordTooShort = "Minimum 4 characters", confirmRequired = "Confirmation is required", passwordsDontMatch = "Passwords do not match", acceptTermsRequired = "Please accept the Terms of Use", invalidNumberExample = "Invalid number (e.g. 691234567)",
    codeTooShort = "Code too short", incorrectCode = "Incorrect code", serverLoginFailed = "Server login failed", googleLoginFailed = "Google login failed or cancelled", sendingError = "Error sending",
    authErrorPrefix = "Error"
) else AuthStrings(
    signIn = "Se connecter", signUp = "S'inscrire", authOtpTitle = "Vérification OTP", phoneLogin = "Connexion par téléphone", loginContinue = "Connectez-vous pour continuer",
    createYourAccount = "Créez votre compte", continueWithGoogle = "Continuer avec Google", useEmailPassword = "Utiliser Email / Mot de passe", or = "ou", noAccountSignup = "Pas encore de compte ? S'inscrire",
    alreadyAccountLogin = "Déjà inscrit ? Se connecter", createAccountTitle = "Créer un compte", fullName = "Nom complet", phone = "Téléphone", email = "Email",
    passwordLabel = "Mot de passe", confirmPasswordLabel = "Confirmer le mot de passe", acceptTerms = "J'accepte les Conditions d'Utilisation", andPrivacy = "et la Politique de Confidentialité", loginBtn = "Connexion",
    signupBtn = "S'inscrire", chooseYourCity = "Choisissez votre ville", cityLabel = "Ville", selectYourCity = "Sélectionnez votre ville", receiveCodeSms = "Vous recevrez un code par SMS",
    sentTo = "Envoyé au +237 %s", sixDigitCode = "Code à 6 chiffres", codeValidFor = "Code valide pendant %ds", verify = "Vérifier", resendCode = "Renvoyer le code",
    changeNumber = "Changer le numéro", other = "Autre", sendCode = "Envoyer le code", chooseCityRequired = "Veuillez choisir votre ville avant de continuer", nameRequired = "Le nom est requis",
    phoneRequired = "Le téléphone est requis", invalidPhone = "Numéro invalide (8+ chiffres)", emailRequired = "L'email est requis", invalidEmail = "Email invalide", passwordRequired = "Le mot de passe est requis",
    passwordTooShort = "Minimum 4 caractères", confirmRequired = "La confirmation est requise", passwordsDontMatch = "Les mots de passe ne correspondent pas", acceptTermsRequired = "Veuillez accepter les Conditions d'Utilisation", invalidNumberExample = "Numéro invalide (ex: 691234567)",
    codeTooShort = "Code trop court", incorrectCode = "Code incorrect", serverLoginFailed = "La connexion au serveur a échoué", googleLoginFailed = "Connexion Google échouée ou annulée", sendingError = "Erreur d'envoi",
    authErrorPrefix = "Erreur"
)

private fun getCheckoutStrings(lang: String) = if (lang == "en") CheckoutStrings(
    cartTitle = "My Cart", removeFromCart = "Remove from cart", removeFromCartConfirm = "Remove « %s » from cart?", remove = "Remove", emptyCartTitle = "Your cart is empty",
    emptyCartHint = "Add products from the home page", finalizeOrder = "Finalize my order", addressLabel = "Address", contactPhone = "Contact phone", paymentMode = "Payment mode",
    payOnDelivery = "Pay on delivery", payOnDeliveryHint = "You pay in cash on receipt", directToVendor = "Pay directly to the seller", directToVendorHint = "You pay the seller by Mobile Money", paymentInstructions = "Payment instructions",
    transferInstructions = "Transfer the amount of %d FCFA to the seller's Mobile Money number below, then click « %s ». The seller will validate your payment.", amountToTransfer = "Amount to transfer", payAndOrder = "Order & pay", paymentMethod = "Payment method", cashOnDelivery = "Cash on delivery",
    promoCode = "Promo code", enterCode = "Enter your code", apply = "Apply", promoInvalid = "Invalid code", promoValidationError = "Validation error",
    promoApplied = "✅ Discount of %d FCFA applied!", articles = "Articles", subtotal = "Subtotal", discount = "Discount", deliveryFee = "Delivery fee",
    instructions = "Instructions", notesForVendor = "Notes for the seller (optional)", copy = "Copy"
) else CheckoutStrings(
    cartTitle = "Mon Panier", removeFromCart = "Retirer du panier", removeFromCartConfirm = "Retirer « %s » du panier ?", remove = "Retirer", emptyCartTitle = "Votre panier est vide",
    emptyCartHint = "Ajoutez des produits depuis l'accueil", finalizeOrder = "Finaliser ma commande", addressLabel = "Adresse", contactPhone = "Téléphone de contact", paymentMode = "Mode de paiement",
    payOnDelivery = "Payer à la livraison", payOnDeliveryHint = "Vous payez en espèces à la réception", directToVendor = "Payer directement au vendeur", directToVendorHint = "Vous réglez le vendeur par Mobile Money", paymentInstructions = "Instructions de paiement",
    transferInstructions = "Transférez le montant de %d FCFA sur le numéro Mobile Money du vendeur ci-dessous, puis cliquez sur « %s ». Le vendeur validera votre paiement.", amountToTransfer = "Montant à transférer", payAndOrder = "Commander & payer", paymentMethod = "Moyen de paiement", cashOnDelivery = "Cash à la livraison",
    promoCode = "Code promo", enterCode = "Entrez votre code", apply = "Appliquer", promoInvalid = "Code invalide", promoValidationError = "Erreur de validation",
    promoApplied = "✅ Réduction de %d FCFA appliquée !", articles = "Articles", subtotal = "Sous-total", discount = "Réduction", deliveryFee = "Frais de livraison",
    instructions = "Instructions", notesForVendor = "Notes pour le vendeur (optionnel)", copy = "Copier"
)

private fun getOrderStrings(lang: String) = if (lang == "en") OrderStrings(
    myOrders = "My orders", noOrders = "No orders", noOrdersHint = "Your orders will appear here", orderCancelled = "Order cancelled", orderActionError = "action failed",
    receptionConfirmed = "✅ Receipt confirmed! The order will be finalized after seller validation.", cancelOrderTitle = "Cancel this order?", cancelOrderConfirmText = "Are you sure you want to cancel order %s ?", confirmReceptionTitle = "Confirm receipt?", confirmReceptionText = "Do you confirm you received all items of order %s ?",
    estimatedDelivery = "Estimated delivery: %s", waitingForVendor = "Waiting for seller", transferWait = "Transfer the amount to the seller's Mobile Money number, then wait for their validation.", orderCreated = "Order created", paymentValidated = "Payment validated",
    orderConfirmed = "Order confirmed", orderPreparing = "Preparing", orderDelivering = "Out for delivery", orderTracking = "Order tracking", awaitingSellerConfirmation = "Awaiting seller confirmation",
    confirmReception = "Confirm receipt", cancelOrder = "Cancel order", contactSeller = "Contact seller", no = "No", yes = "Yes",
    deleteConfirm = "Yes, delete", delivery = "Delivery", amount = "Amount", orderDelivered = "Delivered"
) else OrderStrings(
    myOrders = "Mes commandes", noOrders = "Aucune commande", noOrdersHint = "Vos commandes apparaîtront ici", orderCancelled = "Commande annulée", orderActionError = "l'action a échoué",
    receptionConfirmed = "✅ Réception confirmée ! La commande sera finalisée après validation du vendeur.", cancelOrderTitle = "Annuler cette commande ?", cancelOrderConfirmText = "Êtes-vous sûr de vouloir annuler la commande %s ?", confirmReceptionTitle = "Confirmer la réception ?", confirmReceptionText = "Confirmez-vous avoir reçu tous les articles de la commande %s ?",
    estimatedDelivery = "Livraison estimée : %s", waitingForVendor = "En attente du vendeur", transferWait = "Effectuez le transfert sur le numéro Mobile Money du vendeur, puis attendez sa validation.", orderCreated = "Commande créée", paymentValidated = "Paiement validé",
    orderConfirmed = "Commande confirmée", orderPreparing = "En préparation", orderDelivering = "En cours de livraison", orderTracking = "Suivi de commande", awaitingSellerConfirmation = "En attente de confirmation vendeur",
    confirmReception = "Confirmer réception", cancelOrder = "Annuler commande", contactSeller = "Contacter vendeur", no = "Non", yes = "Oui",
    deleteConfirm = "Oui, supprimer", delivery = "Livraison", amount = "Montant", orderDelivered = "Livrée"
)

private fun getHomeStrings(lang: String) = if (lang == "en") HomeStrings(
    registerShort = "Sign up", products = "Products", shops = "Shops", compareCta = "Compare (%d)", comparateur = "Comparator",
    compareSelection = "Comparator (%d/4)", compareEmptyHint = "Add 2 to 4 products to compare", compareEmptyHint2 = "Select \"Compare\" on a product", searchCompareHint = "Search a product to compare...", addCompareHint = "Add a product...",
    alreadyAdded = "Already added", price = "Price", stock = "Stock", units = "%d units", outOfStock = "Out of stock",
    bestPrice = "Best price", bestChoice = "Best choice", optimalValue = "Optimal value for money", view = "View", arrivalsToday = "✨ Today's arrivals",
    addStory = "Add a story", addStoryHint = "Choose the type of story to publish.", video = "Video", textOnly = "Text only", addCaption = "Add a caption",
    addCaptionHint = "Do you want to add a message to your story?", yourMessage = "Your message...", publish = "Publish", skip = "Skip", textStory = "Text story",
    whatDoYouWantToSay = "What do you want to say?", backgroundColor = "Background color:", filters = "Filters", reset = "Reset", sortBy = "Sort by:",
    newest = "Newest", priceAsc = "Price ↑", priceDesc = "Price ↓", noProductsFound = "No products found", noProductsFoundHint = "Check your connection or filters",
    favorites = "Favorites", articlesCount = "%d items", add = "Add", story = "Story"
) else HomeStrings(
    registerShort = "S'inscrire", products = "Produits", shops = "Boutiques", compareCta = "Comparer (%d)", comparateur = "Comparateur",
    compareSelection = "Comparateur (%d/4)", compareEmptyHint = "Ajoutez 2 à 4 produits pour comparer", compareEmptyHint2 = "Sélectionnez \"Comparer\" sur un produit", searchCompareHint = "Chercher un produit à comparer...", addCompareHint = "Ajouter un produit...",
    alreadyAdded = "Déjà ajouté", price = "Prix", stock = "Stock", units = "%d unités", outOfStock = "Rupture de stock",
    bestPrice = "Meilleur prix", bestChoice = "Meilleur choix", optimalValue = "Rapport qualité-prix optimal", view = "Voir", arrivalsToday = "✨ Arrivages du jour",
    addStory = "Ajouter une story", addStoryHint = "Choisissez le type de story à publier.", video = "Vidéo", textOnly = "Texte uniquement", addCaption = "Ajouter une légende",
    addCaptionHint = "Voulez-vous ajouter un message à votre story ?", yourMessage = "Votre message...", publish = "Publier", skip = "Passer", textStory = "Story texte",
    whatDoYouWantToSay = "Que voulez-vous dire ?", backgroundColor = "Couleur de fond :", filters = "Filtres", reset = "Réinitialiser", sortBy = "Trier par :",
    newest = "Plus récents", priceAsc = "Prix croissant", priceDesc = "Prix décroissant", noProductsFound = "Aucun produit trouvé", noProductsFoundHint = "Vérifiez votre connexion ou les filtres",
    favorites = "Favoris", articlesCount = "%d articles", add = "Ajouter", story = "Story"
)

private fun getProfileStrings(lang: String) = if (lang == "en") ProfileStrings(
    myAccount = "My Account", logoutConfirmTitle = "Log out", logoutConfirmText = "Do you really want to log out?", defaultUser = "User", editProfile = "Edit profile",
    seeAll = "See all", myWallet = "My Wallet", myServices = "My Services", connect = "Sign in", loginRequiredHint = "To access your orders, favorites and settings",
    points = "Points", cashback = "Cashback", level = "Level", toPay = "To pay", toShip = "To ship",
    toReceive = "To receive", myFavorites = "Favorites", followed = "Following", coupons = "Coupons", groupBuys = "Group buys",
    vendorSpace = "Vendor Space", vendorSpaceSubtitle = "Manage your products and sales", admin = "Administration", adminSubtitle = "Manage the platform", balance = "Balance"
) else ProfileStrings(
    myAccount = "Mon Compte", logoutConfirmTitle = "Déconnexion", logoutConfirmText = "Voulez-vous vraiment vous déconnecter ?", defaultUser = "Utilisateur", editProfile = "Modifier le profil",
    seeAll = "Tout voir", myWallet = "Mon Portefeuille", myServices = "Mes Services", connect = "Se connecter", loginRequiredHint = "Pour accéder à vos commandes, favoris et paramètres",
    points = "Points", cashback = "Cashback", level = "Niveau", toPay = "À payer", toShip = "À expédier",
    toReceive = "À recevoir", myFavorites = "Mes favoris", followed = "Suivis", coupons = "Mes coupons", groupBuys = "Mes groupes",
    vendorSpace = "Espace Vendeur", vendorSpaceSubtitle = "Gérez vos produits et vos ventes", admin = "Administration", adminSubtitle = "Gérer la plateforme", balance = "Solde"
)

private fun getProductDetailStrings(lang: String) = if (lang == "en") ProductDetailStrings(
    productDetails = "Product details", soldCount = "%d sold", alreadyBought = "Bought %d times", onlyLeft = "Only %d left!", bestSeller = "#1 best seller",
    color = "Color", reviewsAndRatings = "Reviews & Ratings", similarProducts = "Similar products", noSimilarProducts = "No similar products", reportSent = "✅ Report sent. Thank you for contributing to TiK-Market's quality.",
    reportCommentPlaceholder = "Comment (optional)", close = "Close", sendReport = "Send report", rating = "Rating", sales = "Sales",
    orderItemsCount = "%d item(s)", backToOrders = "Back to orders", markAllRead = "Mark all as read"
) else ProductDetailStrings(
    productDetails = "Détails du produit", soldCount = "%d vendus", alreadyBought = "Acheté %d fois", onlyLeft = "Plus que %d en stock !", bestSeller = "N°1 des ventes",
    color = "Couleur", reviewsAndRatings = "Avis & Notes", similarProducts = "Produits similaires", noSimilarProducts = "Aucun produit similaire", reportSent = "✅ Signalement envoyé. Merci de contribuer à la qualité de TiK-Market.",
    reportCommentPlaceholder = "Commentaire (optionnel)", close = "Fermer", sendReport = "Envoyer le signalement", rating = "Note", sales = "Ventes",
    orderItemsCount = "%d article(s)", backToOrders = "Retour aux commandes", markAllRead = "Tout marquer comme lu"
)

private fun getChatNotifStrings(lang: String) = if (lang == "en") ChatNotifStrings(
    deleteMessage = "Delete message", deleteMessageConfirm = "Do you really want to delete this message?", you = "You", productLabel = "Product", reply = "Reply",
    react = "React", reactToMessage = "React to message", shareLocation = "📍 Share my location", currentPosition = "Current position", map = "🗺️ Map",
    gallery = "Gallery", camera = "Camera", localization = "Location", messageCenter = "Message center", noMessages = "No messages",
    noResultsFor = "No results for '%s'", deleteConversation = "Delete conversation", deleteConversationConfirm = "Do you really want to delete the conversation with %s ?\nMessages will be permanently lost.", searchContacts = "Search a contact..."
) else ChatNotifStrings(
    deleteMessage = "Supprimer le message", deleteMessageConfirm = "Voulez-vous vraiment supprimer ce message ?", you = "Vous", productLabel = "Produit", reply = "Répondre",
    react = "Réagir", reactToMessage = "Réagir au message", shareLocation = "📍 Partager ma position", currentPosition = "Position actuelle", map = "🗺️ Carte",
    gallery = "Galerie", camera = "Caméra", localization = "Localisation", messageCenter = "Centre de messagerie", noMessages = "Aucun message",
    noResultsFor = "Aucun résultat pour '%s'", deleteConversation = "Supprimer la discussion", deleteConversationConfirm = "Voulez-vous vraiment supprimer la discussion avec %s ?\nLes messages seront perdus définitivement.", searchContacts = "Rechercher un contact..."
)

private fun getLoyaltyStrings(lang: String) = if (lang == "en") LoyaltyStrings(
    loyaltyProgram = "Loyalty program", dataUpdated = "Data updated", redeemPoints = "Redeem points", recharge = "Recharge", history = "History",
    myCoupons = "My coupons (%d)", noTransactions = "No transactions", noCoupons = "No coupons", advantagesPerTier = "Per-tier benefits", cardLabel = "%s card",
    cashbackBalanceLabel = "Available cashback balance", pointsUsable = "Usable points", pointsAccumulated = "Accumulated points", nextLevel = "Next level: %s", pointsToReach = "Only %d points left to reach %s",
    earned = "Earned", spent = "Spent", rechargeLabel = "Recharge", cashbackLabel = "Cashback", bonusLabel = "Bonus", refund = "Refund",
    fcfaDiscount = "%d FCFA off", pctDiscount = "%d%% off", expiresOn = "Expires on %s", redeemMyPoints = "Redeem my points", youHavePoints = "You have %d points",
    pointsToExchange = "Points to exchange", pointsValue = "100 points = 500 FCFA. Value: %d FCFA", exchange = "Exchange", rechargeWallet = "Recharge my wallet", amountFcfa = "Amount (FCFA)",
    couponGenerated = "Coupon %s generated!", errorRedeem = "Error during redemption", rechargeDone = "Recharge of %d FCFA completed", errorRecharge = "Recharge error", active = "ACTIVE"
) else LoyaltyStrings(
    loyaltyProgram = "Programme de fidélité", dataUpdated = "Données actualisées", redeemPoints = "Échanger des points", recharge = "Recharge", history = "Historique",
    myCoupons = "Mes coupons (%d)", noTransactions = "Aucune transaction", noCoupons = "Aucun coupon", advantagesPerTier = "Avantages par niveau", cardLabel = "%s card",
    cashbackBalanceLabel = "Solde cashback disponible", pointsUsable = "Points utilisables", pointsAccumulated = "Points cumulés", nextLevel = "Prochain niveau: %s", pointsToReach = "Encore %d points pour atteindre %s",
    earned = "Gagné", spent = "Dépensé", rechargeLabel = "Recharge", cashbackLabel = "Cashback", bonusLabel = "Bonus", refund = "Remboursement",
    fcfaDiscount = "%d FCFA de réduction", pctDiscount = "%d%% de réduction", expiresOn = "Expire le %s", redeemMyPoints = "Échanger mes points", youHavePoints = "Vous avez %d points",
    pointsToExchange = "Points à échanger", pointsValue = "100 points = 500 FCFA. Valeur: %d FCFA", exchange = "Échanger", rechargeWallet = "Recharger mon portefeuille", amountFcfa = "Montant (FCFA)",
    couponGenerated = "Coupon %s généré !", errorRedeem = "Erreur lors de l'échange", rechargeDone = "Recharge de %d FCFA effectuée", errorRecharge = "Erreur de recharge", active = "ACTIF"
)

private fun getNotifPrefsStrings(lang: String) = if (lang == "en") NotifPrefsStrings(
    notifPrefsTitle = "Notification preferences", pushNotifs = "Push notifications", pushNotifsDesc = "Enable or disable the types of notifications you would like to receive.", newProducts = "New products", newProductsDesc = "Promotions and new arrivals",
    orderUpdates = "Order updates", orderUpdatesDesc = "Status of your orders", promoOffers = "Promotional offers", promoOffersDesc = "Discounts and special offers", messagesToggle = "Messages",
    messagesToggleDesc = "Chat notifications", systemToggle = "System", systemToggleDesc = "General information", pushEnabled = "Push enabled", pushEnabledDesc = "Receive notifications even in the background",
    prefsSaved = "Preferences saved", errorSaving = "Error while saving", save = "Save"
) else NotifPrefsStrings(
    notifPrefsTitle = "Préférences de notification", pushNotifs = "Notifications Push", pushNotifsDesc = "Activez ou désactivez les types de notifications que vous souhaitez recevoir.", newProducts = "Nouveaux produits", newProductsDesc = "Promotions et nouveaux arrivages",
    orderUpdates = "Mises à jour des commandes", orderUpdatesDesc = "Statut de vos commandes", promoOffers = "Offres promotionnelles", promoOffersDesc = "Remises et offres spéciales", messagesToggle = "Messages",
    messagesToggleDesc = "Notifications de chat", systemToggle = "Système", systemToggleDesc = "Informations générales", pushEnabled = "Push activé", pushEnabledDesc = "Recevoir les notifications même en arrière-plan",
    prefsSaved = "Préférences enregistrées", errorSaving = "Erreur lors de l'enregistrement", save = "Enregistrer"
)

private fun getSettingsMiscStrings(lang: String) = if (lang == "en") SettingsMiscStrings(
    legalMentions = "Legal Mentions", termsOfUse = "Terms of Use", downloads = "Downloads", androidApk = "Android APK", installApk = "v1.0.0 — Install APK",
    iosApp = "iOS App", comingSoon = "Coming soon", about = "About", next = "Next", start = "🚀 Start",
    allShops = "All shops", shopsIn = "Shops in %s", noShopsFound = "No shops found", follow = "Follow", followers = "followers",
    followersCount = "%d followers", verified = "Verified", shopNotFound = "Shop not found", sold = "Sold", noProductsForNow = "No products right now",
    followShop = "Follow this shop", unfollow = "Unfollow", featuredProducts = "⭐ Featured products", customerReviews = "💬 Customer reviews (%d)",
    noStory = "No story available", storyDeleted = "Story deleted", storyError = "Error: %s", replyToSeller = "Reply to the seller...", msgSentToSeller = "Message sent to the seller",
    vendorNotFound = "Unable to identify the seller", delete = "Delete", version = "Version %s", lastUpdate = "Last updated: %s", ourMissionTitle = "Our Mission",
    contactSupport = "Support Contact", allRights = "© 2024 AUTENTIK SOFT SOLUTIONS SARLU. All rights reserved.", comparatif = "Comparison (%d)", noProductsCompare = "No products to compare", unit = "Unit",
    inStock = "In stock (%d)", description = "Description", shopsMapTitle = "Shops map", searchShop = "Search a shop...", shopClickTip = "Click on a shop to see its products.",
    mapOpensTip = "The \"Map\" button opens Google Maps with the location.", scanProduct = "Scan a product", cameraInit = "Initializing camera...", barcodeHint = "Place the barcode inside the frame to scan it", scanBarcodeTitle = "Scan barcode",
    useCameraHint = "Use your device camera", orTypeManually = "or type the code manually", barcodeLabel = "Barcode", barcodeExample = "e.g. 4901234567890", searchAction = "Search",
    errorPrefix = "Error: %s", noNotifications = "No notifications", noNotificationsHint = "You will be notified here of new features and updates.", emptyFavorites = "No favorites", emptyFavoritesHint = "Heart products from the home page to add them",
    negotiate = "Negotiate", paymentTitle = "Payment", amountToPay = "Amount to pay", phoneNumberPrefix = "Number %s", confirmPayment = "Confirm payment",
    howItWorks = "How does it work?", step1Send = "1. Send the payment", step1SendDesc = "Use the number above to send the amount", step2Confirm = "2. Confirm the request", step2ConfirmDesc = "You will receive a notification on your phone",
    step3Validate = "3. Validate the payment", step3ValidateDesc = "Enter your secret code to approve", processingPayment = "Processing payment", confirmOnPhone = "Please confirm the operation on your phone", paymentDone = "Payment done",
    paymentAlreadyDone = "Payment already done", paymentSuccess = "Payment successful!", orderConfirmedFmt = "Your order %s has been confirmed.", returnToHome = "Back to home", myGroupBuys = "My group buys",
    myParticipations = "My participations", participantsStats = "%d active · %d completed", allFilter = "All", activePlural = "Active", completedPlural = "Completed",
    cancelledPlural = "Cancelled", noParticipation = "No participation", joinGroupHint = "Join group buys on product pages.", originalPrice = "Original price", reduction = "DISCOUNT",
    myPrice = "My price", groupLabel = "Group", participantsCount = "Participants (%d)", anonymous = "Anonymous", filled = "Filled",
    completed = "Completed", cancelled = "Cancelled", profileUpdated = "✅ Profile updated", saveError = "❌ Error: %s", personalInfo = "Personal information",
    locationLabel = "Location (City)", locationPlaceholder = "e.g. Bafoussam", security = "Security", newPassword = "New password", passwordPlaceholder = "Leave empty to keep it unchanged",
    updateProfile = "Update profile", avatarError = "Avatar error: %s", coverError = "Cover error: %s", followedShops = "Followed shops", noFollowedShops = "You are not following any shop",
    unfollowedMsg = "%s removed from followed", unfollowError = "Error: %s", productsCount = "%d products", salesCount = "%d sales", unsubscribe = "Unfollow",
    failed = "failed"
) else SettingsMiscStrings(
    legalMentions = "Mentions Légales", termsOfUse = "Conditions d'Utilisation", downloads = "Téléchargements", androidApk = "Android APK", installApk = "v1.0.0 — Installer l'APK",
    iosApp = "iOS App", comingSoon = "Bientôt disponible", about = "À propos", next = "Suivant", start = "🚀 Démarrer",
    allShops = "Toutes les boutiques", shopsIn = "Boutiques à %s", noShopsFound = "Aucune boutique trouvée", follow = "Suivre", followers = "abonnés",
    followersCount = "%d abonnés", verified = "Vérifiée", shopNotFound = "Boutique non trouvée", sold = "Vendu", noProductsForNow = "Aucun produit pour le moment",
    followShop = "Suivre cette boutique", unfollow = "Ne plus suivre", featuredProducts = "⭐ Produits en avant", customerReviews = "💬 Avis clients (%d)",
    noStory = "Aucune story disponible", storyDeleted = "Story supprimée", storyError = "Erreur : %s", replyToSeller = "Répondre au vendeur...", msgSentToSeller = "Message envoyé au vendeur",
    vendorNotFound = "Impossible d'identifier le vendeur", delete = "Supprimer", version = "Version %s", lastUpdate = "Dernière mise à jour : %s", ourMissionTitle = "Notre Mission",
    contactSupport = "Contact Support", allRights = "© 2024 AUTENTIK SOFT SOLUTIONS SARLU. Tous droits réservés.", comparatif = "Comparatif (%d)", noProductsCompare = "Aucun produit à comparer", unit = "Unité",
    inStock = "En stock (%d)", description = "Description", shopsMapTitle = "Carte des boutiques", searchShop = "Chercher une boutique...", shopClickTip = "Cliquez sur une boutique pour voir ses produits.",
    mapOpensTip = "Le bouton \"Carte\" ouvre Google Maps avec la position.", scanProduct = "Scanner un produit", cameraInit = "Initialisation de la caméra...", barcodeHint = "Placez le code-barres dans le cadre pour le scanner", scanBarcodeTitle = "Scan code-barres",
    useCameraHint = "Utilisez la caméra de votre appareil", orTypeManually = "ou saisissez le code manuellement", barcodeLabel = "Code-barres", barcodeExample = "Ex: 4901234567890", searchAction = "Chercher",
    errorPrefix = "Erreur: %s", noNotifications = "Aucune notification", noNotificationsHint = "Vous serez averti ici des nouveautés et mises à jour.", emptyFavorites = "Aucun favori", emptyFavoritesHint = "Ajoutez des produits en cœur depuis l'accueil",
    negotiate = "Négocier", paymentTitle = "Paiement", amountToPay = "Montant à payer", phoneNumberPrefix = "Numéro %s", confirmPayment = "Confirmer le paiement",
    howItWorks = "Comment ça marche ?", step1Send = "1. Envoyez le paiement", step1SendDesc = "Utilisez le numéro ci-dessus pour envoyer le montant", step2Confirm = "2. Confirmez la demande", step2ConfirmDesc = "Vous recevrez une notification sur votre téléphone",
    step3Validate = "3. Validez le paiement", step3ValidateDesc = "Entrez votre code secret pour approuver", processingPayment = "Traitement du paiement", confirmOnPhone = "Veuillez confirmer l'opération sur votre téléphone", paymentDone = "Paiement effectué",
    paymentAlreadyDone = "Paiement déjà effectué", paymentSuccess = "Paiement réussi !", orderConfirmedFmt = "Votre commande %s a été confirmée.", returnToHome = "Retour à l'accueil", myGroupBuys = "Mes achats groupés",
    myParticipations = "Mes participations", participantsStats = "%d actifs · %d terminés", allFilter = "Tous", activePlural = "Actifs", completedPlural = "Terminés",
    cancelledPlural = "Annulés", noParticipation = "Aucune participation", joinGroupHint = "Rejoignez des achats groupés sur les fiches produits.", originalPrice = "Prix original", reduction = "RÉDUCTION",
    myPrice = "Mon prix", groupLabel = "Groupe", participantsCount = "Participants (%d)", anonymous = "Anonyme", filled = "Rempli",
    completed = "Terminé", cancelled = "Annulé", profileUpdated = "✅ Profil mis à jour", saveError = "❌ Erreur: %s", personalInfo = "Informations personnelles",
    locationLabel = "Localisation (Ville)", locationPlaceholder = "ex: Bafoussam", security = "Sécurité", newPassword = "Nouveau mot de passe", passwordPlaceholder = "Laisser vide pour ne pas changer",
    updateProfile = "Mettre à jour le profil", avatarError = "Erreur avatar: %s", coverError = "Erreur couverture: %s", followedShops = "Boutiques suivies", noFollowedShops = "Vous ne suivez aucune boutique",
    unfollowedMsg = "%s retiré des suivis", unfollowError = "Erreur: %s", productsCount = "%d produits", salesCount = "%d ventes", unsubscribe = "Se désabonner",
    failed = "échec"
)

private fun getVendorStrings(lang: String) = if (lang == "en") VendorStrings(
    createMyShop = "Create my shop", addShopPhoto = "Add a shop photo", shopInfo = "Shop information", shopNameRequired = "Shop name *", shopPhoneRequired = "Phone *",
    locationRequiredField = "Location *", chooseOnMap = "Choose on the map", suggestions = "Suggestions:", categoryRequired = "Category *", errShopName = "Please enter the shop name",
    errPhoneField = "Please enter the phone number", errLocationField = "Please enter the location", errCategoryField = "Please select a category", errGeneric = "An error occurred", editProductTitle = "Edit product",
    newProduct = "New product", productPhotos = "Product photos", productInfo = "Product information", productTitleField = "Product title *", oldPrice = "Old price",
    publishStory = "Publish as a story", publishStoryDesc = "The product will appear in the story section of the home page", mySubscribers = "My Subscribers", searchSubscriber = "Search a subscriber...", noSubscriberFound = "No subscriber found",
    subscriber = "Subscriber", revenue = "Revenue", noStatsAvailable = "No statistics available", stockAlerts = "Stock alerts", lowStock = "%d product(s) almost out of stock",
    outOfStockCount = "%d product(s) out of stock", updateStock = "Update stock", revenue7d = "Revenue (7 days)", monthlyRevenue = "Monthly revenue", ordersByStatus = "Orders by status",
    quickActions = "Quick actions", addPlus = "+ Add", topProducts = "Top products", addFirstProduct = "Add my first product", allMyProducts = "All my products",
    noProductsListed = "No products listed.", manageShop = "Manage shop", viewOrders = "View orders", myGroupBuysMenus = "Group buys", viewSubscribers = "View subscribers",
    exportCsv = "Export CSV", csvOrders = "Orders CSV", csvRevenue = "Revenue CSV", addNewLineProduct = "Add\nproduct", dashboardTitle = "Dashboard",
    manageOrders = "Order management", unknownError = "Unknown error", updateError = "Error during update", noOrdersNow = "No orders right now", customerInfo = "Customer info",
    shopShare = "Shop share", confirmOrder = "Confirm the order", startPrep = "Start preparation", readyForDelivery = "Ready for delivery", confirmFinalDelivery = "Confirm final delivery",
    awaitingReceipt = "Awaiting receipt by the client", telPrefix = "Tel: %s", deliveryPrefix = "Delivery: %s", manageShopTitle = "Manage shop", done = "Done",
    edit = "Edit", editShopName = "Shop name", editShopLocation = "Location (District/Street)", shopUpdated = "Shop updated ✓", shopUpdateError = "Error: %s",
    addFirstProductHint = "Add your first product!", inStockShort = "in stock", productsCountLabel = "%d products", interactionsCustomers = "Customer interactions", noLikes = "No likes",
    noSubscribers = "No subscribers", groupBuysTitle = "Group buys", launchGroup = "Launch a group", noGroupBuys = "No group buy", noGroupBuysHint = "You can launch group buys on your products\nto boost your sales.",
    summary = "Summary", filledPlural = "Filled", participantsLabel = "Participants", groupBuyCancelled = "Group buy cancelled", groupBuyDeleted = "Group buy deleted",
    groupBuyLaunched = "Group buy launched!", errorCreatingGroup = "Error during creation", notificationSentParticipants = "Notification sent to participants", productFallback = "Product #%s", byCreator = "By %s",
    creatorAnonymous = "A buyer", participantCountFmt = "%d participant(s)", progress = "Progress", groupPrice = "Group price", groupExpiry = "Expires on %s",
    cancelGroup = "Cancel group", notifyAll = "Notify all", newGroupBuy = "New group buy", chooseProductOffer = "Choose a product and the offer conditions.", selectProduct = "Select a product",
    minParticipants = "Min. participants", discountPercent = "Discount %", offerDuration = "Offer duration (hours)", finalClientPrice = "Final client price: %s", launchOffer = "Launch the offer",
    notifyParticipantsTitle = "Notify participants", sendNotificationParticipants = "Send a notification to the %d participants.", notifTitle = "Title", messageLabel = "Message", groupBuyNotifTitle = "Group buy: %s"
) else VendorStrings(
    createMyShop = "Créer ma boutique", addShopPhoto = "Ajouter une photo de boutique", shopInfo = "Informations de la boutique", shopNameRequired = "Nom de la boutique *", shopPhoneRequired = "Téléphone *",
    locationRequiredField = "Localisation *", chooseOnMap = "Choisir sur la carte", suggestions = "Suggestions :", categoryRequired = "Catégorie *", errShopName = "Veuillez saisir le nom de la boutique",
    errPhoneField = "Veuillez saisir le numéro de téléphone", errLocationField = "Veuillez saisir la localisation", errCategoryField = "Veuillez sélectionner une catégorie", errGeneric = "Une erreur est survenue", editProductTitle = "Modifier le produit",
    newProduct = "Nouveau produit", productPhotos = "Photos du produit", productInfo = "Informations produit", productTitleField = "Titre du produit *", oldPrice = "Ancien prix",
    publishStory = "Publier en story", publishStoryDesc = "Le produit apparaîtra dans la section story de l'accueil", mySubscribers = "Mes Abonnés", searchSubscriber = "Rechercher un abonné...", noSubscriberFound = "Aucun abonné trouvé",
    subscriber = "Abonné", revenue = "Chiffre d'affaires", noStatsAvailable = "Aucune statistique disponible", stockAlerts = "Alertes stock", lowStock = "%d produit(s) presque en rupture",
    outOfStockCount = "%d produit(s) en rupture", updateStock = "Mettre à jour le stock", revenue7d = "CA (7 derniers jours)", monthlyRevenue = "Chiffre d'affaires mensuel", ordersByStatus = "Commandes par statut",
    quickActions = "Actions rapides", addPlus = "+ Ajouter", topProducts = "Top produits", addFirstProduct = "Ajouter mon premier produit", allMyProducts = "Tous mes produits",
    noProductsListed = "Aucun produit listé.", manageShop = "Gérer boutique", viewOrders = "Voir commandes", myGroupBuysMenus = "Achats groupés", viewSubscribers = "Voir abonnés",
    exportCsv = "Exporter CSV", csvOrders = "Commandes CSV", csvRevenue = "CA CSV", addNewLineProduct = "Ajouter\nproduit", dashboardTitle = "Tableau de bord",
    manageOrders = "Gestion des commandes", unknownError = "Erreur inconnue", updateError = "Erreur lors de la mise à jour", noOrdersNow = "Aucune commande pour le moment", customerInfo = "Infos client",
    shopShare = "Partage boutique", confirmOrder = "Confirmer la commande", startPrep = "Démarrer préparation", readyForDelivery = "Prêt pour livraison", confirmFinalDelivery = "Confirmer livraison finale",
    awaitingReceipt = "En attente réception client", telPrefix = "Tél: %s", deliveryPrefix = "Livraison: %s", manageShopTitle = "Gérer la boutique", done = "Terminé",
    edit = "Modifier", editShopName = "Nom de la boutique", editShopLocation = "Localisation (Quartier/Rue)", shopUpdated = "Boutique mise à jour ✓", shopUpdateError = "Erreur : %s",
    addFirstProductHint = "Ajoutez votre premier produit !", inStockShort = "en stock", productsCountLabel = "%d produits", interactionsCustomers = "Interactions clients", noLikes = "Aucun like",
    noSubscribers = "Aucun abonné", groupBuysTitle = "Achats groupés", launchGroup = "Lancer un groupe", noGroupBuys = "Aucun achat groupé", noGroupBuysHint = "Vous pouvez lancer des achats groupés sur vos produits\npour booster vos ventes.",
    summary = "Résumé", filledPlural = "Remplis", participantsLabel = "Participants", groupBuyCancelled = "Achat groupé annulé", groupBuyDeleted = "Achat groupé supprimé",
    groupBuyLaunched = "Achat groupé lancé !", errorCreatingGroup = "Erreur lors de la création", notificationSentParticipants = "Notification envoyée aux participants", productFallback = "Produit #%s", byCreator = "Par %s",
    creatorAnonymous = "Un acheteur", participantCountFmt = "%d participant(s)", progress = "Progression", groupPrice = "Prix groupe", groupExpiry = "Expire le %s",
    cancelGroup = "Annuler le groupe", notifyAll = "Notifier tous", newGroupBuy = "Nouvel achat groupé", chooseProductOffer = "Choisissez un produit et les conditions de l'offre.", selectProduct = "Sélectionner un produit",
    minParticipants = "Participants min.", discountPercent = "Réduction %", offerDuration = "Durée de l'offre (heures)", finalClientPrice = "Prix final client : %s", launchOffer = "Lancer l'offre",
    notifyParticipantsTitle = "Notifier les participants", sendNotificationParticipants = "Envoyer une notification aux %d participants.", notifTitle = "Titre", messageLabel = "Message", groupBuyNotifTitle = "Achat groupé : %s"
)

private fun getAdminStrings(lang: String) = if (lang == "en") AdminStrings(
    manageAccounts = "Manage accounts", activeUsers = "Active users", verifyManage = "Verify and manage", broadcastMessages = "Broadcast messages", statsKPIs = "Statistics and KPIs",
    ephemeralContent = "Ephemeral content", homeBanners = "Home banners", totalControl = "Total control", usersLabel = "Users", onlineLabel = "Online",
    storiesLabel = "Stories", promoHeroLabel = "Promo Hero", superAdminLabel = "Super Admin", adminConnError = "Connection error to the admin server.", addUserTitle = "Add a user",
    role = "Role", clientLabel = "Client", adminLabel = "Admin", superLabel = "Super", managedCityOptional = "Managed city (optional)",
    globalAdminHint = "Leave empty for a global admin.", allFieldsRequired = "❌ All fields are required", userCreatedSuccess = "✅ User %s created successfully!", adminErrPrefix = "❌ Error: %s", promoCodeRequired = "❌ Code and discount are required",
    promoAtShop = "🎉 Promotion at %s", promoReductPct = "Discount % (e.g. 10)", promoFixedFcfa = "Fixed FCFA discount", promoMinAmountFcfa = "Minimum amount (FCFA)", promoCreatedNotified = "✅ Promotion created and notified to everyone!",
    createNotify = "Create & Notify", roleChangeError = "Role change error: %s", deleteErrorPrefix = "Delete error: %s", notifSent = "✅ Notification sent!", sendFailed = "❌ Send failed",
    sendErrPrefix = "❌ Failed: %s", sendHistory = "Send history", noHistory = "No history", andOthers = "And %d more...", systemNotif = "System notification",
    receivedByAll = "Will be received by all users.", individualNotif = "Individual notification", sendToSpecific = "Send to a specific user.", searchUserMin = "Search a user (min 2)", noUserFound = "No user found",
    change = "Change", notifBroadcastAll = "✅ Notification broadcast to all users", broadcastAll = "Broadcast to all", sendTo = "Send to %s", bannedLabel = "Banned",
    sendNotifMenuItem = "Send notification", roleSuperAdmin = "Super Admin role", roleAdmin = "Admin role", roleVendor = "Vendor role", roleClient = "Client role",
    reactivate = "Reactivate", ban = "Ban", unverify = "Unverify", verifyAction = "Verify", removePromo = "Remove promo",
    featureShop = "Feature shop", createPromo = "Create promotion", deleteShopTitle = "Delete shop?", deleteShopConfirm = "Are you sure you want to delete « %s » ?\nAll related products and orders will be permanently deleted.", filterByCity = "Filter by city",
    overview = "Overview", clientsLabel = "Clients", vendorsLabel = "Vendors", ordersLabel = "Orders", totalRevenueLabel = "Total CA",
    todayLabel = "Today", alerts = "Alertes", pendingVerifyShops = "%d shops pending verification", goToShopsVerify = "Go to the Shops tab to verify them", bannedShopsLabel = "%d banned shops",
    checkShopsDetails = "Check the Shops tab for more details", registrations30 = "Registrations (30 days)", newUsersMonth = "+%d new users this month", monthlyRevenue12 = "Monthly revenue (12 months)", topVendorsCA = "Top vendors (CA)",
    orderCountFmt = "%d orders", topProductsSold = "Top sold products", usersByRole = "Users by role", onlineUsersTitle = "Online users", refreshLabel = "Refresh",
    onlineUsersNow = "%d users currently online", noOneOnline = "No one online right now", secondsAgo = "%ds ago", allStories = "All stories", noStoryNow = "No story right now",
    deleteStoryError = "Delete error: %s", newStoryAdmin = "New Story (Admin)", mediaPickLabel = "Media (Photo or Video max 30s)", mediaAdjustNote = "Note: Adjustment is automatic (30s max)", selectMedia = "Select Media",
    captionOptional = "Caption (Optional)", replyCountFmt = "%d replies", heroSectionMgmt = "Hero Section management", heroModifyHint = "Edit the promotional banners of the home page.", addPromotion = "Add a promotion",
    heroTitleExample = "Title (e.g. Local flavors)", heroSubtitleExample = "Subtitle (e.g. Local fruits)", heroMediaLabel = "Image or Video (max 10s)", orDirectUrl = "Or direct URL", shopToPromote = "Shop to promote (optional)",
    addToHome = "Add to home", activeBanners = "Active banners", noCustomBanner = "No custom banner.", linkPrefix = "Link: %s", mediaUploadError = "Media upload: %s",
    noShopSelected = "No shop", noneLabel = "None", create = "Create", notifyUser = "Notify: %s", promoNotifBody = "Use the code \"%s\" to get %s on %s products!",
    superAdminPanel = "Super Admin Panel", systemConfig = "System Configuration", globalStats = "Global Statistics", reportsCount = "Reports (%d)", globalBroadcast = "Global Broadcast",
    broadcast = "Broadcast", appVersionLabel = "App Version", minVersionRequired = "Minimum Version Required", commissionRate = "Commission Rate", maintenanceMode = "Mode Maintenance",
    activeProducts = "Active Products", globalCA = "Global CA", reportTypeLabel = "Report %s", byReporter = "By: %s", reasonPrefix = "Reason: %s",
    resolve = "Résoudre", ignore = "Ignorer"
) else AdminStrings(
    manageAccounts = "Gérer les comptes", activeUsers = "Utilisateurs actifs", verifyManage = "Vérifier et gérer", broadcastMessages = "Diffuser des messages", statsKPIs = "Statistiques et KPIs",
    ephemeralContent = "Contenu éphémère", homeBanners = "Bannières accueil", totalControl = "Contrôle total", usersLabel = "Utilisateurs", onlineLabel = "En ligne",
    storiesLabel = "Stories", promoHeroLabel = "Promo Hero", superAdminLabel = "Super Admin", adminConnError = "Erreur de connexion au serveur d'administration.", addUserTitle = "Ajouter un utilisateur",
    role = "Rôle", clientLabel = "Client", adminLabel = "Admin", superLabel = "Super", managedCityOptional = "Ville gérée (optionnel)",
    globalAdminHint = "Laissez vide pour un admin global.", allFieldsRequired = "❌ Tous les champs sont obligatoires", userCreatedSuccess = "✅ Utilisateur %s créé avec succès !", adminErrPrefix = "❌ Erreur: %s", promoCodeRequired = "❌ Code et réduction requis",
    promoAtShop = "🎉 Promotion chez %s", promoReductPct = "Réduction % (ex: 10)", promoFixedFcfa = "Réduction fixe FCFA", promoMinAmountFcfa = "Montant minimum (FCFA)", promoCreatedNotified = "✅ Promotion créée et notifiée à tous !",
    createNotify = "Créer & Notifier", roleChangeError = "Erreur changement rôle: %s", deleteErrorPrefix = "Erreur suppression: %s", notifSent = "✅ Notification envoyée !", sendFailed = "❌ Échec de l'envoi",
    sendErrPrefix = "❌ Échec : %s", sendHistory = "Historique des envois", noHistory = "Aucun historique", andOthers = "Et %d autres...", systemNotif = "Notification système",
    receivedByAll = "Sera reçue par tous les utilisateurs.", individualNotif = "Notification individuelle", sendToSpecific = "Envoyer à un utilisateur spécifique.", searchUserMin = "Rechercher un utilisateur (min 2)", noUserFound = "Aucun utilisateur trouvé",
    change = "Changer", notifBroadcastAll = "✅ Notification diffusée à tous les utilisateurs", broadcastAll = "Diffuser à tous", sendTo = "Envoyer à %s", bannedLabel = "Banni",
    sendNotifMenuItem = "Envoyer notification", roleSuperAdmin = "Rôle Super Admin", roleAdmin = "Rôle Admin", roleVendor = "Rôle Vendeur", roleClient = "Rôle Client",
    reactivate = "Réactiver", ban = "Bannir", unverify = "Dé-vérifier", verifyAction = "Vérifier", removePromo = "Retirer promo",
    featureShop = "Mettre en avant", createPromo = "Créer promotion", deleteShopTitle = "Supprimer la boutique ?", deleteShopConfirm = "Êtes-vous sûr de vouloir supprimer « %s » ?\nTous les produits et commandes liés seront définitivement supprimés.", filterByCity = "Filtrer par ville",
    overview = "Vue d'ensemble", clientsLabel = "Clients", vendorsLabel = "Vendeurs", ordersLabel = "Commandes", totalRevenueLabel = "CA Total",
    todayLabel = "Aujourd'hui", alerts = "Alertes", pendingVerifyShops = "%d boutiques en attente de vérification", goToShopsVerify = "Allez dans l'onglet Boutiques pour les vérifier", bannedShopsLabel = "%d boutiques bannies",
    checkShopsDetails = "Consultez l'onglet Boutiques pour plus de détails", registrations30 = "Inscriptions (30 jours)", newUsersMonth = "+%d nouveaux utilisateurs ce mois", monthlyRevenue12 = "Revenu mensuel (12 mois)", topVendorsCA = "Top vendeurs (CA)",
    orderCountFmt = "%d commandes", topProductsSold = "Top produits vendus", usersByRole = "Utilisateurs par rôle", onlineUsersTitle = "Utilisateurs en ligne", refreshLabel = "Actualiser",
    onlineUsersNow = "%d utilisateurs en ligne actuellement", noOneOnline = "Personne en ligne pour le moment", secondsAgo = "il y a %ds", allStories = "Toutes les stories", noStoryNow = "Aucune story pour le moment",
    deleteStoryError = "Erreur suppression: %s", newStoryAdmin = "Nouvelle Story (Admin)", mediaPickLabel = "Média (Photo ou Vidéo max 30s)", mediaAdjustNote = "Note : L'ajustement est automatique (30s max)", selectMedia = "Sélectionner Média",
    captionOptional = "Légende (Optionnel)", replyCountFmt = "%d réponses", heroSectionMgmt = "Gestion de la Hero Section", heroModifyHint = "Modifiez les bannières promotionnelles de l'accueil.", addPromotion = "Ajouter une promotion",
    heroTitleExample = "Titre (ex: Saveurs locales)", heroSubtitleExample = "Sous-titre (ex: Fruits du terroir)", heroMediaLabel = "Image ou Vidéo (max 10s)", orDirectUrl = "Ou URL directe", shopToPromote = "Boutique à promouvoir (optionnel)",
    addToHome = "Ajouter à l'accueil", activeBanners = "Bannières actives", noCustomBanner = "Aucune bannière personnalisée.", linkPrefix = "Lien: %s", mediaUploadError = "Upload média : %s",
    noShopSelected = "Aucune boutique", noneLabel = "Aucune", create = "Créer", notifyUser = "Notifier : %s", promoNotifBody = "Utilisez le code \"%s\" pour obtenir %s sur les produits %s !",
    superAdminPanel = "Super Admin Panel", systemConfig = "Configuration Système", globalStats = "Statistiques Globales", reportsCount = "Signalements (%d)", globalBroadcast = "Diffusion Globale",
    broadcast = "Diffuser", appVersionLabel = "App Version", minVersionRequired = "Version Min Requise", commissionRate = "Taux Commission", maintenanceMode = "Mode Maintenance",
    activeProducts = "Produits Actifs", globalCA = "CA Global", reportTypeLabel = "Signalement %s", byReporter = "Par: %s", reasonPrefix = "Raison: %s",
    resolve = "Résoudre", ignore = "Ignorer"
)
