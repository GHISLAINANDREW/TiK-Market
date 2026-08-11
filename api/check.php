<?php
header('Content-Type: text/plain');
echo "Files in current directory:\n";
$files = scandir(__DIR__);
foreach ($files as $file) {
    echo $file . "\n";
}
echo "\nDocument Root: " . $_SERVER['DOCUMENT_ROOT'] . "\n";
echo "Script Filename: " . $_SERVER['SCRIPT_FILENAME'] . "\n";
