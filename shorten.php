<?php

$cmds = array('end','for','next','data','input#','input','dim','read','let','goto','run','if',
              'restore','gosub','return','rem','stop','on','wait','load','save','verify',
              'def','poke','print#','print','cont','list','clr','cmd','sys','open','close',
              'get','new','tab(','to','fn','spc(','then','not','step',
              'and','or','sgn','int','abs','usr','fre','pos','sqr','rnd','log',
              'exp','cos','sin','tan','atn','peek','len','str$','val','asc','chr$','left$',
              'right$','mid$','go'
              );

$prg = tokenize(file($argv[1]));
$prg = replace_strings($prg);
$prg = shorten_vars($prg);
$aims = get_aims($prg);
$prg = reorganize($prg, $aims);
$prg = remove_useless($prg);
$prg = reorder($prg, $aims);
$prg = renumber($prg, $aims);
$prg = maxlen($prg);
pretty_print($prg);

function replace_strings($prg)
{
    global $summe;
    $summe = 0;

    fwrite(STDERR,PHP_EOL);

    $v = 2;
    while (true)
    {
        list($prg,$changed) = replace_string($prg, $v);
        if (!$changed) break;
        $v++;
        if ($v==26) break;
    }

    fwrite(STDERR,"Gesamt = ".$summe.PHP_EOL);

    return $prg;
}

function replace_string($prg, $c)
{
    global $summe;

    // Alle Strings der Länge 5 oder mehr
    $strs = array();
    foreach ($prg as list($lnr,$cmds))
    {
        $is_print = false;
        foreach ($cmds as list($type,$value))
        {
            if ($type=='cmd' and $value=='print')
              $is_print = true;
            if ($type=='tok' and $value==':')
              $is_print = false;
            if ($is_print and $type=='str')
            {
                $h = substr($value,1,strlen($value)-2);
                $l = getstrlen($h);
                if ($l>=5)
                  @$strs[$h]++;
            }
        }
    }

    $conv_strs = array();
    foreach ($strs as $s=>$l)
      $conv_strs[] = array(conv($s),$l);

    // Vorkommen zählen
    $count = array();
    foreach ($conv_strs as list($s,$co))
    {
        for ($i=0;$i<count($s);$i++)
          for ($j=$i+5;$j<=count($s);$j++)
          {
              $hlp = "";
              for ($k=$i;$k<$j;$k++)
                $hlp .= $s[$k];

                @$count[$hlp] += $co;
          }
    }

    // Jeweilige Reduktion berechnen
    $gains = array();
    foreach ($count as $s=>$co)
    {
        $s = strval($s);
        $a = $co*getstrlen($s);
        $b = getstrlen($s)+6+4*$co+7;
        $gain = $a-$b;
        if ($gain>1)
          $gains[$s] = $gain;
    }
    if (empty($gains))
      return array($prg,false);

    arsort($gains);
    foreach ($gains as $s=>$g)
    {
        $va = chr(ord('a')+$c).'$';
        $neu = array(-1,array(array('var',$va),array('tok','='),array('str','"'.$s.'"')));
        $rep = array($s,$va);
        $summe += $g;
        fwrite(STDERR,$va.' = "'.$s.'" ('.$g.')'.PHP_EOL);
        break;
    }

    $nprg = array();
    foreach ($prg as list($lnr,$cmds))
    {
        $ncmds = array();
        $is_print = false;
        foreach ($cmds as list($type,$value))
        {
            if ($type=='cmd' and $value=='print')
              $is_print = true;
            if ($type=='tok' and $value==':')
              $is_print = false;
            if ($is_print and $type=='str' and getstrlen($value)>=7)
            {
                list($s,$va) = $rep;
                if (strpos($value,$s)!==false)
                {
                    $pos = strpos($value,$s);
                    $hlp = substr($value,0,$pos).'"';
                    if (strlen($hlp)>2)
                      $ncmds[] = array('str',$hlp);
                    $ncmds[] = array('var',$va);
                    $hlp = '"'.substr($value,$pos+strlen($s));
                    if (strlen($hlp)>2)
                      $ncmds[] = array('str',$hlp);
                    continue;
                }
            }
            $ncmds[] = array($type,$value);
        }

        $nprg[] = array($lnr,$ncmds);
    }

    array_unshift($nprg,$neu);

    return array($nprg,true);
}

function conv($value)
{
    $erg = array();
    $i = 0;
    while ($i<strlen($value))
    {
        if ($value[$i]=='{')
        {
            $tok = '';
            while ($value[$i]!='}')
            {
                $tok .= $value[$i];
                $i++;
            }
            $tok.='}';
        }
        else
          $tok = $value[$i];

        $erg[] = $tok;
        $i++;
    }
    return $erg;
}

