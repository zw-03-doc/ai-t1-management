const fs = require('node:fs/promises')
const path = require('node:path')

const KiB = 1024
const imageLimit = 195 * KiB
// Leave room for the extra files and transformations added by WeChat's compiler.
const mainPackageBudget = 1536 * KiB

async function preparePhotos(sourceDir, outputDir) {
  const Jimp = require('jimp')
  const folder = 'static/images'
  for (const name of await fs.readdir(path.join(sourceDir, folder))) {
    if (!/\.jpe?g$/i.test(name)) continue
    const source = path.join(sourceDir, folder, name)
    const target = path.join(outputDir, folder, name)
    const photo = await Jimp.read(source)
    const { width, height } = photo.bitmap
    const maxSide = name === 'brand-icon.jpg' ? 240 : 1080
    const ratio = Math.min(1, maxSide / Math.max(width, height))
    if (ratio === 1 && (await fs.stat(source)).size <= imageLimit) continue
    photo.resize(Math.max(1, Math.round(width * ratio)), Math.max(1, Math.round(height * ratio)))
    const bytes = await photo.quality(75).getBufferAsync(Jimp.MIME_JPEG)
    if (bytes.length > imageLimit) {
      throw new Error(`微信主包图片 ${name} 压缩后为 ${Math.ceil(bytes.length / KiB)} KiB，预算为 195 KiB。`)
    }
    await fs.writeFile(target, bytes)
  }
}

async function mainPackageBytes(folder, subpackageDirs) {
  let bytes = 0
  for (const entry of await fs.readdir(folder, { withFileTypes: true })) {
    const file = path.join(folder, entry.name)
    if (entry.isDirectory() && !subpackageDirs.has(file)) {
      bytes += await mainPackageBytes(file, subpackageDirs)
    } else if (entry.isFile()) {
      bytes += (await fs.stat(file)).size
    }
  }
  return bytes
}

module.exports = function wechatPackage() {
  let sourceDir, outputDir
  return {
    name: 't1-wechat-package',
    apply: (_, env) => env.command === 'build' && process.env.UNI_PLATFORM === 'mp-weixin' && process.env.NODE_ENV === 'production',
    configResolved(config) {
      sourceDir = path.resolve(process.env.UNI_INPUT_DIR || config.root)
      outputDir = path.resolve(config.root, config.build.outDir)
    },
    // uni:copy finishes copying static files in writeBundle. Replace only the
    // WeChat copies, always reading the high-resolution originals from source.
    async closeBundle() {
      if (sourceDir === outputDir) throw new Error('微信输出目录不能与源码目录相同。')
      await preparePhotos(sourceDir, outputDir)
      const app = JSON.parse(await fs.readFile(path.join(outputDir, 'app.json'), 'utf8'))
      const packages = app.subPackages || app.subpackages || []
      const subpackageDirs = new Set(packages.map(pkg => path.resolve(outputDir, pkg.root)))
      const bytes = await mainPackageBytes(outputDir, subpackageDirs)
      const size = Math.ceil(bytes / KiB)
      if (bytes > mainPackageBudget) {
        throw new Error(`微信主包源码为 ${size} KiB，超过 1536 KiB 构建预算。请减小图片或将非首页内容放入分包。`)
      }
      console.log(`微信主包源码 ${size} KiB / 1536 KiB；最终大小以微信预览为准。`)
    }
  }
}
