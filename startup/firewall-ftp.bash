#!/bin/bash

/sbin/sysctl -w net.ipv4.ip_forward=1

function open_port {
        FROM=$1
        TO=$2
        #/sbin/iptables -D INPUT -p tcp --dport $FROM -j ACCEPT
        /sbin/iptables -t nat -D PREROUTING -p tcp --dport $TO -j REDIRECT --to-port $FROM
        #/sbin/iptables -A INPUT -p tcp --dport $FROM -j ACCEPT
        /sbin/iptables -t nat -A PREROUTING -p tcp --dport $TO -j REDIRECT --to-port $FROM
}

open_port 5020 20;
open_port 5021 21;

#ExecStopPost=/sbin/iptables -D INPUT -p tcp --dport $FROM -j ACCEPT
#ExecStopPost=/sbin/iptables -t nat -D PREROUTING -p tcp --dport $TO -j REDIRECT --to-ports $FROM
