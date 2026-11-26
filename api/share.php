<?php
@error_reporting(0);
@ini_set('display_errors', '0');

$productId = isset($_GET['p']) ? (int)$_GET['p'] : 0;
$title = "TiK-Market — La marketplace camerounaise";
$description = "Achetez et vendez facilement au Cameroun sur TiK-Market.";
$imageUrl = "https://tikmarket-app.vercel.app/favicon.svg";
$appUrl = "https://tikmarket-app.vercel.app/?p=" . $productId;

if ($productId > 0) {
    try {
        @require_once __DIR__ . '/config/database.php';
        if (function_exists('getDbConnection')) {
            $db = @getDbConnection();
            if ($db) {
                $stmt = $db->prepare("SELECT p.*, s.name as shop_name FROM products p LEFT JOIN shops s ON p.shop_id = s.id WHERE p.id = ?");
                $stmt->execute([$productId]);
                $product = $stmt->fetch(PDO::FETCH_ASSOC);

                if ($product) {
                    $priceText = number_format((float)($product['price'] ?? 0), 0, ',', ' ') . ' FCFA';
                    $title = ($product['title'] ?? 'Produit') . " — " . $priceText . " | TiK-Market";

                    if (!empty($product['description'])) {
                        $desc = strip_tags($product['description']);
                        $description = (mb_strlen($desc) > 150) ? mb_substr($desc, 0, 150) . "..." : $desc;
                    } else if (!empty($product['shop_name'])) {
                        $description = "Disponible dans la boutique " . $product['shop_name'] . " sur TiK-Market.";
                    }

                    if (!empty($product['images'])) {
                        $imgs = json_decode($product['images'], true);
                        if (is_array($imgs) && !empty($imgs[0])) {
                            $imageUrl = $imgs[0];
                        }
                    } else if (!empty($product['image_url'])) {
                        $imageUrl = $product['image_url'];
                    }

                    // Convert relative image URLs to absolute URLs
                    if (!empty($imageUrl) && !preg_match('/^https?:\/\//i', $imageUrl)) {
                        $imageUrl = "https://tik-market.onrender.com/" . ltrim($imageUrl, '/');
                    }
                }
            }
        }
    } catch (Throwable $e) {
        // Silently catch all errors, fallback to default meta tags
    }
}

// Detect social media crawlers
$ua = strtolower($_SERVER['HTTP_USER_AGENT'] ?? '');
$isBot = preg_match('/(facebookexternalhit|whatsapp|twitterbot|pinterest|googlebot|bingbot|linkedinbot|slackbot|telegrambot)/i', $ua);

if (!$isBot && !isset($_GET['preview'])) {
    header("Location: " . $appUrl, true, 302);
    exit;
}
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title><?php echo htmlspecialchars($title); ?></title>
    <meta name="description" content="<?php echo htmlspecialchars($description); ?>">

    <!-- Open Graph / Facebook / WhatsApp -->
    <meta property="og:site_name" content="TiK-Market">
    <meta property="og:type" content="product">
    <meta property="og:url" content="<?php echo htmlspecialchars($appUrl); ?>">
    <meta property="og:title" content="<?php echo htmlspecialchars($title); ?>">
    <meta property="og:description" content="<?php echo htmlspecialchars($description); ?>">
    <meta property="og:image" content="<?php echo htmlspecialchars($imageUrl); ?>">
    <meta property="og:image:secure_url" content="<?php echo htmlspecialchars($imageUrl); ?>">
    <meta property="og:image:type" content="image/jpeg">
    <meta property="og:image:width" content="800">
    <meta property="og:image:height" content="800">

    <!-- Twitter -->
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:url" content="<?php echo htmlspecialchars($appUrl); ?>">
    <meta name="twitter:title" content="<?php echo htmlspecialchars($title); ?>">
    <meta name="twitter:description" content="<?php echo htmlspecialchars($description); ?>">
    <meta name="twitter:image" content="<?php echo htmlspecialchars($imageUrl); ?>">

    <script>
        window.location.href = "<?php echo $appUrl; ?>";
    </script>
</head>
<body>
    <p>Redirection vers <a href="<?php echo $appUrl; ?>">TiK-Market</a>...</p>
</body>
</html>
