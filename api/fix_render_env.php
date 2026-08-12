<?php
/**
 * Script to clean Render environment variables via API.
 * This removes accidental spaces or newlines in keys and values.
 */

$apiKey = 'rnd_Osa8VtPBreUDwRpH7zOmSGYeH2XL';
$serviceId = 'srv-d9ttmajm8hqs73e1pu3g'; // TiK-Market service ID

function callRender($path, $method = 'GET', $data = null) {
    global $apiKey;
    $url = "https://api.render.com/v1" . $path;

    $ch = curl_init($url);
    $headers = [
        "Authorization: Bearer $apiKey",
        "Accept: application/json",
        "Content-Type: application/json"
    ];

    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);

    if ($data) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
    }

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    return ['status' => $status, 'data' => json_decode($response, true)];
}

echo "1. Fetching current variables...\n";
$res = callRender("/services/$serviceId/env-vars");

if ($res['status'] !== 200) {
    die("Error fetching vars: " . print_r($res, true));
}

$currentVars = $res['data'];
$cleanedVars = [];

echo "2. Cleaning variables...\n";
foreach ($currentVars as $item) {
    $ev = $item['envVar'];
    // Remove ALL whitespace from keys, and trim values
    $cleanKey = preg_replace('/\s+/', '', $ev['key']);
    $cleanValue = trim($ev['value']);

    echo "   - [{$ev['key']}] -> [$cleanKey]\n";

    $cleanedVars[] = [
        'key' => $cleanKey,
        'value' => $cleanValue
    ];
}

// ⚠️ Double check: DB_NAME should be defaultdb (based on earlier logs)
// and DB_HOST should be the NEW one.
echo "3. Updating Render service...\n";
$updateRes = callRender("/services/$serviceId/env-vars", 'PUT', $cleanedVars);

if ($updateRes['status'] >= 200 && $updateRes['status'] < 300) {
    echo "✅ Success! Render variables updated and cleaned.\n";
    echo "The service is now redeploying.\n";
} else {
    echo "❌ Update failed: " . print_r($updateRes, true);
}
