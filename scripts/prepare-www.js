// Копирует приложение в www/ для Capacitor (iOS). Service worker не нужен: файлы лежат внутри приложения.
const fs = require('fs'), path = require('path');
const root = path.resolve(__dirname, '..'), out = path.join(root, 'www');
fs.rmSync(out, {recursive: true, force: true}); fs.mkdirSync(out, {recursive: true});
for (const f of ['index.html', 'manifest.webmanifest']) fs.copyFileSync(path.join(root, f), path.join(out, f));
for (const d of ['data', 'icons']) fs.cpSync(path.join(root, d), path.join(out, d), {recursive: true});
console.log('www готов');
