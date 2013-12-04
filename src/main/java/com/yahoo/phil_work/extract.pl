while (<>)
 {
    if (/.*\[CONFIG\] \[AntiFire\] (\w+) is a (.*$)/) {
       print "|$1|$2||\n";
    }
 }
