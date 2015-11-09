# MyDMAM Startup scripts
Used with Debian 8 and SystemD

## Preparation
- check Git
- check Java JRE Oracle, the same as installed for ElasticSearch
- check FFmpeg, ImageMagick... if needed.
- check mydmam configuration (conf/app.d/*.yml, conf/application.conf, conf/dependencies.yml and conf/log4j.xml)
- check dest log directory (mkdir /var/log/mydmam)
- check the presence of unzipped directory for Play 1
- check the "chmod +x" for all *.sh files here (startup)

## Setup
- Check configuration vars for precompile.sh file ($BASEPATH, $PLAYPATH and $PLAY_JAR).
Don't forget, it's Play which is responsible for compilation.
- ./precompile.sh

And For Probe and/or Server:
- cp (this directory)/server.service /usr/lib/systemd/mydmam-server.service (for enable Server in service)
- systemctl enable /usr/lib/systemd/mydmam-server.service
- systemctl daemon-reload
- systemctl start mydmam-server

Check with
- systemctl status mydmam-server and
- tail -f /var/log/mydmam/mydmam.log
- Your mails

## Update
See [MyDMAM changelogs](http://mydmam.org/category/changelogs) before updating...
- use Git with mydmam app
- change Play directory
- update-alternative for new Java JRE
- change all Jars in mydmam/lib directory
- upgrade ES and/or Cassandra version

Activate this with a simple ./precompile.sh
Edit it before run if you want to change Play version.
Restart with
- systemctl daemon-reload
- systemctl stop mydmam-{server|probe}
- systemctl start mydmam-{server|probe}
- systemctl status mydmam-{server|probe}
