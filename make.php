<?php

$bilder = array('sonderangebot2','titanic2','kommando','knopf','bildschirm',
                'innen1','innen2','schrank','luke','unterbett',
                'inkiste1','inkiste2','schrank_offen','orgie','port',
                'goldquest','kommando_gold','port2',
                'heim0','heim1','insel','insel2','autor',
                'innen1a','innen2a');

cmd("acme asm.a");
cmd("acme char.a");
cmd("cd Bilder; javac RTI.java");
foreach ($bilder as $bild)
{
    if (!file_exists("Bilder/".$bild.".a"))
        cmd("cd Bilder; java RTI -o ".$bild." ".$bild.".rti");
    cmd("cd Bilder; acme ".$bild.'.a');
}
cmd("php converter.php tiefsee.u > main");
cmd("php shorten.php main > main2");
cmd("petcat -w2 -l 3201 -o main.prg -- main2");
cmd("c1541 -attach tiefsee.d64 -del \"*\"");
cmd("c1541 -attach tiefsee.d64 -write tiefsee.prg tiefsee");
cmd("c1541 -attach tiefsee.d64 -write main.prg main");
cmd("c1541 -attach tiefsee.d64 -write asm.prg asm");
cmd("c1541 -attach tiefsee.d64 -write char.prg char");
foreach ($bilder as $bild)
  cmd("c1541 -attach tiefsee.d64 -write Bilder/".$bild.".prg bild".nn());
cmd("c1541 -attach tiefsee.d64 -dir",true);

function cmd($cmd,$show=false)
{
    echo $cmd.' ';
    exec($cmd,$out,$ret);
    if ($ret==0)
    {
        echo 'done.'.PHP_EOL;
        if ($show)
          echo implode("\n",$out).PHP_EOL;
        return;
    }
    echo 'failed.'.PHP_EOL;
    die(implode("\n",$out));
}

function nn()
{
    global $n;

    $ret = chr(ord('a')+$n/16).chr(ord('a')+$n%16);
    $n++;
    return $ret;
}


?>
