<?php

for ($i = 1; $i <= 10; $i++) {
  $url = "https://osmcha.org/api/v1/changesets/?editor=noteSolver&page_size=500&page=" . $i;
  file_put_contents("page-$i.json", getOsmCha($url));
}

function getOsmCha($url) {
    $headers = array('Authorization: Token 279785fdba463240fc660c306909236552769c0e', 'accept: application/json');
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_VERBOSE, false);
    curl_setopt($ch, CURLOPT_POST, false);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);
    curl_setopt($ch, CURLOPT_TIMEOUT, 300);
    $retval = curl_exec($ch);
    curl_close($ch);
    return $retval;
}

?>