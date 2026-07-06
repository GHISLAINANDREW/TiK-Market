# Utiliser une image PHP légère
FROM php:8.2-cli-alpine

# Installer les extensions PDO pour MySQL (nécessaires pour Aiven/TiDB)
RUN docker-php-ext-install pdo pdo_mysql

# Créer le répertoire de l'application
WORKDIR /app

# Copier uniquement le dossier API dans le conteneur
COPY api/ .

# Exposer le port que Render va nous donner
EXPOSE 8080

# Commande pour lancer le serveur PHP
# On utilise la variable d'environnement $PORT fournie par Render
CMD php -S 0.0.0.0:$PORT router.php
