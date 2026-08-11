-- ============================================================
-- Dschang Market Place — Schéma MySQL
-- ============================================================

CREATE DATABASE IF NOT EXISTS dschang_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dschang_market;

-- ============================================================
-- UTILISATEURS
-- ============================================================
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(20) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('buyer','vendor','admin') NOT NULL DEFAULT 'buyer',
    location    VARCHAR(200) DEFAULT '',
    avatar      VARCHAR(500) DEFAULT '',
    last_seen   DATETIME DEFAULT NULL,
    status      ENUM('active','banned','suspended') DEFAULT 'active',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- BOUTIQUES
-- ============================================================
CREATE TABLE shops (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    vendor_id       INT NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    logo            VARCHAR(500) DEFAULT '',
    phone           VARCHAR(20) NOT NULL,
    location        VARCHAR(200) NOT NULL,
    category        VARCHAR(100) DEFAULT '',
    is_verified     TINYINT(1) DEFAULT 0,
    status          ENUM('active','banned','suspended') DEFAULT 'active',
    is_featured     TINYINT(1) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- CATÉGORIES
-- ============================================================
CREATE TABLE categories (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE,
    icon    VARCHAR(50) DEFAULT '',
    color   VARCHAR(7) DEFAULT '#2E7D32'
);
INSERT INTO categories (name, icon, color) VALUES
('Alimentation', 'restaurant', '#FF6F00'),
('Mode', 'checkroom', '#7B1FA2'),
('Électronique', 'devices', '#1565C0'),
('Artisanat', 'handyman', '#5D4037'),
('Beauté', 'spa', '#E91E63'),
('Services', 'support', '#00838F'),
('Agriculture', 'eco', '#2E7D32'),
('Autres', 'category', '#546E7A');

-- ============================================================
-- PRODUITS
-- ============================================================
CREATE TABLE products (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    shop_id         INT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    price           DECIMAL(12,0) NOT NULL,
    compare_price   DECIMAL(12,0) DEFAULT NULL,
    category        VARCHAR(100) DEFAULT '',
    image_url       TEXT DEFAULT '',
    stock           INT DEFAULT 0,
    unit            VARCHAR(30) DEFAULT 'pièce',
    rating          FLOAT DEFAULT 0,
    total_reviews   INT DEFAULT 0,
    total_sales     INT DEFAULT 0,
    is_story        TINYINT(1) DEFAULT 0,
    is_active       TINYINT(1) DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    INDEX idx_category (category),
    INDEX idx_shop (shop_id),
    INDEX idx_active (is_active),
    INDEX idx_is_story (is_story)
);

-- ============================================================
-- PANIER
-- ============================================================
CREATE TABLE cart_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    product_id  INT NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product (user_id, product_id)
);

-- ============================================================
-- COMMANDES
-- ============================================================
CREATE TABLE orders (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    order_number      VARCHAR(20) NOT NULL UNIQUE,
    total_amount      DECIMAL(12,0) NOT NULL,
    status            ENUM('pending','confirmed','preparing','delivering','delivered','cancelled') NOT NULL DEFAULT 'pending',
    payment_method    VARCHAR(50) DEFAULT 'Mobile Money',
    payment_status    ENUM('unpaid','paid','refunded') DEFAULT 'unpaid',
    payment_type      ENUM('delivery','direct') DEFAULT 'delivery',
    phone             VARCHAR(20) NOT NULL,
    shipping_address  VARCHAR(300) NOT NULL,
    notes             TEXT,
    vendor_confirmed  TINYINT(1) DEFAULT 0,
    client_confirmed  TINYINT(1) DEFAULT 0,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
);

CREATE TABLE order_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    order_id    INT NOT NULL,
    product_id  INT NOT NULL,
    quantity    INT NOT NULL,
    price       DECIMAL(12,0) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- ============================================================
-- MESSAGES (Chat vendeur-acheteur)
-- ============================================================
CREATE TABLE messages (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    sender_id       INT NOT NULL,
    receiver_id     INT NOT NULL,
    product_id      INT DEFAULT NULL,
    text            TEXT NOT NULL,
    is_read         TINYINT(1) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation (sender_id, receiver_id)
);

-- ============================================================
-- AVIS
-- ============================================================
CREATE TABLE reviews (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    product_id  INT NOT NULL,
    rating      TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product_review (user_id, product_id)
);

-- ============================================================
-- WISHLIST
-- ============================================================
CREATE TABLE wishlist (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    product_id  INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_product_wish (user_id, product_id)
);

-- ============================================================
-- FAVORIS BOUTIQUES (Abonnements boutique)
-- ============================================================
CREATE TABLE shop_favorites (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    shop_id     INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_shop (user_id, shop_id)
);

