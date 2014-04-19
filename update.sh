#!/bin/sh

wget -m -k -p -c -E -e robots=off --exclude-directories=wp-admin,dwl,comment-page,trackback http://mydmam.org

cp -rf mydmam.org/* .

rm -rf mydmam.org

