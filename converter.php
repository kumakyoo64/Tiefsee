<?php

$f = file($argv[1]);

$output = array();

$lnr = 0;
$text = '';
$opt = array();
while (true)
{
    if (empty($f)) break;

    $line = trim(array_shift($f));
    if (strlen($line)==0) continue;

    if (strpos($line,'|')!==false)
      $line = trim(substr($line,0,strpos($line,'|')));

    if (strlen($line)>=2 and $line[0]!='#' and $line[1]!=' ')
    {
        fwrite(STDERR,"Wrong line: ".$line.PHP_EOL);
        die(-1);
    }

    if ($line[0]!='T' && $text!='')
    {
        outtext($text);
        $text = '';
    }

    switch ($line[0])
    {
     case '#': break;
     case '.':
        $tmp = explode(' ',$line,2);
        out($tmp[1]);
        break;
     case 'L':
        $tmp = explode(' ',$line,3);
        $lnr = $tmp[1];
        break;
     case 'B':
        $tmp = explode(' ',$line,3);
        out('syshd:syslo,'.$tmp[1].':syssh');
        break;
     case 'C':
        out('gosub60000');
        break;
     case 'T':
        $tmp = explode(' ',$line,2);
        $text .= ($tmp[1]??'').' ';
        break;
     case 'W':
        $tmp = explode(' ',$line,2);
        if ($tmp[1]=='!')
          out('gosub60020');
        else
          out('gosub60010');
        break;
     case 'Q':
        if (empty($opt))
          out('syswt,y');
        else
        {
            outopt($opt, $iopt);
            $opt = array();
            $iopt = array();
        }
        break;
     case 'A':
        $opt[] = $line;
        break;
     case 'I':
        $iopt[] = $line;
        break;
     case 'S':
        out('gosub60200');
        break;
     default:
        fwrite(STDERR,"Unknown line: ".$line.PHP_EOL);
        die(-1);
    }
}

foreach ($output as list($nr,$line))
{
    while (strlen($nr)<5) $nr=' '.$nr;
    echo $nr.' '.$line.PHP_EOL;
}

function outopt($o, $io)
{
    global $lnr;

    $hascond = false;
    $gotos = [];
    $conds = [];
    foreach ($o as $nr=>$line)
    {
        $tmp = explode(' ',$line);
        array_shift($tmp);
        $cond = false;
        $nr = array_shift($tmp);
        if (!is_numeric($nr))
        {
            $cond = $nr;
            $nr = array_shift($tmp);
        }
        $txt = implode(' ',$tmp);
        $gotos[] = $nr;
        $conds[] = $cond;
        if ($cond!=false)
        {
            $hascond = true;
            out('if'.$cond.'thenprinta$"'.conv($txt));
        }
        else
          out('printa$"'.conv($txt));
    }
    $slnr = $lnr;
    if ($hascond)
    {
        out('gosub60300:ifr=0goto'.$lnr);
        foreach ($io as $line)
        {
            $tmp = explode(' ',$line,3);
            out('ifr>=129thenifiv(r-129)='.$tmp[1].'then'.$tmp[2]);
        }
        for ($i=0;$i<count($gotos);$i++)
        {
            $g = $gotos[$i];
            $c = $conds[$i];
            if ($c===false)
              out('r=r-1:ifr=0goto'.$g);
            else
              out('if'.$c.'thenr=r-1:ifr=0goto'.$g);
        }
    }
    else
    {
        out('gosub60300:onr+1goto'.$lnr.','.implode(',',$gotos));
        foreach ($io as $line)
        {
            $tmp = explode(' ',$line,3);
            out('ifr>=129thenifiv(r-129)='.$tmp[1].'then'.$tmp[2]);
        }
    }
    out('goto'.$slnr);
}

function outtext($t)
{
    $h = explode(' ',substr($t,0,strlen($t)-1));

    $hlp = '';
    while (count($h)!=0)
    {
        $first = array_shift($h);
        if ($first=='')
        {
            if ($hlp=='')
              $hlp = chr(0);
            else
            {
                outline(conv($hlp).'{down}');
                $hlp = '';
            }
        }
        else if (mb_strlen($hlp)+mb_strlen($first)>24)
        {
            outline(conv($hlp));
            $hlp = $first;
        }
        else
          $hlp .= ' '.$first;
    }
    if (mb_strlen($hlp)>0)
      outline(conv($hlp));
}

function conv($t)
{
    return str_replace(array('ß','ä','ö','ü','Ä','Ö','Ü','"'),array('@','[','\\',']','{CBM--}','~','{CBM-*}','{$a0}'),$t);
}

function outline($hlp)
{
    $hlp = str_replace(chr(0).' ','{down}',$hlp);
    out('printa$"'.trim($hlp));
}

function out($code)
{
    global $lnr, $output;

    $output[] = array($lnr,$code);
    $lnr += 1;
}

?>
