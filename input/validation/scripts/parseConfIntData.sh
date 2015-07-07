#!/bin/sh

FILENAME=$1
OUTFILE=$1_out
#echo "Your file to be parsed is $1"
#echo "Number of arguments passed $#"

if [ $# -ne 1 ]; then
	echo "Please pass an argument"
	exit;
fi

#for i in `cat $FILENAME`
#do
#value=`echo $i`
#range=`echo $i | cut -f2 | awk -F+- '{print $2}'`
#echo $i
#done

while read line
do
#echo $line
users=`echo $line | cut -d ' ' -f1`
value=`echo $line | cut -d ' ' -f2 | awk -F+- '{print $1}'`
range=`echo $line | cut -d ' ' -f2 | awk -F+- '{print $2}'`
range_bc=`echo ${range} | sed 's/E/\\*10\\^/' | sed 's/+//'`
upper=`echo "scale = 15; $value + $range_bc"|bc`
lower=`echo "scale = 15; $value - $range_bc"|bc`
echo "$users	$lower	$upper" >> $OUTFILE
done < $FILENAME

