#!/bin/bash
# $Header: /usr/cvs/te/resources/filepreparer_de.sh,v 1.1 2005/01/12 09:54:16 fwitschel Exp $

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

echo de.txt: $(wc -l <de.txt) lines
#echo en.txt: $(wc -l <en.txt) lines

echo sortiere de.txt
sort -k2 de.txt >foo
mv foo de.txt

#echo sortiere en.txt
#sort -k2 en.txt >foo
#mv foo en.txt

echo erzeuge wortnummern für de.txt
lines=$(cat -n de.txt |sed "s|^ *||" | tee de_with_wordnumbers.txt |wc -l)
[ $? ] || exitf
echo de_with_wordnumbers.txt: $lines lines

#echo erzeuge wortnummern für en.txt
#lines=$(cat -n en.txt |sed "s|^ *||" |tee en_with_wordnumbers.txt |wc -l)
#[ $? ] || exitf
#echo en_with_wordnumbers.txt: $lines lines

#echo erzeuge daten für wordserver \(spalte 1+2\) für de:
#lines=$(sed "s|^\(.*\)\t\(.*\)\t\(.*\)$|\1\t\2|" de_with_wordnumbers.txt |tee de_wordnumbers_words.txt |wc -l)
#[ $? ] || exitf
#echo de_wordnumbers_words.txt: $lines lines

#echo erzeuge daten für wordserver \(spalte 1+2\) für en
#lines=$(sed "s|^\(.*\)\t\(.*\)\t\(.*\)$|\1\t\2|" en_with_wordnumbers.txt |tee en_wordnumbers_words.txt |wc -l)
#[ $? ] || exitf
#echo en_wordnumbers_words.txt: $lines lines

echo erzeuge wordserver de
java -Xmx512m -jar ${ws_jar} de_with_wordnumbers.txt 3 || exitf
#echo erzeuge wordserver en
#java -Xmx512m -jar ${ws_jar} en_with_wordnumbers.txt 3 || exitf

echo erzeuge daten für cooccaccess \(spalte 1+3\) für de
lines=$(sed "s|^\(.*\)\t\(.*\)\t\(.*\)$|\1\t\3|" de_with_wordnumbers.txt |tee de_wordnumbers_counts.txt |wc -l)
[ $? ] || exitf
echo de_wordnumbers_counts.txt: $lines lines

#echo erzeuge daten für cooccaccess \(spalte 1+3\) für en
#lines=$(sed "s|^\(.*\)\t\(.*\)\t\(.*\)$|\1\t\3|" en_with_wordnumbers.txt |tee en_wordnumbers_counts.txt |wc -l)
#[ $? ] || exitf
#echo en_wordnumbers_counts.txt: $lines lines

echo erzeuge cooccaccess de
java -Xmx512m -classpath ${ca_jar} de.uni_leipzig.asv.coocc.BinFileMultColPreparer de_wordnumbers_counts.txt 2 || exitf
#echo erzeuge cooccaccess en
#java -Xmx512m -classpath ${ca_jar} de.uni_leipzig.asv.coocc.BinFileMultColPreparer en_wordnumbers_counts.txt 2 || exitf

echo done