function shorten_vars($prg)
{
    $vars = array();
    foreach ($prg as list($lnr,$cmds))
      foreach ($cmds as list($type,$value))
        if ($type=='var')
          @$vars[$value]++;


    arsort($vars);

    $next = 0;
    $conv = array();
    foreach ($vars as $name=>$count)
    {
        if ($name[strlen($name)-1]=='$' or $name[strlen($name)-1]=='%')
        {
            $conv[$name] = $name;
            continue;
        }

        $conv[$name] = $next<26?chr($next+ord('a')):(chr(ord('a')+($next-26)/10).chr(ord('0')+($next-26)%10));
        $next++;
    }

    $res = fopen('vars','w');
    foreach ($conv as $alt=>$neu)
      fwrite($res,$alt.' => '.$neu.PHP_EOL);
    fclose($res);

    foreach ($prg as $k1=>list($lnr,$cmds))
      foreach ($cmds as $k2=>list($type,$value))
        if ($type=='var')
          $prg[$k1][1][$k2][1] = $conv[$value];

    return $prg;
}

function tokenize($f)
{
    global $cmds;

    $data = array();

    foreach ($f as $line)
    {
        $lnr = intval(trim(substr($line,0,6)));
        $ln = trim(substr($line,6));
        $token = array();
        while (strlen($ln)>0)
        {
            foreach ($cmds as $cmd)
              if (str_starts_with($ln,$cmd))
              {
                  $token[] = array('cmd',$cmd);
                  if ($cmd=='rem' or $cmd=='data')
                  {
                      $token[] = array('dat',$ln);
                      $ln='';
                  }
                  else
                    $ln = substr($ln,strlen($cmd));
                  continue 2;
              }

            if ($ln[0]=='"')
            {
                $str = $ln[0];
                $ln = substr($ln,1);
                while (strlen($ln)>0)
                {
                    $str .= $ln[0];
                    $ln = substr($ln,1);
                    if ($str[strlen($str)-1]=='"') break;
                }
                if ($str[strlen($str)-1]!='"') $str.='"';
                $token[] = array('str',$str);
                continue;
            }

            if (ord($ln[0])>=ord('0') and ord($ln[0])<=ord('9'))
            {
                $nr = $ln[0];
                $ln = substr($ln,1);
                while (strlen($ln)>0 and ord($ln[0])>=ord('0') and ord($ln[0])<=ord('9'))
                {
                    $nr .= $ln[0];
                    $ln = substr($ln,1);
                }
                $token[] = array('num',$nr);
                continue;
            }

            if (ord($ln[0])>=ord('a') and ord($ln[0])<=ord('z'))
            {
                $v = $ln[0];
                $ln = substr($ln,1);

                while (strlen($ln)>0 and ((ord($ln[0])>=ord('0') and ord($ln[0])<=ord('9')) or (ord($ln[0])>=ord('a') and ord($ln[0])<=ord('z'))))
                {
                    if (str_starts_with($ln,'goto') or str_starts_with($ln,'gosub') or str_starts_with($ln,'to') or str_starts_with($ln,'then'))
                      break;
                    $v .= $ln[0];
                    $ln = substr($ln,1);
                }

                if (strlen($ln)>0 and ($ln[0]=='%' or $ln[0]=='$'))
                {
                    $v .= $ln[0];
                    $ln = substr($ln,1);
                }

                $token[] = array('var',$v);
                continue;
            }

            if ($ln[0]==' ')
            {
                $ln = substr($ln,1);
                continue;
            }

            $token[] = array('tok',$ln[0]);
            $ln = substr($ln,1);
        }

        $data[] = array($lnr,$token);
    }

    return $data;
}

function get_aims($prg)
{
    $aims = array();

    foreach ($prg as list($lnr,$cmds))
    {
        $goto = false;
        foreach ($cmds as list($type,$value))
        {
            if ($type=='cmd' and ($value=='goto' or $value=='gosub' or $value=='then'))
            {
                $goto = true;
                continue;
            }

            if ($goto && $type=='num')
              @$aims[$value]++;

            if ($type!='num' and ($type!='tok' or $value!=','))
              $goto = false;
        }
    }

    return $aims;
}

function reorganize($prg, $aims)
{
    ksort($aims);

    $neu = array();

    $chunk = array();
    $keep_end = false;
    $is_end = false;
    $next = array(false,array());
    $cont = false;
    foreach ($prg as list($lnr,$cmds))
    {
        if ($keep_end) $is_end = false;
        if ($is_end)
        {
            $chunk[] = $next;
            $neu[] = $chunk;
            $chunk = array();
            $next = array(isset($aims[$lnr])?$lnr:false,array());
            $cont = false;
        }
        else if (isset($aims[$lnr]) or $keep_end)
        {
            $chunk[] = $next;
            $next = array($lnr,array());
            $cont = false;
        }
        if ($cont)
          $next[1][] = array('tok',':');

        $cont = true;
        $keep_end = false;
        $is_end = false;
        $on_found = false;
        foreach ($cmds as $token)
        {
            $next[1][] = $token;
            if ($token[0]=='cmd')
            {
                if ($token[1]=='if' or $token[1]=='rem' or $token[1]=='data')
                  $keep_end = true;
                if ($token[1]=='end' or ($token[1]=='goto' and !$on_found) or $token[1]=='run' or $token[1]=='return')
                  $is_end = true;
                $on_found = $token[1]=='on';
            }
        }
    }
    $chunk[] = $next;
    $neu[] = $chunk;

    return $neu;
}

