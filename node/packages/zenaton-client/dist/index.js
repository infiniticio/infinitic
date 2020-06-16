
'use strict'

if (process.env.NODE_ENV === 'production') {
  module.exports = require('./client.cjs.production.min.js')
} else {
  module.exports = require('./client.cjs.development.js')
}
