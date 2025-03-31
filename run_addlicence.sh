#!/bin/bash
# "Commons Clause" License Condition v1.0
#
# The Software is provided to you by the Licensor under the License, as defined
# below, subject to the following condition.
#
# Without limiting other conditions in the License, the grant of rights under the
# License will not include, and the License does not grant to you, the right to
# Sell the Software.
#
# For purposes of the foregoing, "Sell" means practicing any or all of the rights
# granted to you under the License to provide to third parties, for a fee or
# other consideration (including without limitation fees for hosting or
# consulting/ support services related to the Software), a product or service
# whose value derives, entirely or substantially, from the functionality of the
# Software. Any license notice or attribution required by the License must also
# include this Commons Clause License Condition notice.
#
# Software: Infinitic
#
# License: MIT License (https://opensource.org/licenses/MIT)
#
# Licensor: infinitic.io

#
# Script that runs the addlicense container.
# Before using it, make sure to pull the container from dockerhub:
# > docker pull nokia/addlicense-nokia
#

# Default to check mode
MODE="check"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --fix)
      MODE="fix"
      shift
      ;;
    --check)
      MODE="check"
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--check|--fix]"
      exit 1
      ;;
  esac
done

# Set options based on mode
if [ "$MODE" = "check" ]; then
  OPTIONS="-s -c infinitic.io -f .license-header -config .addlicense.yml -check"
else
  OPTIONS="-s -c infinitic.io -f .license-header -config .addlicense.yml"
fi

docker run -e OPTIONS="$OPTIONS" --rm -it -v $(pwd):/myapp nokia/addlicense-nokia:latest
