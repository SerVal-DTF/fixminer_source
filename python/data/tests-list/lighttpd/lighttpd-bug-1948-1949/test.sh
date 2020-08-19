#!/bin/bash
bugrev=1948
dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
run_test()
{
    cd $dir/src/tests
    perl ../../lighttpd-run-tests.pl $1
    if [ $? = 0 ] ; then
        echo ""
        echo "PASS"
    else
        echo ""
        echo "FAIL"
    fi
    popd > /dev/null
    killall -9 lighttpd &> /dev/null
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
    n1) run_test 19 && exit 0 ;; 
esac
exit 1
