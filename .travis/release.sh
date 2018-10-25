#!/bin/bash

##
# This file is part of the ChillDev-Lambda.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2018 © by Rafal Wrzeszcz - Wrzasq.pl.
##

set -ex

REPO=$(git config remote.origin.url)
SSH_REPO=${REPO/https:\/\/github.com\//git@github.com:}

# prevent looping
if [ "${TRAVIS_COMMIT_MESSAGE:0:6}" == "[auto]" ] ; then
    exit
fi

# change origin URL to SSH to allow uploading with SSH key
git remote rm origin
git remote add origin ${SSH_REPO}

# make sure HEAD points to the branch
git checkout ${TRAVIS_BRANCH}

# first make current version release
mvn build-helper:parse-version versions:set versions:commit \
    -DnewVersion="\${semver.majorVersion}.\${semver.minorVersion}.\${semver.incrementalVersion}"
git add -u
git commit -m "[auto] Automated release release."
git push origin ${TRAVIS_BRANCH}:master

# now create a new version commit
mvn build-helper:parse-version versions:set versions:commit \
    -DnewVersion="\${semver.majorVersion}.\${semver.minorVersion}.\${semver.nextIncrementalVersion}-SNAPSHOT"
git add -u
git commit -m "[skip ci] New version bump."
git push origin ${TRAVIS_BRANCH}
