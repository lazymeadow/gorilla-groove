# Gorilla Groove

## Installation

Copy `/config/application.properties.cfg` to `/config/application.properties`. 

Environment specific setup will go in here. Some of the items in that file will need to be configured for the server to run successfully.

### Database
Gorilla Groove uses MySQL. Right now there isn't a hard version requirement. Likely any modern MySQL will work.

Create a database called "groovatron"

Edit `/config/application.properties` and set `spring.datasource.username` and `spring.datasource.password` to be a MySQL user with access to the new `groovatron` database

If your MySQL is running on localhost with port 3306, then you are done. If it isn't, edit `spring.datasource.url` to be accurate for your database's IP and port

### FFmpeg

FFmpeg is used to convert files to OGG format. It will need to be installed from https://www.ffmpeg.org/download.html
Once installed, edit `/config/application.properties` and setup `spring.data.ffmpeg.binary.location` to be the location to the FFmpeg binaries.
Setup `spring.data.ffmpeg.output.location` to be some value for music to be exported to when converted

## Running

If using Java 10, add `--add-modules java.xml.bind` to the JVM arguments or you will see a ClassNotFoundException for JAXB

When the projects starts up, it will use Flyway to migrate the database to the latest version.

### User Accounts

User accounts are invite only, and the first user has to be created by hand in the DB
By default the migrations add a user with the email "dude@dude.dude" and the password "dude"

You may, however, add your own user. 
The first way is to add it directly to the `user` DB table. The password will need to be encoded with BCrypt

However it is recommended to log in with the "dude" user and create any additional users through the UI

(See the README in the UI directory for how to do so)

### Logging in and sending requests
With the user created (or the default user), authenticate by sending a POST to:

`http://localhost:8080/api/authentication/login`

using the POST body
```$xslt
{ 
  "email": "the@best.email",
  "password": "plaintext-is-the-best"
}
```

This will return a UUID as an authentication token. You can then access authenticated resources by using the Authorization header like so:
```
Authorization: Bearer e9bedd3a-f476-46d9-aa31-92729342a3c3
```

Or setting this as a cookie with the name "cookieToken" and the value "e9bedd3a-f476-46d9-aa31-92729342a3c3"
