-- phpMyAdmin SQL Dump
-- École d'Informatique
-- 2026-10-08
-- Version du serveur : 10.11.14-MariaDB
-- Version de PHP : 8.2.29

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `tik_market`
--

-- --------------------------------------------------------

--
-- Structure de la table `live_comment_likes`
--

CREATE TABLE IF NOT EXISTS `live_comment_likes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `stream_id` int(11) NOT NULL,
  `comment_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_like` (`stream_id`,`comment_id`,`user_id`),
  KEY `idx_comment_likes` (`comment_id`),
  CONSTRAINT `fk_live_comment_likes_stream` FOREIGN KEY (`stream_id`) REFERENCES `live_streams` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_live_comment_likes_comment` FOREIGN KEY (`comment_id`) REFERENCES `live_comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_live_comment_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
