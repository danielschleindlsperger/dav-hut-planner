#!/bin/sh -l

set -e

#SHA=$(git rev-parse --short HEAD)
SHA="${GITHUB_SHA}" # provided by github actions
COMMIT_ID="${SHA:0:7}"
VERSION="${COMMIT_ID}"
# TODO: check out how to access git in the docker container
#VERSION=$(git show -s --format=%ci-%h $COMMIT_ID | sed 's/+//g' | sed 's/[:+ ]/-/g')

echo "SHA: ${SHA}"
echo "COMMIT_ID: ${COMMIT_ID}"
echo "Determined version: ${VERSION}"
echo "::set-output name=version::${VERSION}"
