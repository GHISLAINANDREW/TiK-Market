<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

echo json_encode([
    'version_code' => 19,
    'version_name' => '1.0.19',
    'apk_url' => 'https://github.com/GHISLAINANDREW/TiK-Market/releases/latest/download/TiK-Market_latest.apk',
    'release_notes' => "Mise à jour v1.0.19 : Géolocalisation automatique des boutiques, tri par distance du plus proche au plus éloigné et itinéraire GPS en un clic !",
    'force_update' => false
]);
