<?php

$vers = Array();
$user = Array();
$mont = Array();

foreach(glob("*.json") as $f) {
  $j = json_decode(file_get_contents($f));
  foreach($j->features as $cs) {
    if (!in_array($cs->properties->editor, array_keys($vers))) $vers[$cs->properties->editor] = 1; else $vers[$cs->properties->editor]++;
    if (!in_array($cs->properties->user, array_keys($user))) $user[$cs->properties->user] = 1; else $user[$cs->properties->user]++;
    if (!in_array(substr($cs->properties->date,0,7), array_keys($mont))) $mont[substr($cs->properties->date,0,7)] = 1; else $mont[substr($cs->properties->date,0,7)]++;
  }
  echo $f."\n";
}

foreach(array_keys($vers) as $v) {
  preg_match("/noteSolver\_plugin\/([^;]*)/", $v, $m1);
  preg_match("/JOSM\/1.5 \((\d{5}) (Debian )?([^\)]*)/", $v, $m2);
  $vers2[$m1[1]]++;
  $vers2[$m2[1]]++;
  $vers2[$m2[3]]++;
}

#print_r($vers);
#print_r($user);
#print_r($mont);
print_r($vers2);

?>