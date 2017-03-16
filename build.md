# MyDMAM build.xml instructions

Use build.xml to perform some automatic and reverberative operations.

You need **Apache Ant** to start this (available with all Linux distributions, and for Windows, Mac Port, and Mac Brew).

I you wants to compile some Java code, you needs also a **Java JDK 8** (from Oracle or OpenJDK).

And, get the code with Git.

## Usage

```
$ ant target
```

The **build** is the default. It make a ready-to-use MyDMAM directory.

Don't forget to read _setup.md_ instructions for prepare the MyDMAM run.

## Targets for Administrators
 * **build** Prepare dependencies, compile, create jars, and prepare clean project.
 * **clean** Remove build directory, pack archives and source archive.

## Targets for Developers
 * **compile** Invoke javac for make classes.
 * **copyplayjars** Copy Play dependencies to the current lib dir (autonomous task). Don't forget to set _-Dplay="/my/unzip/dir/play"_ in ant command line.
 * **makesource** Create MyDMAM source jar (autonomous task).
 * **eclipsify** Create Eclipse project in MyDMAM root dir (autonomous task).
 * **makelib** Create MyDMAM bin jar.

## Targets for packaging
 * **pack-windows** Create MyDMAM Windows redistributable archive with JRE.
 * **pack-linux** Create MyDMAM Linux redistributable archive with JRE.
 * **pack-mac** Create MyDMAM macOS redistributable archive with JRE.
 * **pack-databases** Create Cassandra and ElasticSearch redistributables for MyDMAM.
 * **makestarters** Prepare Windows/OSX/Linux starters.
 * **preparewinrun4j** Make _WinRun4j_ executable for the MyDMAM look **run only for Windows**.

For download separate JARs you can to [dependencies list page](http://mydmam.org/dwl/lib/list.php). 