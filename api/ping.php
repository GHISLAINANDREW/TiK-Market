<?php
header('Content-Type: application/json');
echo json_encode(['status' => 'ok', 'server' => 'render', 'version' => '1.0.1', 'time' => time()]);
