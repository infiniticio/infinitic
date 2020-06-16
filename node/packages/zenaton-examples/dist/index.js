
'use strict'

if (process.env.NODE_ENV === 'production') {
  module.exports = require('./examples.cjs.production.min.js')
} else {
  module.exports = require('./examples.cjs.development.js')
}
