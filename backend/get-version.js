const version = '4.7.2';
const commitHash = require('child_process')
	.execSync('git rev-parse --short HEAD')
	.toString();

const result = version + '-' + commitHash
console.log(result);

return result
