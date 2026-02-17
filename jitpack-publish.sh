#!/usr/bin/env sh
./gradlew publishToMavenLocal -Pversion=$VERSION -Pgroup="${GROUP}.${ARTIFACT}"