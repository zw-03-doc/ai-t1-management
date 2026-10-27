const { spawnSync } = require('node:child_process');
const path = require('node:path');
const platform = process.argv[2] || 'h5';
const root = path.resolve(__dirname, '..');
const result = spawnSync(process.execPath, [require.resolve('@dcloudio/vite-plugin-uni/bin/uni.js'), 'build', '-p', platform, ...process.argv.slice(3)], {
  cwd: root, stdio: 'inherit', env: { ...process.env, UNI_INPUT_DIR: root, UNI_OUTPUT_DIR: path.join(root, 'dist', platform) }
});
process.exit(result.status ?? 1);
