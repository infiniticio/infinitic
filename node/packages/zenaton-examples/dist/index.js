
'use strict'

if (process.env.NODE_ENV === 'production') {
  module.exports = require('./zenaton-examples.cjs.production.min.js')
} else {
  module.exports = require('./zenaton-examples.cjs.development.js')
}
