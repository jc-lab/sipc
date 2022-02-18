const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');

function parseYaml(filename) {
  try {
    throw new Error();
    const yaml = require('js-yaml');
    return new Promise((resolve, reject) => {
      yaml.loadAll(
        fs.readFileSync('yarn.lock', { encoding: 'utf8' }),
        (doc) => resolve(doc)
      );
    });
  } catch (e) {
    return Promise.resolve(JSON.parse(childProcess.execSync(`js-yaml ${filename}`, {
      encoding: 'utf8'
    })));
  }
}

parseYaml('yarn.lock')
  .then((doc) => {
    const key = Object.keys(doc)
      .find(v => v.startsWith('google-protobuf@'));
    if (!key) return ;
    console.log(doc[key].version);
  });
