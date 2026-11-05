import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'
import wechatPackage from './scripts/wechat-package.cjs'
const uni = uniModule.default || uniModule
export default defineConfig({ plugins: [uni(), wechatPackage()], base: '/app/' })
