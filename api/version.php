<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

echo json_encode([
    'version_code' => 17,
    'version_name' => '1.0.17',
    'apk_url' => 'https://github.com/GHISLAINANDREW/TiK-Market/releases/latest/download/TiK-Market_latest.apk',
    'release_notes' => "Mise à jour v1.0.17 : Défilement fluide des photos, partage WhatsApp optimisé et téléchargement PDF haute définition.",
    'force_update' => false
]);
