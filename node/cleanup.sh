find . -type f -name 'yarn.lock' -exec rm {} +
find . -type f -name 'package-lock.json' -exec rm {} +
find . -name "node_modules" -type d -prune -exec rm -rf '{}' +
