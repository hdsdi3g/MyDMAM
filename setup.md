# MyDMAM Setup instructions

Setup instructions can be found in the [MyDMAM site](http://mydmam.org/setup/). **Take a look before continuing.** You should download [prepared packs](http://mydmam.org/downloads/).

Don't forget to read [best setup practices](http://mydmam.org/setup/performance-tips-for-production/) before start MyDMAM in production. Also read [security recommendations](http://mydmam.org/setup/security-recommendations/).

After unpack MyDMAM archive or do an `ant build`:

* Windows: use goto in scripts/*.bat / *.exe
* macOS / Linux, `cd startup`
* `bash linux-bootstrap.bash` OR `macos-bootstrap.bash`
* If this instance not needs a HTTP Server (web interface), remove  `/conf/app.d/play.yml`
* Check configuration files in `/conf/app.d`, setup databases addresses, master password key, your mail... Only `.yml` files are loaded. Do not use tabulations in this files.
* Test a startup with cli, like the script tell you to do. For Linux: `runuser -u mydmam /opt/mydmam/startup/mydmam-cli.bash [-- parameters]`
* For Linux, you can setup cli in /bin directory for get in in path `ln -s (here)/startup/mydmam-cli.bash /bin/mydmam`
* Activate service (see .bash files) and start it.
* Keep an eye to logs. Log file are declared in `/conf/log4j.xml', and only this file (it's target to /var/log/mydmam/mydmam.log by default).
* For web interface, Play Server needs some seconds to start, so wait for it.
* Then pickup the auto-generated new admin password in `mydmam/play-new-password.txt`, goto to `http://(mydman-server-addr):9001/` and logon.

You should setup NTP time synchronization.

### Linux, FTP server and port restriction

FTP Server can't run in non-root user and listen on TCP ports 20 and 21.

So:
 * You can run it on root (just remove user and group from service), but it's not a good idea.
 * Or set a firewall rule. Add `ExecStartPre=bash /opt/mydmam/startup/firewall-ftp.bash` in service setup.

By default, FTP Server configuration listen on TCP/5020 and 5021.

On macOS and Windows, just set in ftpserver.yml configuration `listen` and `active` to 21 and 20.

## Configure external tools

It's not mandatory but necessary for manipulate medias.

MyDMAM can detect this tools. You can force to set binaries executables paths in `conf/app.d/executables.yml`

### ffmpeg
Compile or setup ffmpeg with h264 + aac support with a version > 3.0

### ImageMagick 
The actual ImageMagick distribution works well. You can install it from packages.

### BBC BMX MXF 
Compile the actual package.

## Build and configure MyDMAM

In the case if you want build yourself a MyDMAM package (more informations in _build.md_):

 * Install Apache ant
 * Install a Java JDK 8
 * Install git
 * Git clone from GitHub: https://github.com/hdsdi3g/MyDMAM.git
 * ant build
 * ant pack-databases
