package com.tik_market.data.models

object SampleData {
    /** Map of product id → emoji for visual product representation */
    val productEmojis: Map<String, String> = mapOf(
        "p1" to "🐔", "p2" to "🥚", "p3" to "🧣", "p4" to "👗",
        "p5" to "👜", "p6" to "📱", "p7" to "🎧", "p8" to "🦃",
        "p9" to "🔌", "p10" to "👔"
    )
    /** Map of category → emoji */
    val categoryEmojis: Map<String, String> = mapOf(
        "Alimentation" to "🍲", "Mode" to "👕", "Électronique" to "💻",
        "Artisanat" to "🔨", "Beauté" to "💄", "Services" to "🛠️",
        "Agriculture" to "🌿", "Autres" to "📦"
    )
    /** Map of category → gradient colors */
    val categoryGradients: Map<String, Pair<Long, Long>> = mapOf(
        "Alimentation" to (0xFFFF6F00L to 0xFFFFA726L),
        "Mode" to (0xFF7B1FA2L to 0xFFAB47BCL),
        "Électronique" to (0xFF1565C0L to 0xFF42A5F5L),
        "Artisanat" to (0xFF5D4037L to 0xFF8D6E63L),
        "Beauté" to (0xFFE91E63L to 0xFFF06292L),
        "Services" to (0xFF00838FL to 0xFF26C6DAL),
        "Agriculture" to (0xFF2E7D32L to 0xFF66BB6AL),
        "Autres" to (0xFF546E7AL to 0xFF90A4AEL)
    )

    val categories = listOf(
        Category("1", "Alimentation", "restaurant", 0xFFFF6F00),
        Category("2", "Mode", "checkroom", 0xFF7B1FA2),
        Category("3", "Électronique", "devices", 0xFF1565C0),
        Category("4", "Artisanat", "handyman", 0xFF5D4037),
        Category("5", "Beauté", "spa", 0xFFE91E63),
        Category("6", "Services", "support", 0xFF00838F),
        Category("7", "Agriculture", "eco", 0xFF2E7D32),
        Category("8", "Autres", "category", 0xFF546E7A)
    )

    val products = listOf(
        Product("p1", "shop1", "Ferme Avicole TiK", "Quartier Foto", "v1", "+237 690 00 00 01",
            "🐔 Poulet fermier (1 kg)", "Poulet élevé en plein air, nourri au maïs bio local. Chair ferme et savoureuse, idéal pour les grillades et sauces.",
            3500.0, 4000.0, "Alimentation", listOf("https://images.unsplash.com/photo-1587593810167-a84920ea0781?q=80&w=400"), stock = 50, unit = "kg", rating = 4.8f, totalReviews = 24, totalSales = 120),
        Product("p2", "shop1", "Ferme Avicole TiK", "Quartier Foto", "v1", "+237 690 00 00 01",
            "🥚 Œufs fermiers (12)", "Œufs frais de poules élevées en liberté. Riche en oméga-3, jaune bien orange.",
            2500.0, null, "Alimentation", listOf("https://images.unsplash.com/photo-1582722872445-41ea51159092?q=80&w=400"), stock = 100, unit = "boîte", rating = 4.6f, totalReviews = 18, totalSales = 200),
        Product("p3", "shop2", "Tissus & Mode TiK", "Marché A", "v2", "+237 690 00 00 02",
            "🧣 Pagne Wax 6 yards", "Tissu wax 100% coton, motifs authentiques camerounais. Idéal pour vos tenues de cérémonie.",
            8500.0, 10000.0, "Mode", listOf("https://images.unsplash.com/photo-1523381210434-271e8be1f52b?q=80&w=400"), stock = 30, unit = "pièce", rating = 4.9f, totalReviews = 42, totalSales = 85),
        Product("p4", "shop2", "Tissus & Mode TiK", "Marché A", "v2", "+237 690 00 00 02",
            "👗 Robe cérémonie femme", "Robe élégante en pagne wax sur mesure. Choix de motifs et couleurs disponibles.",
            35000.0, 42000.0, "Mode", listOf("https://images.unsplash.com/photo-1539008835657-9e8e6293e232?q=80&w=400"), stock = 10, unit = "pièce", rating = 4.7f, totalReviews = 12, totalSales = 35),
        Product("p5", "shop2", "Tissus & Mode TiK", "Marché A", "v2", "+237 690 00 00 02",
            "👜 Sac à main pagne", "Sac à main artisanal doublé en pagne wax. Pratique et élégant pour le quotidien.",
            12000.0, null, "Mode", listOf("https://images.unsplash.com/photo-1544816153-12ad5d7133a2?q=80&w=400"), stock = 20, unit = "pièce", rating = 4.5f, totalReviews = 8, totalSales = 40),
        Product("p6", "shop3", "Electro-TiK", "Centre-ville", "v3", "+237 690 00 00 03",
            "📱 Samsung Galaxy A25 5G", "Smartphone Samsung Galaxy A25 5G, 8GB RAM, 128GB, double SIM, écran 120Hz.",
            185000.0, 210000.0, "Électronique", listOf("https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?q=80&w=400"), stock = 8, unit = "pièce", rating = 4.6f, totalReviews = 15, totalSales = 28),
        Product("p7", "shop3", "Electro-TiK", "Centre-ville", "v3", "+237 690 00 00 03",
            "🎧 Écouteurs BT Pro", "Écouteurs Bluetooth avec réduction de bruit active, autonomie 30h, étui de charge.",
            35000.0, 45000.0, "Électronique", listOf("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=400"), stock = 15, unit = "pièce", rating = 4.3f, totalReviews = 9, totalSales = 52),
        Product("p8", "shop1", "Ferme Avicole TiK", "Quartier Foto", "v1", "+237 690 00 00 01",
            "🦃 Pintade fermière", "Pintade élevée en plein air, idéale pour les grandes occasions et fêtes.",
            6500.0, 7500.0, "Alimentation", listOf("https://images.unsplash.com/photo-1594142510255-a4968875560b?q=80&w=400"), stock = 20, unit = "pièce", rating = 4.9f, totalReviews = 6, totalSales = 45),
        Product("p9", "shop3", "Electro-TiK", "Centre-ville", "v3", "+237 690 00 00 03",
            "🔌 Chargeur rapide 25W", "Chargeur rapide USB-C 25W Super Fast Charging, compatible Samsung et Android.",
            8500.0, null, "Électronique", listOf("https://images.unsplash.com/photo-1610492421919-866485ecad27?q=80&w=400"), stock = 25, unit = "pièce", rating = 4.2f, totalReviews = 11, totalSales = 67),
        Product("p10", "shop2", "Tissus & Mode TiK", "Marché A", "v2", "+237 690 00 00 02",
            "👔 Ensemble homme traditionnel", "Ensemble chemise + pantalon en pagne traditionnel, fait main par nos artisans.",
            25000.0, null, "Mode", listOf("https://images.unsplash.com/photo-1589410137257-272e50953a81?q=80&w=400"), stock = 15, unit = "ensemble", rating = 4.8f, totalReviews = 20, totalSales = 30),
    )