-- ============================================================
-- PAIEMENTS Mobile Money
-- ============================================================
CREATE TABLE payments (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT NOT NULL,
    user_id         INT NOT NULL,
    amount          DECIMAL(12,0) NOT NULL,
    provider        ENUM('orange','mtn','other') NOT NULL,
    phone           VARCHAR(20) NOT NULL,
    transaction_id  VARCHAR(100) DEFAULT NULL,
    status          ENUM('pending','success','failed') DEFAULT 'pending',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- CODES OTP (Authentification par téléphone)
-- ============================================================
CREATE TABLE otp_codes (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    phone       VARCHAR(20) NOT NULL,
    code        VARCHAR(6) NOT NULL,
    used        TINYINT(1) DEFAULT 0,
    expires_at  DATETIME NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_code (phone, code),
    INDEX idx_expires (expires_at)
);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT DEFAULT NULL, -- NULL pour les notifications système globales
    title       VARCHAR(200) NOT NULL,
    message     TEXT NOT NULL,
    type        ENUM('product', 'system', 'order', 'promo', 'message') NOT NULL DEFAULT 'system',
    related_id  INT DEFAULT NULL, -- ID du produit, commande, etc.
    is_read     TINYINT(1) DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- ACHATS GROUPÉS
-- ============================================================
CREATE TABLE group_buys (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      INT NOT NULL,
    shop_id         INT NOT NULL,
    creator_id      INT NOT NULL,
    min_quantity    INT NOT NULL DEFAULT 5,
    max_quantity    INT NOT NULL DEFAULT 100,
    current_qty     INT NOT NULL DEFAULT 1,
    target_price    DECIMAL(10,0) NOT NULL DEFAULT 0,
    discount_pct    DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    status          ENUM('open','filled','completed','cancelled') NOT NULL DEFAULT 'open',
    expires_at      DATETIME DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE group_buy_participants (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    group_buy_id    INT NOT NULL,
    user_id         INT NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    joined_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_buy_id) REFERENCES group_buys(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_gb_user (group_buy_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- PORTEFEUILLE & PROGRAMME FIDÉLITÉ
-- ============================================================
CREATE TABLE wallets (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL UNIQUE,
    balance         DECIMAL(12,0) NOT NULL DEFAULT 0,
    total_points    INT NOT NULL DEFAULT 0,
    current_points  INT NOT NULL DEFAULT 0,
    tier            ENUM('bronze','argent','or') NOT NULL DEFAULT 'bronze',
    lifetime_spent  DECIMAL(12,0) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE loyalty_tiers (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(20) NOT NULL UNIQUE,
    min_points      INT NOT NULL DEFAULT 0,
    cashback_pct    DECIMAL(5,2) NOT NULL DEFAULT 0,
    bonus_pct       DECIMAL(5,2) NOT NULL DEFAULT 0,
    color           VARCHAR(7) NOT NULL DEFAULT '#2E7D32',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO loyalty_tiers (name, min_points, cashback_pct, bonus_pct, color) VALUES
('bronze', 0, 1.0, 0, '#8D6E63'),
('argent', 500, 2.0, 5, '#9E9E9E'),
('or', 2000, 3.5, 10, '#FFD700');

CREATE TABLE wallet_transactions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id       INT NOT NULL,
    type            ENUM('earn','spend','recharge','cashback','bonus','refund') NOT NULL,
    amount_fcfa     DECIMAL(12,0) NOT NULL DEFAULT 0,
    points          INT NOT NULL DEFAULT 0,
    description     VARCHAR(300) NOT NULL DEFAULT '',
    reference_type  VARCHAR(30) DEFAULT NULL,
    reference_id    INT DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    INDEX idx_wallet (wallet_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupons (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    code            VARCHAR(20) NOT NULL UNIQUE,
    discount_pct    DECIMAL(5,2) DEFAULT NULL,
    discount_fcfa   DECIMAL(12,0) DEFAULT NULL,
    min_amount      DECIMAL(12,0) DEFAULT 0,
    points_cost     INT NOT NULL DEFAULT 0,
    expires_at      DATETIME DEFAULT NULL,
    is_used         TINYINT(1) DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- NOTIFICATIONS PUSH — Tokens + Préférences
-- ============================================================
CREATE TABLE device_tokens (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    token           VARCHAR(500) NOT NULL,
    platform        ENUM('android','web','ios') NOT NULL DEFAULT 'web',
    is_active       TINYINT(1) DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_token (user_id, token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_preferences (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL UNIQUE,
    allow_product   TINYINT(1) DEFAULT 1,
    allow_order     TINYINT(1) DEFAULT 1,
    allow_promo     TINYINT(1) DEFAULT 1,
    allow_message   TINYINT(1) DEFAULT 1,
    allow_system    TINYINT(1) DEFAULT 1,
    push_enabled    TINYINT(1) DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO wallets (user_id) SELECT id FROM users;
INSERT IGNORE INTO notification_preferences (user_id) SELECT id FROM users;
