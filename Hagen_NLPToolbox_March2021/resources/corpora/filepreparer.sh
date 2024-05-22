#!/bin/bash
# $Header: /usr/cvs/te/resources/filepreparer.sh,v 1.4 2005/05/02 16:05:34 fwitschel Exp $

ws_jar=../../lib/WordServer.jar
ca_jar=../../lib/cooccaccess.jar

exitf () {
   echo ein fehler ist bei der letzten operation aufgetreten!
   exit 1
}


[ -f "en.txt" ] || (echo kann en.txt nicht finden! && exitf)
[ -f "de.txt" ] || (echo kann de.txt nicht finden! && exitf)
[ -f "${ws_jar}" ] || (echo kann wordserver nicht unter ${ws_jar} finden && exitf)
[ -f "${ca_jar}" ] || (echo kann cooccaccess nicht unter ${ca_jar} finden && exitf)

export CLASSPATH=$CLASSPATH:${ws_jar}:${ca_jar}

echo de_with_wordnumbers.txt: $(wc -l <de_with_wordnumbers.txt) lines

echo erzeuge wordserver de
java -Xmx512m -jar ${ws_jar} de_with_wordnumbers.txt 3 || exitf
#echo erzeuge wordserver en
#java -Xmx512m -jar ${ws_jar} en_with_wordnumbers.txt 3 || exitf

echo erzeuge cooccaccess de-Kookks
java -Xmx512m -classpath ${ca_jar} de.uni_leipzig.asv.coocc.BinFileMultColPreparer cooccs.txt 2 || exitf

echo erzeuge daten fr cooccaccess Referenzkorpus \(spalte 1+3\) fr de
lines=$(sed "s|^\(.*\)\t\(.*\)\t\(.*\)$|\1\t\3|" de_with_wordnumbers.txt |tee de_wordnumbers_counts.txt |wc -l)
[ $? ] || exitf
echo de_wordnumbers_counts.txt: $lines lines

echo erzeuge cooccaccess de-Referenzkorpus
java -Xmx512m -classpath ${ca_jar} de.uni_leipzig.asv.coocc.BinFileMultColPreparer de_wordnumbers_counts.txt 2 || exitf

echo done

