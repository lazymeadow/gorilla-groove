# Gorilla Groove

## Installation

Copy `/config/application.properties.cfg` to `/config/application.properties`. Environment specific setup will go in here.

### Database
Gorilla Groove uses MySQL. Right now there isn't a hard version requirement. Likely any modern MySQL will work.

Create a database called "groovatron"

Edit `/config/application.properties` and set `spring.datasource.username` and `spring.datasource.password` to be a MySQL user with access to the new `groovatron` database
Then edit `spring.flyway.user` and `spring.flyway.password` to have the same MySQL user credentials

### FFmpeg

FFmpeg is used to convert files to OGG format. It will need to be installed from https://ffmpeg.zeranoe.com/builds/
Once installed, edit `/config.application.properties` and setup `spring.data.ffmpeg.binary.location` to be the location to the FFmpeg binaries.
Setup `spring.data.ffmpeg.output.location` to be some value for music to be exported to when converted

## Running

If using Java 10, add `--add-modules java.xml.bind` to the JVM arguments or you will see a ClassNotFoundException for JAXB

When the projects starts up, it will use Flyway to migrate the database to the latest version.

A (basic) webpage can be found at `localhost:8080` 
