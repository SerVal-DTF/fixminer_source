#!/bin/bash
bugrev=1913
dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd ) 

#Check if coverage is being run. If so, don't use time limit.
#if [ `basename $2` = "coverage" ] ; then
#    cov=0
#else
cov=0
#fi

run_test()
{
    cd $dir/src/tests
    if [ $cov -eq 0 ] ; then
        perl /experiment/lighttpd-run-tests.pl $1
    else
        timeout 5 perl /experiment/lighttpd-run-tests.pl $1
    fi
    RESULT=$?
    if [ $RESULT = 0 ] ; then 
	echo ""
	echo "PASS"
    else
	echo ""
	echo "FAIL"
    fi
    killall -9 lighttpd &> /dev/null
    cd ../../
    return 0
}
case $1 in
    p1) run_test 1 && exit 0 ;; 
    p2) run_test 2 && exit 0 ;; 
    p3) run_test 3 && exit 0 ;; 
    p4) run_test 4 && exit 0 ;; 
    p5) run_test 6 && exit 0 ;; 
    p6) run_test 7 && exit 0 ;; 
    p7) run_test 8 && exit 0 ;; 
    p8) run_test 10 && exit 0 ;; 
    p9) run_test 11 && exit 0 ;; 
    p10) run_test 12 && exit 0 ;; 
    p11) run_test 14 && exit 0 ;; 
    p12) run_test 15 && exit 0 ;; 
    p13) run_test 16 && exit 0 ;; 
    p14) run_test 17 && exit 0 ;; 
    p15) run_test 18 && exit 0 ;; 
    p16) run_test 20 && exit 0 ;; 
    p17) run_test 21 && exit 0 ;; 
    n1) run_test 9 && exit 0 ;; 
esac
exit 1
