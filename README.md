# MyDMAM Startup scripts
Used with Debian 8 and SystemD

See the [MyDMAM website setup](http://mydmam.org/setup/) instructions for more informations.

## Preparation
- check Git
- check Java JRE Oracle, the same as installed for ElasticSearch
- check FFmpeg, ImageMagick... if needed.
- check mydmam configuration (`conf/app.d/*.yml`, `conf/application.conf` and `dependencies.yml`)
- check dest log directory (`mkdir /var/log/mydmam`)
- check the presence of unzipped directory for Play 1
- check the `chmod +x` for all *.sh files here (startup)

## Setup
- Check configuration vars for `precompile.sh` file (`$BASEPATH`, `$PLAYPATH` and `$PLAY_JAR`).
Don't forget, it's Play which is responsible for compilation.
- `./precompile.sh`

And For Probe and/or Server:
- `cp (this directory)/server.service `usr/lib/systemd/mydmam-server.service` (for enable Server in service)
- `systemctl enable /usr/lib/systemd/mydmam-server.service`
- `systemctl daemon-reload`
- `systemctl start mydmam-server`

Check with
- `systemctl status mydmam-server` and
- `tail -f /var/log/mydmam/mydmam.log`
- Your mails

## Update
See [MyDMAM changelogs](http://mydmam.org/category/changelogs) before updating...
- use Git with mydmam app
- change Play directory
- update-alternative for new Java JRE
- change all Jars in `mydmam/lib` directory
- upgrade ES and/or Cassandra version

Activate this with a simple `./precompile.sh`
Edit it before run if you want to change Play version.
Restart with
- `systemctl daemon-reload`
- `systemctl stop mydmam-{server|probe}`
- `systemctl start mydmam-{server|probe}`
- `systemctl status mydmam-{server|probe}`

## Tips
### Setup Java 
- unpack Oracle’s version somewhere, like in `/opt`
- `update-alternatives --install /usr/bin/java java (extracted directory)/bin/java 2000`
If Debian install some others JRE, check with
`update-alternatives --display java`
if your JRE has the best priority (we set 2000 here).

Don’t forget to setup the *Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files*, free downloadable from [Oracle website](http://www.oracle.com/technetwork/java/javase/downloads/index.html), else the startup script will throw a fatal exception. See the message for known the exact location to extract the files, often here: `(extracted JRE directory)/jre/lib/security/`.

### Update Java
- `update-alternatives --remove java $(realpath /etc/alternatives/java)`
- `update-alternatives --install /usr/bin/java java (extracted directory for new JRE)/bin/java 2000`
Check with
- `update-alternatives --display java`
- `java -version`
- Unpack *Java Cryptography Extension (JCE*)
