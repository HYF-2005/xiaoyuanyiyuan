// 图标制作辅助脚本；用 sharp 把 SVG 转成 PNG，需要该依赖。不会随页面自动执行。
// 生成“我的”、账户设置和发布类型弹层使用的绿色线条图标。
// 运行时需要 sharp；页面实际引用生成后的 PNG，不依赖运行时绘图。
// require 引入公共工具；解构写法只取需要的函数，避免在每个页面复制请求代码。
const fs = require('fs')
const path = require('path')
const sharp = require('sharp')

const icons = {
  school: '<path d="M2.5 9 12 4l9.5 5-9.5 5z"/><path d="M6 11v5.5c3.4 2.6 8.6 2.6 12 0V11M21.5 9v6"/>',
  user: '<circle cx="12" cy="7.5" r="3.3"/><path d="M4.5 21v-2.2a7.5 7.5 0 0 1 15 0V21"/>',
  mail: '<rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="m3.5 6 8.5 7 8.5-7"/>',
  settings: '<path d="M13.6 2.6l.8 2.1 2.2.9 2-.9 1.9 1.9-.9 2 .9 2.2 2.1.8v2.8l-2.1.8-.9 2.2.9 2-1.9 1.9-2-.9-2.2.9-.8 2.1h-2.8l-.8-2.1-2.2-.9-2 .9-1.9-1.9.9-2-.9-2.2-2.1-.8v-2.8l2.1-.8.9-2.2-.9-2 1.9-1.9 2 .9 2.2-.9.8-2.1z"/><circle cx="12" cy="12" r="3.1"/>',
  help: '<circle cx="12" cy="12" r="9.5"/><path d="M9.5 9a2.7 2.7 0 0 1 5.2 1c0 1.8-2.7 2.1-2.7 4"/><path d="M12 17.5h.01" stroke-linecap="round" stroke-width="2.4"/>',
  logout: '<path d="M15 4H5.5A2 2 0 0 0 3.5 6v12a2 2 0 0 0 2 2H15"/><path d="M12 12h8m-3-3 3 3-3 3" stroke-linecap="round" stroke-linejoin="round"/>',
  shield: '<path d="M12 2.6 20 6v5.8c0 5-3.1 8-8 9.9-4.9-1.9-8-4.9-8-9.9V6z"/><path d="M12 7.7v8.2M8 11.8h8" stroke-linecap="round"/>',
  lock: '<rect x="4.2" y="10.5" width="15.6" height="11" rx="2.3"/><path d="M7.2 10.5V7.6a4.8 4.8 0 0 1 9.6 0v2.9"/><circle cx="12" cy="16" r="1"/>',
  phone: '<rect x="5.3" y="2" width="13.4" height="20" rx="2"/><path d="M5.3 5h13.4M5.3 18.8h13.4"/><circle cx="12" cy="20.4" r=".45" fill="#00c768" stroke="none"/>',
  power: '<path d="M12 2.3v10.2M7 4.6a9.1 9.1 0 1 0 10 0" stroke-linecap="round"/>',
  info: '<circle cx="12" cy="12" r="9.5"/><path d="M12 10.5v6M11.9 7h.2" stroke-linecap="round" stroke-width="2.3"/>',
  document: '<path d="M6 2.5h8.5L20 8v13.5H6a2 2 0 0 1-2-2v-15a2 2 0 0 1 2-2z"/><path d="M14.5 2.5V8H20M8 12h8M8 15.5h8M8 19h6" stroke-linecap="round"/>',
  privacy: '<rect x="4" y="2.3" width="16" height="19.4" rx="2"/><path d="M4 5.8h16M9.5 16.4h7M9.5 19h5" stroke-linecap="round"/><rect x="7" y="10.9" width="5.7" height="4.9" rx=".8"/><path d="M8.4 10.9V9.8a1.5 1.5 0 0 1 3 0v1.1"/>',
  parcel: '<path d="M6 3h9l4 4v14H5V5a2 2 0 0 1 1-2z"/><path d="M15 3v4h4M8 12h8M8 15h8M13 18h3" stroke-linecap="round"/>',
  meal: '<circle cx="10.5" cy="12" r="8.5"/><circle cx="10.5" cy="12" r="4.5"/><path d="M21 4v16m-3-11c0-3 1.3-5 3-5" stroke-linecap="round"/>'
}

// main：把本文件定义的 SVG 图形转成 PNG 写入图标目录；是制作工具，不要讲成线上页面逻辑。
async function main() {
  for (const [name, shape] of Object.entries(icons)) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="#00c768" stroke-width="1.55" stroke-linecap="round" stroke-linejoin="round">${shape}</svg>`
    await sharp(Buffer.from(svg)).png().toFile(path.join(__dirname, `${name}.png`))
  }
}

main().catch(error => {
  console.error(error); process.exitCode = 1
})
