#!/bin/bash
#
# Script that runs the addlicense container.
# Before using it, make sure to pull the container from dockerhub:
# > docker pull nokia/addlicense-nokia
#

docker run -e OPTIONS="-s -c infinitic.io -f .license-header -config .addlicense.yml -check" --rm -it -v $(pwd):/myapp nokia/addlicense-nokia:latest
