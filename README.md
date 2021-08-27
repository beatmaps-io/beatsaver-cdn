# Beatsaver CDN

[![Build Status](https://jenkins.kirkstall.top-cat.me/buildStatus/icon?job=CDN)](https://jenkins.kirkstall.top-cat.me/view/Beatsaver/job/CDN/)

Very simple app for serving static files.

Maintains a local db to correctly determine access for deleted maps, redirect keys to published versions and provide the user with a readable filename.

The db is fed with updates over AMQP and a message is placed on AMQP for every zip download to increment the counter without delaying the client waiting to talk across continents.

## Local setup

#### Prerequisites
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Community Edition is fine)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

#### Lets go
- Run `docker-compose -d up` to start the local database  
If you're already running postgres for the main app you should be able to just create a new schema called `cdn`
- Open the project in IntelliJ IDEA
- Run the `run` gradle task

#### Extra environment variables
- `ZIP_DIR` Directory zips will get served from
- `COVER_DIR` Directory cover images will get served from
- `AUDIO_DIR` Directory preview audio will get served from
- `AVATAR_DIR` Directory avatars will get served from

Zips, covers and audio files must be placed in a subfolder that is named with their first character

e.g. `cb9f1581ff6c09130c991db8823c5953c660688f.zip` must be in `$ZIP_DIR/c/cb9f1581ff6c09130c991db8823c5953c660688f.zip`

## Code Style

This project uses ktlint which provides fairly basic style rules.

You can run the `ktlinkFormat` gradle task to automatically apply most of them or the `ktlinkCheck` task to get a list of linting errors.

## Volunteer

Beatsaver serves between 3-4 TB of data every day to players all over the world. This requires hosting the library of maps on multiple continents and isn't cheap. 
If you can volunteer bandwidth feel free to DM me on discord Top_Cat#1961
