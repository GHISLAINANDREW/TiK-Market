<?php
/**
 * Seed script: inserts sample categories, shops, products, and reviews.
 * Run: php api/seeder.php
 */
require_once __DIR__ . '/config/database.php';

$db = getDB();

echo "🌱 Seeding database...\n";

// ── Categories ──────────────────────────────────────────────────
$categories = [
    ['Alimentation', '#FF6F00'],
    ['Mode', '#7B1FA2'],
    ['Électronique', '#1565C0'],
    ['Artisanat', '#5D4037'],
    ['Beauté', '#E91E63'],
    ['Services', '#00838F'],
    ['Agriculture', '#2E7D32'],
    ['Autres', '#546E7A'],
];

$stmt = $db->prepare('INSERT IGNORE INTO categories (name, color) VALUES (?, ?)');
foreach ($categories as $cat) {
    $stmt->execute($cat);
}
echo "  ✅ " . count($categories) . " catégories insérées\n";

// ── Shops ──────────────────────────────────────────────────────
// We need vendor users first. Let's check if vendors exist.
$vendorCount = $db->query("SELECT COUNT(*) FROM users WHERE role = 'vendor'")->fetchColumn();

if ($vendorCount == 0) {
    // Create vendor users
    $vendors = [
        ['Mbié Landry', 'landry@example.com', '690000001', 'Ferme Avicole'],
        ['Nadege Tchinda', 'nadege@example.com', '690000002', 'Tissus & Mode'],
        ['Fokou Samuel', 'samuel@example.com', '690000003', 'Electro-Dschang'],
    ];
    $insertUser = $db->prepare('INSERT IGNORE INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)');
    foreach ($vendors as $v) {
        $insertUser->execute([$v[0], $v[1], $v[2], password_hash('password123', PASSWORD_BCRYPT), 'vendor']);
    }
    $vendorCount = 3;
    echo "  ✅ $vendorCount vendeurs créés (mot de passe: password123)\n";
}

// Insert shops
$shopsData = [
    ['Ferme Avicole de Dschang', 'Produits avicoles de qualité depuis 2015. Élevage en plein air.', '690000001', 'Quartier Foto', 'Alimentation'],
    ['Tissus & Mode Dschang', 'Vente de tissus traditionnels et confection sur mesure.', '690000002', 'Marché A', 'Mode'],
    ['Electro-Dschang', 'Appareils électroniques neufs sous garantie. Livraison à Dschang.', '690000003', 'Centre-ville', 'Électronique'],
];

// Get vendor IDs
$vendorIds = $db->query("SELECT id FROM users WHERE role = 'vendor' ORDER BY id LIMIT 3")->fetchAll(PDO::FETCH_COLUMN);

$stmtShop = $db->prepare('INSERT IGNORE INTO shops (vendor_id, name, description, phone, location, category, is_verified) VALUES (?, ?, ?, ?, ?, ?, 1)');
$shopIds = [];
foreach ($shopsData as $i => $shop) {
    $stmtShop->execute([$vendorIds[$i], $shop[0], $shop[1], $shop[2], $shop[3], $shop[4]]);
    $shopIds[] = (int)$db->lastInsertId();
    echo "  ✅ Boutique créée: {$shop[0]}\n";
}

// ── Products ───────────────────────────────────────────────────
$products = [
    [1, '🐔 Poulet fermier (1 kg)', 'Poulet élevé en plein air, nourri au maïs bio local. Chair ferme et savoureuse, idéal pour les grillades et sauces.', 3500, 4000, 'Alimentation', 50, 'kg', 120],
    [1, '🥚 Œufs fermiers (12)', 'Œufs frais de poules élevées en liberté. Riche en oméga-3, jaune bien orange.', 2500, null, 'Alimentation', 100, 'boîte', 200],
    [2, '🧣 Pagne Wax 6 yards', 'Tissu wax 100% coton, motifs authentiques camerounais.', 8500, 10000, 'Mode', 30, 'pièce', 85],
    [2, '👗 Robe cérémonie femme', 'Robe élégante en pagne wax sur mesure.', 35000, 42000, 'Mode', 10, 'pièce', 35],
    [2, '👜 Sac à main pagne', 'Sac à main artisanal doublé en pagne wax.', 12000, null, 'Mode', 20, 'pièce', 40],
    [3, '📱 Samsung Galaxy A25 5G', 'Smartphone Samsung Galaxy A25 5G, 8GB RAM, 128GB, double SIM, écran 120Hz.', 185000, 210000, 'Électronique', 8, 'pièce', 28],
    [3, '🎧 Écouteurs BT Pro', 'Écouteurs Bluetooth avec réduction de bruit active, autonomie 30h.', 35000, 45000, 'Électronique', 15, 'pièce', 52],
    [1, '🦃 Pintade fermière', 'Pintade élevée en plein air, idéale pour les grandes occasions.', 6500, 7500, 'Alimentation', 20, 'pièce', 45],
    [3, '🔌 Chargeur rapide 25W', 'Chargeur rapide USB-C 25W Super Fast Charging.', 8500, null, 'Électronique', 25, 'pièce', 67],
    [2, '👔 Ensemble homme traditionnel', 'Ensemble chemise + pantalon en pagne traditionnel.', 25000, null, 'Mode', 15, 'ensemble', 30],
];

$stmtProd = $db->prepare('INSERT IGNORE INTO products (shop_id, title, description, price, compare_price, category, stock, unit, total_sales) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
$productIds = [];
foreach ($products as $p) {
    $stmtProd->execute([
        $shopIds[$p[0] - 1], $p[1], $p[2], $p[3],
        $p[4], $p[5], $p[6], $p[7], $p[8]
    ]);
    $productIds[] = (int)$db->lastInsertId();
}
echo "  ✅ " . count($products) . " produits insérés\n";

// ── Reviews ────────────────────────────────────────────────────
$buyerIds = $db->query("SELECT id FROM users WHERE role = 'buyer' ORDER BY id LIMIT 2")->fetchAll(PDO::FETCH_COLUMN);
if (count($buyerIds) < 2) {
    // Create a buyer user
    $db->prepare('INSERT IGNORE INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)')
       ->execute(['Jean K.', 'jean@example.com', '690000010', password_hash('password123', PASSWORD_BCRYPT), 'buyer']);
    $buyerIds[] = (int)$db->lastInsertId();
    $db->prepare('INSERT IGNORE INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)')
       ->execute(['Marie N.', 'marie@example.com', '690000011', password_hash('password123', PASSWORD_BCRYPT), 'buyer']);
    $buyerIds[] = (int)$db->lastInsertId();
}

$reviews = [
    [$buyerIds[0], $productIds[0], 5, 'Poulet très frais, livraison rapide !'],
    [$buyerIds[1], $productIds[0], 4, 'Très bonne qualité, je recommande'],
    [$buyerIds[0], $productIds[2], 5, 'Pagne magnifique, couleurs éclatantes'],
];

$stmtRev = $db->prepare('INSERT IGNORE INTO reviews (user_id, product_id, rating, comment) VALUES (?, ?, ?, ?)');
foreach ($reviews as $r) {
    $stmtRev->execute($r);
}
echo "  ✅ " . count($reviews) . " avis insérés\n";

echo "\n✨ Seed terminé avec succès !\n";
echo "   Vendeurs: landry@example.com / nadege@example.com / samuel@example.com\n";
echo "   Mot de passe: password123\n";
echo "   Acheteur: jean@example.com / marie@example.com\n";
