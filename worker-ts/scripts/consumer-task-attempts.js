const { runTaskAttemptsConsumer } = require('../dist');

const taskname = process.argv[2];
console.log(taskname);
if (!taskname) {
  throw new Error(
    'You need to define the task name to use to consume attempts (lowercased string).'
  );
}

runTaskAttemptsConsumer(taskname);