function remove_useless($prg)
{
    $neu = array();

    $first = true;
    foreach ($prg as $chunk)
    {
        if (empty($chunk)) continue;
        if (!$first and $chunk[0][0]===false) continue;
        $neu[] = $chunk;
        $first = false;
    }

    return $neu;
}

function reorder($prg, $aims)
{
    $neu = array();
    $chunk = array_shift($prg);
    $chunk[0][0] = 0;
    $neu[] = $chunk;

    arsort($aims);
    foreach ($aims as $lnr => $count)
    {
        foreach ($prg as $k=>$chunk)
          if ($chunk[0][0]==$lnr)
          {
              $neu[] = $chunk;
              unset($prg[$k]);
              break;
          }
    }
    foreach ($prg as $chunk)
      $neu[] = $chunk;

    return $neu;
}

function renumber($prg, $aims)
{
    @$aims[0]++;

    $naims = array();
    foreach ($prg as $chunk)
      foreach ($chunk as list($nr,$cmds))
        if (isset($aims[$nr]))
          $naims[] = $nr;

    $naims = array_flip($naims);

    $res = fopen('lines','w');
    foreach ($naims as $alt=>$neu)
      fwrite($res,$alt.' => '.$neu.PHP_EOL);
    fclose($res);

    $last = 0;
    foreach ($prg as $k1=>$chunk)
      foreach ($chunk as $k2=>list($nr,$cmds))
      {
          $last = $prg[$k1][$k2][0] = $naims[$nr]??$last;

          $goto = false;
          foreach ($cmds as $k3=>list($type,$value))
          {
              if ($type=='cmd' and ($value=='goto' or $value=='gosub' or $value=='then'))
              {
                  $goto = true;
                  continue;
              }

              if ($goto && $type=='num')
                $prg[$k1][$k2][1][$k3][1] = $naims[$value]??'x';

              if ($type!='num' and ($type!='tok' or $value!=','))
                $goto = false;
          }
      }

    return $prg;
}

function maxlen($prg)
{
    $neu = array();

    foreach ($prg as $k1=>$chunk)
    {
        foreach ($chunk as $k2=>list($nr,$cmds))
        {
            $splits = calcSplits($cmds);
            $len = 5;
            $txt = '';
            foreach ($splits as list($slen,$stxt))
            {
                if ($len+1+$slen<257)
                {
                    $len += 1+$slen;
                    $txt .= ':'.$stxt;
                }
                else
                {
                    $neu[] = array($nr,substr($txt,1));
                    $len = 5+1+$slen;
                    $txt = ':'.$stxt;
                }
            }
            $neu[] = array($nr,substr($txt,1));
        }
    }

    return $neu;
}

function calcSplits($cmds)
{
    $splits = array();
    $isif = false;
    $col = array();
    foreach ($cmds as list($type,$value))
    {
        if ($type=='tok' and $value==':' and !$isif)
        {
            $splits[] = array(get_len($col),concat($col));
            $col = array();
        }
        else
          $col[] = array($type,$value);

        if ($type=='cmd' and $value=='if')
          $isif = true;
    }
    $splits[] = array(get_len($col),concat($col));

    return $splits;
}

function concat($col)
{
    $ret = '';
    foreach ($col as list($type,$value))
      $ret .= $value;
    return $ret;
}

function get_len($col)
{
    $len = 0;

    foreach ($col as list($type,$value))
    {
        switch ($type)
        {
         case 'cmd': $len++; break;
         case 'var':
         case 'tok':
         case 'num':
            $len += strlen($value); break;
         case 'str':
            $len += getstrlen($value);
            break;
         default: die($type);
        }
    }

    return $len;
}

function getstrlen($value)
{
    $len = 0;
    $i = 0;
    while ($i<strlen($value))
    {
        $len++;
        if ($value[$i]=='{')
        {
            while ($value[$i]!='}')
              $i++;
        }

        $i++;
    }
    return $len;
}

function pretty_print($prg)
{
    $last = 0;
    foreach ($prg as list($nr,$line))
    {
        if (strlen($line)==0) continue;
        if ($line[strlen($line)-1]=='"' and $line[strlen($line)-2]!=' ')
          $line = substr($line,0,strlen($line)-1);
        $last = $n = $nr===false?$last:$nr;
        while (strlen($n)<5) $n=" ".$n;
        echo $n.' '.$line.PHP_EOL;
    }
}

?>
