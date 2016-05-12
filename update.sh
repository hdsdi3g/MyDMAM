#!/bin/sh

wget -m -k -p -c -E -e robots=off --exclude-directories=wp-admin,dwl,comments,trackback,wp-json,author http://mydmam.org

cp -rf mydmam.org/* .

rm -rf mydmam.org

rm wp-login.php*
rm xmlrpc*
