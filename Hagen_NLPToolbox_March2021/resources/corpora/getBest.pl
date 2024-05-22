#!/usr/bin/perl -w
use strict;

if(@ARGV != 2){die("Parameter(2): infile numCooccs");}
my $infile = $ARGV[0];
my $numCooccs = $ARGV[1];

my $curCount = 1;
my $oldWordNr;
my $newWordNr;

open(IN, "<$infile");

# do the first line (initialize $oldWordNr):
my $line = <IN>;
chomp($line);
my @tmp = split(/\t/, $line);
$oldWordNr = $tmp[0];
my $other = $tmp[1];
print "$oldWordNr\t$other\n";

# mow do the rest:
while($line = <IN>){
	
	chomp($line);
	my @tmp2 = split(/\t/, $line);
	my $newWordNr = $tmp2[0];
	my $otherWordNr = $tmp2[1];

	# check if wordNr has changed:
	# if yes: reset curCount
	if($newWordNr != $oldWordNr){
		$curCount = 1;
	}
	# if not: increment curCount...
	else{
		$curCount++;
	}

	# output the $numCooccs best lines from the sorted file:
	if($curCount <= $numCooccs){
		print "$newWordNr\t$otherWordNr\n";
	}
	
	$oldWordNr = $newWordNr;
}