    val shops = listOf(
        Shop("shop1", "v1", "Ferme Avicole TiK", "Produits avicoles de qualité depuis 2015. Élevage en plein air.", "", "+237 690 00 00 01", "Quartier Foto", "Alimentation", 4.8f, 3, true),
        Shop("shop2", "v2", "Tissus & Mode TiK", "Vente de tissus traditionnels et confection sur mesure. Le choix pour vos cérémonies.", "", "+237 690 00 00 02", "Marché A", "Mode", 4.7f, 4, true),
        Shop("shop3", "v3", "Electro-TiK", "Appareils électroniques neufs sous garantie. Livraison rapide.", "", "+237 690 00 00 03", "Centre-ville", "Électronique", 4.5f, 3, true)
    )

    val orders = listOf(
        Order("o1", "CMD-001", listOf(CartItem(products[0], 2)), 7000.0, OrderStatus.DELIVERED, "Mobile Money", "Dschang, Foto", "+237 690 00 00 01", "12 Jan 2026"),
        Order("o2", "CMD-002", listOf(CartItem(products[2], 1), CartItem(products[4], 1)), 20500.0, OrderStatus.PREPARING, "Mobile Money", "Dschang, Centre-ville", "+237 690 00 00 01", "15 Juin 2026"),
        Order("o3", "CMD-003", listOf(CartItem(products[5], 1)), 185000.0, OrderStatus.CONFIRMED, "Carte Bancaire", "Dschang, Quartier Latin", "+237 690 00 00 02", "18 Juin 2026"),
        Order("o4", "CMD-004", listOf(CartItem(products[7], 1)), 6500.0, OrderStatus.DELIVERING, "Cash", "Dschang, Marché B", "+237 690 00 00 03", "20 Juin 2026"),
        Order("o5", "CMD-005", listOf(CartItem(products[1], 3)), 7500.0, OrderStatus.CANCELLED, "Mobile Money", "Dschang, Foto", "+237 690 00 00 01", "21 Juin 2026")
    )

    val reviews = listOf(
        Review("r1", "u1", "Jean K.", 5, "Poulet très frais, livraison rapide !", "10 Juin 2026"),
        Review("r2", "u2", "Marie N.", 4, "Très bonne qualité, je recommande", "8 Juin 2026"),
        Review("r3", "u1", "Jean K.", 5, "Pagne magnifique, couleurs éclatantes", "5 Juin 2026")
    )

    val messages = listOf(
        Message("m1", "u1", "v1", "Jean K.", "p1", "Poulet fermier (1 kg)", "Bonjour, ce poulet est-il disponible aujourd'hui ?", 1000L, true),
        Message("m2", "v1", "u1", "Ferme Avicole", "p1", "Poulet fermier (1 kg)", "Oui, nous avons des poulets frais disponibles. Livraison dans la journée.", 2000L, true),
        Message("m3", "u1", "v1", "Jean K.", "p1", "Poulet fermier (1 kg)", "Parfait, je prends 2 kg. Merci !", 3000L, false)
    )
}
