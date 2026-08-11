-- ============================================================
-- TiK-Market — Base de donnees (clone de dschang_market)
-- Genere le : date('Y-m-d H:i:s')
-- Source : dump local dschang_market + tables fidelite de database.sql
-- ============================================================
-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: tik_market
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `tik_market`
--

/*!40000 DROP DATABASE IF EXISTS `tik_market`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `tik_market` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;

USE `tik_market`;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cart_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `cart_items_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `cart_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `icon` varchar(50) DEFAULT '',
  `color` varchar(7) DEFAULT '#2E7D32',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Alimentation','restaurant','#FF6F00'),(3,'├ëlectronique','devices','#1565C0'),(4,'Artisanat','handyman','#5D4037'),(5,'Beaut├®','spa','#E91E63'),(6,'Services','support','#00838F'),(7,'Agriculture','eco','#2E7D32'),(8,'Autres','category','#546E7A'),(10,'Mode','','#7B1FA2'),(11,'Électronique','','#1565C0'),(13,'Beauté','','#E91E63');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_buy_participants`
--

DROP TABLE IF EXISTS `group_buy_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group_buy_participants` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_buy_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 1,
  `joined_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gb_user` (`group_buy_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `group_buy_participants_ibfk_1` FOREIGN KEY (`group_buy_id`) REFERENCES `group_buys` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_buy_participants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_buy_participants`
--

LOCK TABLES `group_buy_participants` WRITE;
/*!40000 ALTER TABLE `group_buy_participants` DISABLE KEYS */;
INSERT INTO `group_buy_participants` VALUES (1,1,1,1,'2026-07-01 23:30:47'),(2,2,1,1,'2026-07-01 23:31:55'),(22,15,8,1,'2026-07-05 11:54:36'),(23,15,6,1,'2026-07-05 12:08:13'),(24,15,19,1,'2026-07-05 12:10:00'),(25,15,10,1,'2026-07-05 12:10:25'),(26,15,7,1,'2026-07-05 12:11:31');
/*!40000 ALTER TABLE `group_buy_participants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_buys`
--

DROP TABLE IF EXISTS `group_buys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group_buys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `product_id` int(11) NOT NULL,
  `shop_id` int(11) NOT NULL,
  `creator_id` int(11) NOT NULL,
  `min_quantity` int(11) NOT NULL DEFAULT 5,
  `max_quantity` int(11) NOT NULL DEFAULT 100,
  `current_qty` int(11) NOT NULL DEFAULT 1,
  `target_price` decimal(10,0) NOT NULL DEFAULT 0,
  `discount_pct` decimal(5,2) NOT NULL DEFAULT 0.00,
  `status` enum('open','filled','completed','cancelled') NOT NULL DEFAULT 'open',
  `expires_at` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `product_id` (`product_id`),
  KEY `shop_id` (`shop_id`),
  KEY `creator_id` (`creator_id`),
  CONSTRAINT `group_buys_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_buys_ibfk_2` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_buys_ibfk_3` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_buys`
--

LOCK TABLES `group_buys` WRITE;
/*!40000 ALTER TABLE `group_buys` DISABLE KEYS */;
INSERT INTO `group_buys` VALUES (1,14,14,1,5,100,1,18000,10.00,'open','2026-07-04 01:30:47','2026-07-01 23:30:47','2026-07-01 23:30:47'),(2,19,14,1,5,100,1,13500,10.00,'open','2026-07-04 01:31:55','2026-07-01 23:31:55','2026-07-01 23:31:55'),(15,36,15,8,5,100,5,45000,10.00,'filled','2026-07-07 13:54:36','2026-07-05 11:54:36','2026-07-05 12:11:31');
/*!40000 ALTER TABLE `group_buys` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `product_id` int(11) DEFAULT NULL,
  `text` text NOT NULL,
  `audio_url` text DEFAULT NULL,
  `duration` int(11) DEFAULT 0,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `receiver_id` (`receiver_id`),
  KEY `idx_conversation` (`sender_id`,`receiver_id`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=221 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` VALUES (1,6,3,NULL,'jnjn',NULL,0,0,'2026-06-16 22:39:45'),(2,6,8,NULL,'Bonjour, ce produit est-il disponible ?',NULL,0,1,'2026-06-16 22:40:26'),(3,6,8,NULL,'bonj',NULL,0,1,'2026-06-16 22:44:20'),(4,8,3,NULL,'Bonjour, ce produit est-il disponible ?',NULL,0,0,'2026-06-16 22:45:30'),(5,6,7,NULL,'Quel est le délai de livraison ?',NULL,0,1,'2026-06-16 22:47:01'),(6,6,7,NULL,'bonjour',NULL,0,1,'2026-06-16 23:50:53'),(7,6,8,NULL,'sil vous plait',NULL,0,1,'2026-06-16 23:51:11'),(8,8,6,NULL,'bonjour oui cest dispo',NULL,0,1,'2026-06-16 23:53:31'),(9,6,8,NULL,'waouh cest super',NULL,0,1,'2026-06-16 23:53:50'),(10,7,6,NULL,'desole',NULL,0,1,'2026-06-16 23:55:33'),(11,7,6,NULL,'02 jours au max',NULL,0,1,'2026-06-16 23:56:16'),(12,6,7,NULL,'daccord sa marche',NULL,0,1,'2026-06-17 00:07:51'),(13,8,6,NULL,'merci',NULL,0,1,'2026-06-17 00:08:03'),(14,8,3,NULL,'non non desole',NULL,0,0,'2026-06-17 00:08:23'),(15,8,6,NULL,'moi',NULL,0,1,'2026-06-17 00:54:54'),(16,6,8,NULL,'dacc',NULL,0,1,'2026-06-17 00:57:49'),(17,8,6,NULL,'bonjour',NULL,0,1,'2026-06-17 01:16:52'),(18,6,8,NULL,'merci',NULL,0,1,'2026-06-17 01:17:14'),(19,6,8,NULL,'comment',NULL,0,1,'2026-06-17 01:17:26'),(20,8,6,NULL,'je vais vl',NULL,0,1,'2026-06-17 01:17:38'),(21,6,8,NULL,'je suis la',NULL,0,1,'2026-06-17 01:18:02'),(22,8,6,NULL,'okay',NULL,0,1,'2026-06-17 01:19:38'),(23,8,6,NULL,'kdkw',NULL,0,1,'2026-06-17 01:19:52'),(24,8,6,NULL,'wnwnw',NULL,0,1,'2026-06-17 01:19:57'),(25,8,6,NULL,'zzlkzkz',NULL,0,1,'2026-06-17 01:20:00'),(26,8,6,NULL,'ksksk',NULL,0,1,'2026-06-17 01:20:03'),(27,8,6,NULL,'sksks',NULL,0,1,'2026-06-17 01:20:05'),(28,8,6,NULL,'ksksk',NULL,0,1,'2026-06-17 01:20:07'),(29,8,6,NULL,'kskk',NULL,0,1,'2026-06-17 01:20:09'),(30,6,8,NULL,'hg',NULL,0,1,'2026-06-17 01:20:32'),(31,6,8,NULL,'je sort l pagne////??',NULL,0,1,'2026-06-17 01:21:11'),(32,6,8,NULL,'hg',NULL,0,1,'2026-06-17 01:23:05'),(33,8,6,NULL,'c44',NULL,0,1,'2026-06-17 01:23:32'),(34,6,8,NULL,'gf',NULL,0,1,'2026-06-17 01:23:43'),(35,8,6,NULL,'r',NULL,0,1,'2026-06-17 01:23:49'),(36,8,6,NULL,'f4',NULL,0,1,'2026-06-17 01:23:57'),(37,8,6,NULL,'6h',NULL,0,1,'2026-06-17 01:24:01'),(38,6,8,NULL,'ff',NULL,0,1,'2026-06-17 01:24:10'),(39,6,8,NULL,'gdg',NULL,0,1,'2026-06-17 01:24:26'),(40,8,3,NULL,'je ss fâché',NULL,0,0,'2026-06-17 01:27:42'),(41,8,3,NULL,'hhe',NULL,0,0,'2026-06-17 01:38:22'),(42,10,8,NULL,'bonjour',NULL,0,1,'2026-06-17 01:41:11'),(43,6,3,NULL,'bien\nbv',NULL,0,0,'2026-06-17 02:43:33'),(44,6,8,NULL,'tedghjf',NULL,0,1,'2026-06-17 02:43:49'),(45,6,8,NULL,'bonjour',NULL,0,1,'2026-06-17 04:30:35'),(46,8,6,NULL,'ghj',NULL,0,1,'2026-06-17 04:30:51'),(47,6,8,NULL,'je sui fou',NULL,0,1,'2026-06-17 04:31:08'),(48,8,6,NULL,'et on fait comment',NULL,0,1,'2026-06-17 04:31:22'),(49,6,8,NULL,'on sort le pagne',NULL,0,1,'2026-06-17 04:31:45'),(50,6,8,NULL,'yo',NULL,0,1,'2026-06-17 05:23:51'),(51,8,6,NULL,'way',NULL,0,1,'2026-06-17 05:24:21'),(52,6,8,NULL,'tu do quoi',NULL,0,1,'2026-06-17 05:25:01'),(53,6,8,NULL,'je ne dors pas',NULL,0,1,'2026-06-17 05:25:21'),(54,8,6,NULL,'humm',NULL,0,1,'2026-06-17 05:26:05'),(55,6,8,NULL,'toi',NULL,0,1,'2026-06-17 05:26:42'),(56,8,6,NULL,'dcc',NULL,0,1,'2026-06-17 05:26:52'),(57,8,6,NULL,'sd',NULL,0,1,'2026-06-17 05:27:03'),(58,10,8,NULL,'bonjour',NULL,0,1,'2026-06-17 06:51:49'),(59,8,10,NULL,'yo',NULL,0,1,'2026-06-17 06:52:28'),(60,10,8,NULL,'bien',NULL,0,1,'2026-06-17 06:53:08'),(61,8,10,NULL,'okay',NULL,0,1,'2026-06-17 06:53:42'),(62,10,8,NULL,'merci',NULL,0,1,'2026-06-17 06:53:56'),(63,10,8,NULL,'dfd',NULL,0,1,'2026-06-17 07:03:43'),(64,8,10,NULL,'gg',NULL,0,1,'2026-06-17 07:03:58'),(65,6,8,NULL,'gh',NULL,0,1,'2026-06-17 07:52:47'),(66,6,8,NULL,'bonjour je suis intéressé',NULL,0,1,'2026-06-17 18:57:57'),(67,15,8,NULL,'Bonsoir,le vêtement me plaît',NULL,0,1,'2026-06-17 20:09:55'),(68,8,15,NULL,'et on sort le pagne..??',NULL,0,1,'2026-06-17 20:10:19'),(69,15,8,NULL,'Maintenant tu es satisfait crabe ?',NULL,0,1,'2026-06-17 20:10:56'),(70,15,8,NULL,'Tu ne vends pas les femmes ici crabe?',NULL,0,1,'2026-06-17 20:11:50'),(71,8,15,NULL,'je suis désolé monsieur vous parlez de quel vêtement',NULL,0,0,'2026-06-17 20:12:06'),(72,15,8,NULL,'😒',NULL,0,1,'2026-06-17 20:12:07'),(73,8,15,NULL,'yep',NULL,0,0,'2026-06-17 20:13:19'),(74,16,8,NULL,'Bonjour monsieur , l\'article est encore dispo',NULL,0,1,'2026-06-17 20:20:04'),(75,8,16,NULL,'merci bonjour',NULL,0,1,'2026-06-17 20:20:43'),(81,8,16,NULL,'comment vas ton',NULL,0,1,'2026-06-17 20:44:17'),(82,10,8,NULL,'je suis remontée contre vous monsieur',NULL,0,1,'2026-06-17 21:09:01'),(83,6,3,NULL,'merci bcp',NULL,0,0,'2026-06-18 22:02:25'),(85,6,8,NULL,'yoo',NULL,0,1,'2026-06-19 00:13:45'),(86,8,6,NULL,'oui',NULL,0,1,'2026-06-19 00:14:11'),(87,8,6,NULL,'comment tu vas',NULL,0,1,'2026-06-19 00:14:27'),(88,8,6,NULL,'je suis la',NULL,0,1,'2026-06-19 00:14:35'),(89,8,6,NULL,'d\'accord',NULL,0,1,'2026-06-19 00:14:44'),(90,6,8,NULL,'hey',NULL,0,1,'2026-06-19 00:15:02'),(91,6,8,NULL,'hfg',NULL,0,1,'2026-06-19 00:15:08'),(92,6,8,NULL,'je taddant',NULL,0,1,'2026-06-19 00:15:21'),(93,8,6,NULL,'oui',NULL,0,1,'2026-06-19 00:15:38'),(94,8,6,NULL,'c\'est mort',NULL,0,1,'2026-06-19 00:18:40'),(95,8,6,NULL,'[Vocal]',NULL,0,1,'2026-06-19 00:29:29'),(96,8,6,NULL,'[Vocal]',NULL,0,1,'2026-06-19 00:29:35'),(97,8,6,NULL,'dhgf',NULL,0,1,'2026-06-19 00:49:18'),(98,8,6,NULL,'rty',NULL,0,1,'2026-06-19 00:49:25'),(99,8,6,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 00:49:33'),(100,8,6,NULL,'[Vocal]','',1,1,'2026-06-19 00:55:23'),(101,8,6,NULL,'[Vocal]','',2,1,'2026-06-19 00:55:31'),(102,6,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/voices/voice_1781830626_dac7d039.amr',0,1,'2026-06-19 00:57:06'),(103,6,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/voices/voice_1781830642_c43af1a0.amr',0,1,'2026-06-19 00:57:22'),(104,6,8,NULL,'😂',NULL,0,1,'2026-06-19 00:57:30'),(105,8,6,NULL,'🍎',NULL,0,1,'2026-06-19 00:57:41'),(106,8,6,NULL,'[Image]','http://192.168.1.230:8081/uploads/voices/voice_1781830675_d1d181d9.amr',0,1,'2026-06-19 00:57:55'),(107,6,8,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 01:13:22'),(108,6,8,NULL,'🛫',NULL,0,1,'2026-06-19 01:13:39'),(109,8,6,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781831868_62841974.jpg',0,1,'2026-06-19 01:17:48'),(110,6,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781831978_755e1483.png',0,1,'2026-06-19 01:19:38'),(111,8,6,NULL,'😍😍',NULL,0,1,'2026-06-19 01:19:57'),(112,6,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781832018_bb3eb7e2.jpg',0,1,'2026-06-19 01:20:18'),(113,8,6,NULL,'[Vocal]','',1,1,'2026-06-19 01:39:03'),(114,8,6,NULL,'[Vocal]','',2,1,'2026-06-19 01:39:16'),(115,8,6,NULL,'[Vocal]','',4,1,'2026-06-19 01:39:25'),(116,6,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781867832_f4f17674.jpg',0,1,'2026-06-19 11:17:12'),(117,6,7,NULL,'bonjour',NULL,0,1,'2026-06-19 13:27:04'),(118,6,8,NULL,'bon',NULL,0,1,'2026-06-19 13:27:52'),(119,8,6,NULL,'yu',NULL,0,1,'2026-06-19 13:28:23'),(120,8,6,NULL,'gyuj',NULL,0,1,'2026-06-19 13:28:29'),(121,8,6,NULL,'hk',NULL,0,1,'2026-06-19 13:28:33'),(122,8,6,NULL,'de',NULL,0,1,'2026-06-19 13:57:12'),(123,6,8,NULL,'fvfv',NULL,0,1,'2026-06-19 13:57:41'),(124,6,8,NULL,'je',NULL,0,1,'2026-06-19 13:58:05'),(125,6,8,NULL,'tues ou frerot',NULL,0,1,'2026-06-19 13:58:28'),(126,6,8,NULL,'je ne suis pas dacord mon ferot',NULL,0,1,'2026-06-19 14:01:25'),(127,8,6,NULL,'[Vocal]','',2,1,'2026-06-19 14:01:56'),(128,8,6,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781877728_00c8adb1.jpg',0,1,'2026-06-19 14:02:08'),(129,8,6,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 14:10:44'),(130,8,6,NULL,'[Vocal]','',3,1,'2026-06-19 14:11:23'),(131,8,6,NULL,'[Vocal]','',5,1,'2026-06-19 14:11:39'),(132,6,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781878379_fa0ab2c9.jpg',0,1,'2026-06-19 14:12:59'),(133,6,8,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 14:13:05'),(134,6,8,NULL,'[Fichier]','http://192.168.1.230:8081/uploads/chat_files/msg_1781878404_e0af2f9e.mpeg',0,1,'2026-06-19 14:13:24'),(135,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781878496_6aefb235.bin',2,1,'2026-06-19 14:14:56'),(136,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781878516_a19c2231.bin',7,1,'2026-06-19 14:15:16'),(137,8,6,NULL,'[Vocal]','',4,1,'2026-06-19 14:15:55'),(138,6,8,NULL,'[Photo]','http://192.168.1.230:8081/uploads/chat_files/msg_1781878618_99e4d458.png',0,1,'2026-06-19 14:16:58'),(139,8,6,NULL,'merci',NULL,0,1,'2026-06-19 14:17:19'),(140,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781879894_d03bd996.bin',3,1,'2026-06-19 14:38:14'),(141,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781880056_e3de495d.amr',3,1,'2026-06-19 14:40:56'),(142,8,6,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 14:41:02'),(143,8,6,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781880068_6acd7403.jpg',0,1,'2026-06-19 14:41:08'),(144,8,6,NULL,'❤️',NULL,0,1,'2026-06-19 14:41:19'),(145,8,6,NULL,'[Fichier]','http://192.168.1.230:8081/uploads/chat_files/msg_1781880096_374445df.bin',0,1,'2026-06-19 14:41:36'),(146,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781880168_c2db5872.bin',1,1,'2026-06-19 14:42:48'),(147,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781880177_a236ea54.bin',6,1,'2026-06-19 14:42:57'),(148,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781880210_2da6ac65.amr',4,1,'2026-06-19 14:43:30'),(149,8,6,NULL,'y',NULL,0,1,'2026-06-19 14:43:53'),(150,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781882782_70278cd7.mp4',2,1,'2026-06-19 15:26:22'),(151,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781882816_a2e1a356.mp4',2,1,'2026-06-19 15:26:56'),(152,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781882837_30e6130a.bin',4,1,'2026-06-19 15:27:17'),(153,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781882854_b935b2f8.mp4',3,1,'2026-06-19 15:27:34'),(154,6,8,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 15:28:11'),(155,6,8,NULL,'Ma position : Banengo',NULL,0,1,'2026-06-19 15:28:39'),(156,8,6,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 15:28:48'),(157,8,6,NULL,'Ma position : Dschang',NULL,0,1,'2026-06-19 15:29:04'),(158,8,6,NULL,'📍 Ma position\nhttps://www.google.com/maps?q=5.44406417,10.05987198',NULL,0,1,'2026-06-19 15:53:51'),(159,6,8,NULL,'Ma position : Al Djoufrah',NULL,0,1,'2026-06-19 15:57:05'),(160,8,6,NULL,'📍 Ma position : Dschang',NULL,0,1,'2026-06-19 16:30:14'),(161,8,6,NULL,'[Fichier]','http://192.168.1.230:8081/uploads/chat_files/msg_1781886657_88d854a8.pdf',0,1,'2026-06-19 16:30:57'),(162,6,8,NULL,'Ma position : Al Djoufrah',NULL,0,1,'2026-06-19 16:31:19'),(163,6,8,NULL,'[Fichier]','http://192.168.1.230:8081/uploads/chat_files/msg_1781886695_4ae31b84.docx',0,1,'2026-06-19 16:31:35'),(164,16,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/chat_files/msg_1781888228_32fdf322.mp4',0,1,'2026-06-19 16:57:08'),(165,16,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781888242_47a86855.mp4',3,1,'2026-06-19 16:57:22'),(166,8,16,NULL,'[Photo]','http://192.168.1.230:8081/uploads/chat_files/msg_1781888323_e629ef02.jpeg',0,1,'2026-06-19 16:58:43'),(175,8,16,NULL,'📍 Ma position\nhttps://www.google.com/maps?q=5.44381005,10.0600654',NULL,0,1,'2026-06-19 17:01:24'),(182,8,16,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781888677_1ed6dc91.mp4',3,1,'2026-06-19 17:04:37'),(183,16,8,NULL,'📍 Ma position : Dschang',NULL,0,1,'2026-06-19 17:10:09'),(184,16,8,NULL,'📍 Ma position : Dschang',NULL,0,1,'2026-06-19 17:10:22'),(185,8,16,NULL,'📍 Ma position : Dschang\nhttps://www.google.com/maps?q=5.44404709,10.05996522',NULL,0,0,'2026-06-19 22:16:42'),(186,8,6,NULL,'📍 Ma position : Dschang\nhttps://www.google.com/maps?q=5.44404709,10.05996522',NULL,0,1,'2026-06-19 22:21:52'),(187,6,7,NULL,'📍 Ma position : Dschang',NULL,0,1,'2026-06-19 22:31:22'),(188,6,7,NULL,'📍 Ma position : Dschang',NULL,0,1,'2026-06-19 22:31:31'),(189,6,8,NULL,'📍 Ma position : Dschang',NULL,0,1,'2026-06-19 22:32:07'),(190,6,8,NULL,'[Fichier]','http://192.168.1.230:8081/uploads/chat_files/msg_1781908391_a8cb9b5e.docx',0,1,'2026-06-19 22:33:11'),(191,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781909025_87ae32a8.mp4',3,1,'2026-06-19 22:43:45'),(192,8,10,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781909430_a1f4965e.mp4',8,1,'2026-06-19 22:50:30'),(193,10,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1781909500_16a456dd.mp4',2,1,'2026-06-19 22:51:40'),(194,8,6,NULL,'atsk_5291e7ab125e7a8523f820fdda01ac35ae776cc083efd53c9ae7368498269927a3c50e2c',NULL,0,1,'2026-06-20 01:32:53'),(195,8,6,NULL,'atsk_5291e7ab125e7a8523f820fdda01ac35ae776cc083efd53c9ae7368498269927a3c50e2c',NULL,0,1,'2026-06-20 01:32:54'),(196,8,6,NULL,'📍 Ma position : Dschang\nhttps://www.google.com/maps?q=5.44404709,10.05996522',NULL,0,1,'2026-06-20 08:35:02'),(197,8,6,NULL,'💯',NULL,0,1,'2026-06-20 08:35:10'),(198,7,6,NULL,'📍 Ma position : Dschang\nhttps://www.google.com/maps?q=5.49,10.45',NULL,0,1,'2026-06-20 08:54:06'),(200,10,8,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1781951044_8a52eb22.jpeg',0,1,'2026-06-20 10:24:04'),(201,6,7,NULL,'📍 Ma position : Dschang\nhttps://www.google.com/maps?q=5.44313619,10.06046198',NULL,0,1,'2026-06-29 00:16:24'),(202,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1782724920_bcd7ad18.bin',2,1,'2026-06-29 09:22:00'),(203,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1782724965_29f6476c.mp4',3,1,'2026-06-29 09:22:45'),(204,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1782804416_09aeca1f.bin',2,1,'2026-06-30 07:26:56'),(205,6,8,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1782804430_6bccb026.bin',7,1,'2026-06-30 07:27:10'),(206,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/chat_files/msg_1782804945_9ce55d6f.mp4',0,1,'2026-06-30 07:35:45'),(207,8,6,NULL,'[Vocal]','http://192.168.1.230:8081/uploads/voices/msg_1782804949_c21d1239.mp4',2,1,'2026-06-30 07:35:49'),(208,8,6,NULL,'he',NULL,0,1,'2026-06-30 15:10:46'),(209,8,6,NULL,'📍 Ma position : 5.44399217, 10.05996556\nhttps://www.google.com/maps?q=5.44399217,10.05996556',NULL,0,1,'2026-06-30 15:10:57'),(210,8,12,NULL,'bonjour',NULL,0,0,'2026-06-30 15:17:10'),(211,8,16,NULL,'yo',NULL,0,0,'2026-06-30 23:08:15'),(212,8,16,NULL,'[Fichier]','http://192.168.1.230:8081/uploads/chat_files/msg_1782860915_1f053d0f.mp4',0,0,'2026-06-30 23:08:35'),(213,10,25,NULL,'bonjour c\'est combien',NULL,0,1,'2026-07-01 19:17:01'),(214,25,10,NULL,'10000 l\'unité',NULL,0,1,'2026-07-01 19:17:45'),(215,10,25,NULL,'ekie quand on mange ça on devient que Dieu..??',NULL,0,1,'2026-07-01 19:19:28'),(216,10,25,NULL,'[Image]','http://192.168.1.230:8081/uploads/chat_files/msg_1782933591_52f80bf8.jpeg',0,1,'2026-07-01 19:19:51'),(217,10,25,NULL,'merci pour le cordon',NULL,0,0,'2026-07-01 20:15:13'),(218,19,25,NULL,'bonjour je suis interresse par votre produit',NULL,0,0,'2026-07-01 21:23:54'),(219,19,8,NULL,'bonjour',NULL,0,1,'2026-07-02 00:49:45'),(220,8,10,NULL,'[Photo]','http://192.168.1.230:8081/uploads/chat_files/msg_1782961361_902af275.jpeg',0,0,'2026-07-02 03:02:41');
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `message` text NOT NULL,
  `type` enum('product','system','order','promo','message') NOT NULL DEFAULT 'system',
  `related_id` int(11) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (130,16,'chef','bonjour','system',NULL,0,'2026-07-02 02:56:29'),(131,13,'bonjour','chef sentez vous salué','system',NULL,0,'2026-07-02 02:56:57'),(132,10,'Nouveau message de will de chez moi','[Photo]','message',NULL,1,'2026-07-02 03:02:41'),(139,8,'🎉 Nouveau participant !','Quelqu\'un a rejoint votre achat groupé !','system',15,1,'2026-07-05 12:08:13'),(140,8,'🎉 Nouveau participant !','Quelqu\'un a rejoint votre achat groupé !','system',15,1,'2026-07-05 12:10:00'),(141,8,'🎉 Nouveau participant !','Quelqu\'un a rejoint votre achat groupé !','system',15,0,'2026-07-05 12:10:25'),(142,8,'🎊 Groupe d\'achat complet !','Le groupe pour votre produit \'36\' est maintenant complet. Vous pouvez contacter les participants.','order',15,0,'2026-07-05 12:11:31'),(143,6,'✅ Groupe complet !','Félicitations ! Le groupe est complet. Vous pouvez maintenant contacter le vendeur WILL SHOPPING au 683271563 pour finaliser votre achat au prix réduit.','order',15,0,'2026-07-05 12:11:31'),(144,7,'✅ Groupe complet !','Félicitations ! Le groupe est complet. Vous pouvez maintenant contacter le vendeur WILL SHOPPING au 683271563 pour finaliser votre achat au prix réduit.','order',15,1,'2026-07-05 12:11:31'),(145,10,'✅ Groupe complet !','Félicitations ! Le groupe est complet. Vous pouvez maintenant contacter le vendeur WILL SHOPPING au 683271563 pour finaliser votre achat au prix réduit.','order',15,0,'2026-07-05 12:11:31'),(146,19,'✅ Groupe complet !','Félicitations ! Le groupe est complet. Vous pouvez maintenant contacter le vendeur WILL SHOPPING au 683271563 pour finaliser votre achat au prix réduit.','order',15,0,'2026-07-05 12:11:31');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `price` decimal(12,0) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `order_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `order_number` varchar(20) NOT NULL,
  `total_amount` decimal(12,0) NOT NULL,
  `status` enum('pending','confirmed','preparing','delivering','delivered','cancelled') NOT NULL DEFAULT 'pending',
  `payment_method` varchar(50) DEFAULT 'Mobile Money',
  `payment_status` enum('unpaid','paid','refunded') DEFAULT 'unpaid',
  `payment_type` enum('delivery','direct') DEFAULT 'delivery',
  `phone` varchar(20) NOT NULL,
  `shipping_address` varchar(300) NOT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `vendor_confirmed` tinyint(1) DEFAULT 0,
  `client_confirmed` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_number` (`order_number`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `otp_codes`
--

DROP TABLE IF EXISTS `otp_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `otp_codes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) NOT NULL,
  `code` varchar(6) NOT NULL,
  `used` tinyint(1) DEFAULT 0,
  `expires_at` datetime NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_phone_code` (`phone`,`code`),
  KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `otp_codes`
--

LOCK TABLES `otp_codes` WRITE;
/*!40000 ALTER TABLE `otp_codes` DISABLE KEYS */;
INSERT INTO `otp_codes` VALUES (1,'691526595','075221',0,'2026-06-20 03:47:33','2026-06-20 01:42:34'),(2,'691526595','027173',0,'2026-06-20 04:03:52','2026-06-20 01:58:52'),(3,'691526595','717468',0,'2026-06-20 04:06:51','2026-06-20 02:01:51'),(4,'691526595','779560',0,'2026-06-20 04:07:26','2026-06-20 02:02:26'),(5,'691526595','010481',0,'2026-06-20 04:07:35','2026-06-20 02:02:35'),(6,'691526595','444683',1,'2026-06-20 04:08:23','2026-06-20 02:03:24'),(7,'673271548','564740',0,'2026-06-20 04:11:26','2026-06-20 02:06:27'),(8,'673271548','915897',0,'2026-06-20 04:13:13','2026-06-20 02:08:14'),(9,'673271548','912467',0,'2026-06-20 04:14:30','2026-06-20 02:09:30'),(10,'673271548','972647',0,'2026-06-20 04:15:32','2026-06-20 02:10:33'),(11,'691526595','909484',0,'2026-06-20 04:17:15','2026-06-20 02:12:16'),(12,'691526595','264871',0,'2026-06-20 04:22:40','2026-06-20 02:17:41'),(13,'673271548','317228',0,'2026-06-20 04:24:46','2026-06-20 02:19:46'),(14,'691526595','751707',0,'2026-06-20 04:26:37','2026-06-20 02:21:38'),(15,'691526595','142610',0,'2026-06-20 04:43:07','2026-06-20 02:38:08'),(16,'673271548','716531',0,'2026-06-20 04:46:09','2026-06-20 02:41:10');
/*!40000 ALTER TABLE `otp_codes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `amount` decimal(12,0) NOT NULL,
  `provider` enum('orange','mtn','other') NOT NULL,
  `phone` varchar(20) NOT NULL,
  `transaction_id` varchar(100) DEFAULT NULL,
  `status` enum('pending','success','failed') DEFAULT 'pending',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `payments_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `payments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shop_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `price` decimal(12,0) NOT NULL,
  `compare_price` decimal(12,0) DEFAULT NULL,
  `category` varchar(100) DEFAULT '',
  `image_url` varchar(500) DEFAULT '',
  `stock` int(11) DEFAULT 0,
  `unit` varchar(30) DEFAULT 'pi├¿ce',
  `rating` float DEFAULT 0,
  `total_reviews` int(11) DEFAULT 0,
  `total_sales` int(11) DEFAULT 0,
  `is_story` tinyint(1) DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_shop` (`shop_id`),
  KEY `idx_active` (`is_active`),
  CONSTRAINT `products_ibfk_1` FOREIGN KEY (`shop_id`) REFERENCES `shops` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (14,14,'lapin du congo','tres bonne viande',20000,45000,'Alimentation','http://192.168.1.230:8081/uploads/products/product_1781563847_1e50a182.jpg',40,'pièce',0,0,0,1,1,'2026-06-15 22:50:48','2026-06-30 21:05:24'),(15,14,'ghislain','refv',300,600,'Autres','http://192.168.1.230:8081/uploads/products/product_1781564606_7cc35b07.png',34,'pièce',0,0,0,0,1,'2026-06-15 23:03:27','2026-06-29 20:51:33'),(16,14,'test téléphone','moi je teste tout',100000,20000000,'Beauté','',100,'pièce',0,0,0,0,1,'2026-06-15 23:06:43','2026-06-15 23:06:43'),(17,14,'lait caller','délicieux lait de vache',1000,1500,'Alimentation','',15,'pièce',0,0,0,0,1,'2026-06-15 23:22:12','2026-06-15 23:22:12'),(18,14,'moi','Le grand 🦁 lion',1500,2000,'Artisanat','',21,'pièce',0,0,0,0,1,'2026-06-15 23:38:56','2026-07-20 22:45:41'),(19,14,'lapin dore du sud','tres bonne viande blanche',15000,20000,'Alimentation','http://192.168.1.230:8081/uploads/products/product_1781567036_494cc790.jpg',20,'pièce',0,0,0,0,1,'2026-06-15 23:43:57','2026-06-29 20:51:33'),(20,15,'air Nike 2','très bonne pour effectuer de longues distances et du footing',15000,20000,'Mode','http://192.168.1.230:8081/uploads/products/product_1781567534_d0c8c8e4.jpg,http://192.168.1.230:8081/uploads/products/product_1782952780_d6d0b909.jpg,http://192.168.1.230:8081/uploads/products/product_1782952780_3c448917.jpg,http://192.168.1.230:8081/uploads/products/product_1782952780_8203d1b1.jpg',15,'pièce',0,0,0,1,1,'2026-06-15 23:52:14','2026-07-02 00:39:58'),(21,15,'SPORT Dresses','ideal pour faire du sport',7500,10000,'Mode','http://192.168.1.230:8081/uploads/products/product_1781567910_22c61419.jpg',12,'pièce',0,0,0,0,1,'2026-06-15 23:58:30','2026-06-29 20:51:33'),(22,14,'La presidential chair','chaise idéal pour les réunion d\'affaires',50000,70000,'Autres','http://192.168.1.230:8081/uploads/products/product_1781568231_1c143b07.jpg',200,'pièce',0,0,0,1,1,'2026-06-16 00:03:51','2026-06-30 21:05:15'),(23,15,'chaise royal en or massif','ideal pour les grandes ceremonies',70000,90000,'Autres','http://192.168.1.230:8081/uploads/products/product_1781569643_66ee6264.jpg',50,'pièce',0,0,0,0,1,'2026-06-16 00:27:23','2026-06-29 20:51:33'),(24,15,'chaise','',20000,25000,'Mode','http://192.168.1.230:8081/uploads/products/product_1781572994_5a353eff.jpg',1000,'pièce',0,0,0,0,1,'2026-06-16 01:23:14','2026-06-29 20:51:33'),(25,15,'robe moulant 1','très utiles',5000,6500,'Mode','http://192.168.1.230:8081/uploads/products/product_1781573787_84d6ce94.jpg',15,'pièce',0,0,0,0,1,'2026-06-16 01:36:27','2026-06-29 20:51:33'),(26,16,'ensemble swag','hyper résistant',5000,8000,'Beauté','http://192.168.1.230:8081/uploads/products/product_1781731367_03657716.jpg',12,'pièce',0,0,0,0,1,'2026-06-17 21:22:47','2026-06-29 20:51:33'),(27,16,'Poulet de l\'inde traditionnel','très délicieux et peut nourrir environ 200 personnes',300000,500000,'Alimentation','http://192.168.1.230:8081/uploads/products/product_1781732881_b146b322.jpg',100,'pièce',0,0,0,0,1,'2026-06-17 21:48:01','2026-06-29 20:51:33'),(28,16,'iPhone','très bon téléphone iPhone a moindre coup',500000,650000,'Électronique','http://192.168.1.230:8081/uploads/products/product_1781733386_36ae529e.jpg',50,'pièce',0,0,0,0,1,'2026-06-17 21:56:26','2026-06-29 20:51:33'),(29,14,'dome','',1000000,10005000,'','http://192.168.1.230:8081/uploads/products/product_1781824123_d1d2b4df.jpg',0,'pièce',0,0,0,0,1,'2026-06-18 23:08:43','2026-06-29 20:51:33'),(30,15,'big gum sweet','meilleurs',100,300,'Alimentation','http://192.168.1.230:8081/uploads/products/product_1781875333_f241d491.jpg',2000,'pièce',0,0,0,0,1,'2026-06-19 13:22:13','2026-07-20 22:45:41'),(31,15,'garçon','homme',100000,10000,'Mode','http://192.168.1.230:8081/uploads/products/product_1781910402_754fb05e.jpg',1,'pièce',0,0,0,0,1,'2026-06-19 13:30:31','2026-06-29 20:51:33'),(32,15,'produit bio','199 pour 100 naturel faites à base de salade russe',100000,150000,'Beauté','http://192.168.1.230:8081/uploads/products/product_1781910527_6a6f70f2.jpg',2,'pièce',0,0,0,1,1,'2026-06-19 23:08:50','2026-06-30 21:51:59'),(33,15,'pack cuisine','',25000,35000,'Électronique','http://192.168.1.230:8081/uploads/products/product_1782908441_b333f0b4.jpg,http://192.168.1.230:8081/uploads/products/product_1782952502_6ed6eead.jpg',12,'pièce',0,0,0,0,1,'2026-06-30 15:21:20','2026-07-02 00:35:02'),(34,14,'rien','très utile',8000,9500,'Mode','http://192.168.1.230:8081/uploads/products/product_1782905074_bd87114e.jpg,http://192.168.1.230:8081/uploads/products/product_1782905074_4e5286a1.jpg,http://192.168.1.230:8081/uploads/products/product_1782905074_12363d87.jpg,http://192.168.1.230:8081/uploads/products/product_1782940070_427b864a.jpg,http://192.168.1.230:8081/uploads/products/product_1782940070_f5dc4624.jpg',50,'pièce',0,0,0,1,1,'2026-07-01 11:24:34','2026-07-01 21:07:51'),(35,17,'bananes asiatique','bananes asiatique hyper géant',10000,15000,'Agriculture','http://192.168.1.230:8081/uploads/products/product_1782933311_29f636af.jpg',10,'pièce',0,0,0,1,1,'2026-07-01 19:15:11','2026-07-01 19:15:11'),(36,15,'iPhone 15','très rapide avec une forte capacité de batterie',50000,80000,'Électronique','http://192.168.1.230:8081/uploads/products/product_1782954075_86e47add.jpg,http://192.168.1.230:8081/uploads/products/product_1782954075_969a6cb9.jpg,http://192.168.1.230:8081/uploads/products/product_1782954076_78763b0b.jpg,http://192.168.1.230:8081/uploads/products/product_1782954076_b3d29f52.jpg',12,'pièce',0,0,0,1,1,'2026-07-02 01:01:16','2026-07-02 01:01:16');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `promotions`
--

DROP TABLE IF EXISTS `promotions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `promotions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shop_id` int(11) NOT NULL,
  `code` varchar(50) NOT NULL,
  `discount_pct` decimal(5,2) NOT NULL DEFAULT 0.00,
  `discount_fixed` int(11) NOT NULL DEFAULT 0,
  `min_amount` int(11) NOT NULL DEFAULT 0,
  `max_uses` int(11) NOT NULL DEFAULT 0,
  `used_count` int(11) NOT NULL DEFAULT 0,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `expires_at` date DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_code` (`shop_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `promotions`
--

LOCK TABLES `promotions` WRITE;
/*!40000 ALTER TABLE `promotions` DISABLE KEYS */;
/*!40000 ALTER TABLE `promotions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `reporter_id` int(11) NOT NULL,
  `type` varchar(20) NOT NULL COMMENT 'product / message / user',
  `target_id` int(11) NOT NULL,
  `reason` varchar(100) NOT NULL,
  `comment` text DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reports`
--

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
/*!40000 ALTER TABLE `reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reviews` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `rating` tinyint(4) NOT NULL CHECK (`rating` between 1 and 5),
  `comment` text DEFAULT NULL,
  `image_url` text DEFAULT NULL,
  `useful_votes` int(11) DEFAULT 0,
  `vendor_reply` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product_review` (`user_id`,`product_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `reviews_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (4,10,31,5,'waouh',NULL,0,NULL,'2026-06-20 08:26:22'),(5,10,25,4,'super',NULL,0,NULL,'2026-06-20 08:27:10'),(6,6,31,4,'',NULL,0,NULL,'2026-06-20 08:38:18'),(7,6,24,5,'',NULL,0,NULL,'2026-06-20 08:39:05'),(8,6,20,4,'',NULL,0,NULL,'2026-06-20 08:40:52'),(9,7,23,4,'',NULL,0,NULL,'2026-06-20 08:53:08'),(10,6,32,4,'bien',NULL,0,NULL,'2026-06-30 15:33:45'),(11,19,33,4,'super',NULL,0,NULL,'2026-06-30 21:49:57'),(12,19,32,2,'',NULL,0,NULL,'2026-06-30 23:02:44'),(13,6,28,4,'',NULL,0,NULL,'2026-07-01 08:09:00'),(14,10,35,5,'',NULL,0,NULL,'2026-07-01 19:16:27'),(15,6,36,4,'',NULL,0,NULL,'2026-07-02 03:03:36'),(16,19,36,3,'',NULL,0,NULL,'2026-07-02 03:04:31'),(17,10,36,4,'',NULL,0,NULL,'2026-07-05 08:34:09'),(18,8,35,5,'',NULL,0,NULL,'2026-07-05 11:55:40'),(19,7,36,5,'',NULL,0,NULL,'2026-07-05 12:11:41');
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shop_favorites`
--

DROP TABLE IF EXISTS `shop_favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shop_favorites` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `shop_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_fav` (`user_id`,`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shop_favorites`
--

LOCK TABLES `shop_favorites` WRITE;
/*!40000 ALTER TABLE `shop_favorites` DISABLE KEYS */;
INSERT INTO `shop_favorites` VALUES (1,6,15,'2026-06-20 08:41:21'),(2,7,15,'2026-06-20 08:52:10'),(3,7,14,'2026-06-20 08:53:30'),(4,8,16,'2026-06-20 13:02:06'),(10,6,16,'2026-07-01 08:09:13'),(11,8,14,'2026-07-01 11:12:05'),(23,6,14,'2026-07-01 11:19:35'),(26,19,15,'2026-07-01 23:49:38'),(44,6,17,'2026-07-02 01:09:55');
/*!40000 ALTER TABLE `shop_favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shops`
--

DROP TABLE IF EXISTS `shops`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shops` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vendor_id` int(11) NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text DEFAULT NULL,
  `logo` varchar(500) DEFAULT '',
  `phone` varchar(20) NOT NULL,
  `location` varchar(200) NOT NULL,
  `category` varchar(100) DEFAULT '',
  `is_verified` tinyint(1) DEFAULT 0,
  `status` enum('active','banned','suspended') DEFAULT 'active',
  `is_featured` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `vendor_id` (`vendor_id`),
  CONSTRAINT `shops_ibfk_1` FOREIGN KEY (`vendor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shops`
--

LOCK TABLES `shops` WRITE;
/*!40000 ALTER TABLE `shops` DISABLE KEYS */;
INSERT INTO `shops` VALUES (13,3,'AUTH SHOP','vente gros et details des produit haut de gammes','','691526585','Dschang','Autres',1,'active',0,'2026-06-15 22:24:30','2026-06-16 10:24:03'),(14,7,'AUTH SHOP','vente en gros et detaille des produit haut de gammes','','677','Dschang','Boutique',1,'active',0,'2026-06-15 22:49:18','2026-07-01 11:08:43'),(15,8,'WILL SHOPPING','vente en gros et details des ustensiles de cuisines','http://192.168.1.230:8081/uploads/products/product_1782852358_7230b5ca.jpg','683271563','5,4383528, 10,0702069','Boutique',1,'active',1,'2026-06-15 23:48:40','2026-07-05 11:52:39'),(16,12,'SHOP ELEC','vente de tout type d\'accessoires','','652454334','F389+M35, DSCHANG, NGUI, Dschang','Mode',1,'active',0,'2026-06-17 21:12:25','2026-06-18 23:01:59'),(17,25,'agrobrave','vente des phytosanitaires, des semences et matériel agricole','','679520510','Dschang','Agriculture',1,'active',1,'2026-07-01 19:13:15','2026-07-01 19:21:46');
/*!40000 ALTER TABLE `shops` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('buyer','vendor','admin') NOT NULL DEFAULT 'buyer',
  `status` enum('active','banned','suspended') NOT NULL DEFAULT 'active',
  `location` varchar(200) DEFAULT '',
  `avatar` varchar(500) DEFAULT '',
  `last_seen` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Mbié Landry','landry@example.com','690000001','$2y$10$NLJuP.FOGKzTTKDVpZf2C.fZpGkOlweRO56W58oq8vAOXRXHt8Ew.','vendor','active','','','2026-07-02 01:55:52','2026-06-15 14:09:28','2026-07-01 23:55:52'),(2,'Nadege Tchinda','nadege@example.com','690000002','$2y$10$jjoCvF0mJ8mNR927bEoUHOHkW4nBhMPNRQxHVl42Rka8kRpemloQ.','vendor','active','','',NULL,'2026-06-15 14:09:28','2026-06-15 14:09:28'),(3,'Fokou Samuel','samuel@example.com','690000003','$2y$10$ONanK/.O7Aml5QuAxLEjX.FnHpoYLy4g2uR4nTs9/HXDiLe4BPiR6','vendor','active','','',NULL,'2026-06-15 14:09:28','2026-06-15 14:09:28'),(5,'Marie N.','marie@example.com','690000011','$2y$10$qkv2ud4RGmkx2eqskA/T4eWNp3uQFRnU9vCYhOCKsrdG3xeMrZ1tm','buyer','active','','',NULL,'2026-06-15 14:09:28','2026-06-15 14:09:28'),(6,'test','test@gmail.com','454','$2y$10$nGZdL41RSWF.ux5mWRd2Z.qPzNrb4I3nTPxT8ZGnklrXs8RQdHd5q','buyer','active','','http://192.168.1.230:8081/uploads/products/product_1782952989_4662aaa7.jpg','2026-07-05 14:09:19','2026-06-15 21:39:50','2026-07-05 12:09:19'),(7,'test vendeur','testvendeur@gmail.com','677','$2y$10$5tkWdrD90Xuci9B6xRS/leXyWExbFCeI1FXeKehx2WkUlGKYjuEli','vendor','active','','','2026-07-05 14:14:11','2026-06-15 22:48:38','2026-07-05 12:14:11'),(8,'will de chez moi','will@gmail.com','673271563','$2y$10$ft4zeo1Zhc9qleOmLoNxqed9l9NTjFVnN0ZuYkdlXwRCPVDf8dE2m','vendor','active','','http://192.168.1.230:8081/uploads/products/product_1782948222_99818634.jpg','2026-07-05 23:31:53','2026-06-15 23:46:57','2026-07-05 21:31:53'),(10,'Admin Dschang','admin@dschangmarket.com','+237 690 000 000','$2y$10$WC1v3ox5NbZFED6DZF/hW.4VlIfC.pqd9EqOy6ehSELqgDqXjyaey','admin','active','','http://192.168.1.230:8081/uploads/products/product_1782860068_b113a6df.jpg','2026-07-05 23:30:54','2026-06-16 09:38:11','2026-07-05 21:30:54'),(11,'test','yan@gmail.com','6XXXXXXXX','$2y$10$y0nhuoIB88e2r34KFLXTOuGFYA.39Ljjakt9O3NthT0n3weuPYf/2','buyer','active','','',NULL,'2026-06-17 04:12:16','2026-06-17 04:12:16'),(12,'t','t@gmail.com','23','$2y$10$RCHM18KJSwkcF33dPoSzfeislix/XejLdLqD76qo6QDRxhD9Drc4K','vendor','active','','',NULL,'2026-06-17 05:09:25','2026-06-17 21:01:15'),(13,'Test','testuser@test.com','691234567','$2y$10$0OdGkmkvX7oQKZBpEvsUEOABSJws2t9C.Toxp5mZZ06/37adL.3.W','buyer','active','','',NULL,'2026-06-17 05:15:07','2026-06-17 05:15:07'),(14,'yan','yan1@gmail.com','5665','$2y$10$ISmZpcLaoZwCOrUxi9te/usLREL2J8VRxce3XAu2MIeGaL/7ObFRm','vendor','active','','',NULL,'2026-06-17 05:49:16','2026-06-17 06:27:30'),(15,'Peguy','Peguycrush@gmail.com','659750870','$2y$10$HH3XupuB5hQA5d1EIQHg9uE9IXbfxYlk8pyz.aI1CsprONsj4FPd2','buyer','active','','',NULL,'2026-06-17 20:09:03','2026-06-19 22:52:14'),(16,'fotso Ulrich','hurichesfotshlgo@gmail.com','682249081','$2y$10$Hfo2mxUnMv9lyicOBK0g7uCcytRgeTuCZjsAZpXLFVJ5bhncrnokW','buyer','active','','',NULL,'2026-06-17 20:19:16','2026-06-17 21:04:27'),(19,'test1','test1@gmail.com','695231548','$2y$10$nAT6aF2QqmJmPBuR87/y0.ORby.LUwekE/wfZN9P0gj77rSGX/57i','buyer','active','','http://192.168.1.230:8081/uploads/products/product_1782953279_97d9de57.jpg','2026-07-05 14:10:04','2026-06-30 20:53:14','2026-07-05 12:10:04'),(20,'Updated Test','test@test.com','691111111','$2y$10$Cyw4XSF7Ehk7qa8pNMJI8eaWnFZqh.EGwcmBiztWltApWXt9XkJWm','buyer','active','Douala','',NULL,'2026-06-30 21:34:33','2026-06-30 21:34:53'),(21,'Admin Test','admin@test.com','690000001','$2y$10$/JvvlLBLqPjbecLY8pxM9uCppLvvoGpr50LFhdSgpgJcm756B/3Ka','buyer','active','','',NULL,'2026-06-30 21:58:02','2026-06-30 21:58:02'),(22,'Super Admin','super@admin.com','690000099','$2y$10$yiccelh74fXJKuUVgPU9XOllwWhMdZGfQo18FqBa9iAxO7qxELpvu','admin','active','','',NULL,'2026-06-30 21:58:18','2026-06-30 21:58:18'),(25,'sinos brave','yannickfoka231@gmail.com','67852','$2y$10$hxmCs70AMDbAnU7gJtuJBeWhGprKIrkOT0f3fsenCxG/HhyJTwaie','vendor','active','','',NULL,'2026-07-01 19:07:32','2026-07-01 19:09:24');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wishlist`
--

DROP TABLE IF EXISTS `wishlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wishlist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product_wish` (`user_id`,`product_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `wishlist_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wishlist_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishlist`
--

LOCK TABLES `wishlist` WRITE;
/*!40000 ALTER TABLE `wishlist` DISABLE KEYS */;
INSERT INTO `wishlist` VALUES (1,8,29,'2026-06-19 12:43:14'),(2,8,26,'2026-06-19 12:43:18'),(4,8,16,'2026-06-19 12:47:11'),(5,8,28,'2026-06-19 13:18:37'),(6,6,31,'2026-06-19 13:33:53'),(7,10,31,'2026-06-19 22:55:49'),(9,10,27,'2026-06-19 22:55:52'),(10,6,16,'2026-06-29 09:18:22'),(11,6,29,'2026-06-30 15:33:18'),(12,6,27,'2026-06-30 15:33:20'),(13,19,32,'2026-06-30 23:02:22'),(14,8,32,'2026-07-01 06:48:32'),(15,8,33,'2026-07-01 06:48:34'),(16,8,22,'2026-07-01 11:12:27'),(17,10,35,'2026-07-01 19:16:30'),(18,19,34,'2026-07-01 21:24:40');
/*!40000 ALTER TABLE `wishlist` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-10 20:35:54

-- ============================================================
-- Tables recentes (fidelite, portefeuille, push) ajoutees au clone
-- ============================================================
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
