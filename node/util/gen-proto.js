const fs = require('fs');
const mkdirp = require('mkdirp');
const childProcess = require('child_process');

const exeExtension = (process.platform === 'win32') ? '.cmd' : '';
const protocGenTsProtoPath = `./node_modules/.bin/protoc-gen-ts${exeExtension}`;
const outDir = './src/proto';

const protoDir = '../proto';

mkdirp.sync(outDir);

const protoFiles = fs.readdirSync(protoDir)
  .filter((v) => /\.proto$/.test(v));
console.log('protoFiles : ', protoFiles);

childProcess.execSync(
  `protoc --plugin="${protocGenTsProtoPath}" --js_out="import_style=commonjs,binary:${outDir}" --ts_out="${outDir}" --proto_path="${protoDir}" ${protoFiles.join(' ')}`,
  {
    stdio: 'inherit',
  },
);
