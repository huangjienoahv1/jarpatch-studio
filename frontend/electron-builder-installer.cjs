const { build } = require('./package.json');

// 与便携包复用同一应用目录，只增加 NSIS 安装向导。
module.exports = {
  ...build,
  win: { ...build.win, target: ['nsis'] },
  nsis: {
    artifactName: 'JarPatch-Studio-${version}-Windows-${arch}-Setup.${ext}',
    oneClick: false,
    perMachine: false,
    allowToChangeInstallationDirectory: true,
    createDesktopShortcut: true,
    createStartMenuShortcut: true,
    shortcutName: 'JarPatch Studio',
    installerLanguages: ['zh_CN', 'en_US'],
    language: '2052',
    runAfterFinish: true
  }
};
