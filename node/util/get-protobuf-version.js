const fs = require('fs');
const yarnLockParse = require('@yarn-tool/yarnlock-parse');

const parsed = yarnLockParse.yarnLockParse(fs.readFileSync('yarn.lock', { encoding: 'utf8' }));
const key = Object.keys(parsed.data).filter(v => /^google-protobuf/.test(v))
    .sort()
    .reverse()[0];
const item = parsed.data[key];
console.log(item.version);
