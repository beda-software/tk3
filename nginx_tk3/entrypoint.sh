#!/bin/sh

export NAMESERVER=`cat /etc/resolv.conf | grep "nameserver" | awk '{print $2}' | tr '\n' ' '`
envsubst '${NAMESERVER} ${AIDBOX_BASE_DOMAIN}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf 
nginx -g 'daemon off;'
