const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const express = require('express');
const fs = require('fs');
const https = require('https');
const path = require('path');
const crypto = require('crypto');
const { execFile } = require('child_process');
const multer = require('multer');

const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, uniqueSuffix + '-' + file.originalname);
    }
});

const upload = multer({ storage: storage });

const app = express();
const port = process.env.PORT || 3000;

let isConnected = false;
let chromePath = process.env.CHROME_PATH;

if (!chromePath) {
    if (process.platform === 'android' || fs.existsSync('/data/data/com.termux')) {
        chromePath = '/data/data/com.termux/files/usr/bin/chromium';
    } else if (process.platform === 'win32') {
        chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
    } else {
        chromePath = '/usr/bin/chromium-browser';
    }
}

const client = new Client({
    authStrategy: new LocalAuth(),
    qrMaxRetries: 5,
    takeoverOnConflict: true,
    authTimeoutMs: 180000, 
    ...(process.env.WA_WEB_VERSION ? {
        webVersion: process.env.WA_WEB_VERSION,
        webVersionCache: {
            type: 'remote',
            remotePath: 'https://raw.githubusercontent.com/wppconnect-team/wa-version/main/html/{version}.html'
        }
    } : {}),
    puppeteer: {
        headless: true,
        handleSIGINT: false,
        executablePath: chromePath,
        timeout: 240000,
        protocolTimeout: 600000,
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
            '--disable-accelerated-2d-canvas',
            '--no-first-run',
            '--no-zygote',
            '--disable-gpu',
            '--disable-extensions',
            '--disable-component-update',
            '--disable-default-apps',
            '--no-default-browser-check',
            '--unhandled-rejections=strict'
        ]
    }
});

function withTimeout(promise, ms, label) {
    let timer;
    const timeout = new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error(label + ' timed out after ' + (ms / 1000) + 's')), ms);
    });
    return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

let mediaQueue = Promise.resolve();
function queueMedia(task) {
    const run = mediaQueue.then(task);
    mediaQueue = run.catch(() => {});
    return run;
}
let mediaBrokenUntil = 0; 

function processEmojisAndText(text, version) {
    if (!text) return "";
    const isAndroid41OrHigher = version && parseFloat(version) >= 4.1;

    if (!isAndroid41OrHigher) {
        const emojiMap = {
            '😂': ':-D', '😀': ':-)', '😁': ':-D', '🙂': ':-)', '😊': ':-)',
            '😉': ';-)', '😍': '<3', '🥰': '<3', '😘': ':-*', '❤️': '<3',
            '💕': '<3', '😭': ':-(', '😢': ':-(', '☹️': ':-(', '👍': '(Y)',
            '👎': '(N)', '😮': ':-O', '😲': ':-O', '🤔': ':-/', '😐': ':-|',
            '😛': ':-P', '😜': ';-P', '😋': ':-P', '😡': ':-@',
            '😎': 'B-)', '🤑': ':-$', '😵': 'x-(', '🤐': ':-!', '😶': ':-X',
            '🤠': 'o:-)', '🤨': 'o_O'
        };

        for (const [emoji, replacement] of Object.entries(emojiMap)) {
            text = text.replaceAll(emoji, replacement);
        }
    }
    const modernEmojiRegex = /[\u{1F300}-\u{1F9FF}]|[\u{1F600}-\u{1F64F}]|[\u{1F680}-\u{1F6FF}]|[\u{2600}-\u{27BF}]|[\u{1F000}-\u{1FBF9}]/gu;
    if (!isAndroid41OrHigher) {
        text = text.replace(modernEmojiRegex, "∅");
        text = text.replace(/[^\x00-\x7F\u0080-\u00FF\u0100-\u017F\u0180-\u024F\u1E00-\u1EFF]/g, "∅");
    }

    if (text.trim() === "" || /^∅+$/.test(text.trim())) {
        return "";
    }

    return text;
}

client.on('ready', () => {
    console.log('WhatsApp Client is ready!');
    isConnected = true;
});

client.on('authenticated', () => {
    console.log('Authenticated successfully');
});

client.on('auth_failure', () => {
    console.error('Authentication failed');
    isConnected = false;
});

client.on('disconnected', (reason) => {
    console.log('Client disconnected, sebep:', reason);
    isConnected = false;
    setTimeout(() => {
        console.log('Trying to connect...');
        client.initialize().catch(err => console.log('Re-init error:', err));
    }, 5000);
});

client.on('change_state', (state) => {
    console.log('WhatsApp state changed:', state);
});

client.on('message', () => {
    console.log('Incoming message event received');
});

app.get('/debug', async (req, res) => {
    const out = { isConnected: isConnected };
    try {
        out.state = await withTimeout(client.getState(), 10000, 'getState');
    } catch (e) {
        out.state = 'ERROR: ' + e.message;
    }
    try {
        out.waWebVersion = await withTimeout(client.getWWebVersion(), 10000, 'getWWebVersion');
    } catch (e) {
        out.waWebVersion = 'ERROR: ' + e.message;
    }
    res.json(out);
});

app.get('/status', (req, res) => {
    res.json({ success: true, loggedIn: isConnected });
});

app.get('/get-code', async (req, res) => {
    const phoneNumber = req.query.phone;
    if (!phoneNumber) {
        return res.json({ success: false, error: "Phone number required" });
    }

    try {
        if (isConnected) {
            return res.json({ success: true, pairingCode: "CONNECTED" });
        }

        const code = await client.requestPairingCode(phoneNumber);
        res.json({ success: true, pairingCode: code });
    } catch (err) {
        res.json({ success: false, error: err.message });
    }
});

app.get('/get-profile-pic', async (req, res) => {
    try {
        const id = req.query.id;
        if (!id) return res.status(400).send("ID required");

        const url = await client.getProfilePicUrl(id);
        if (url) {
            return res.json({ success: true, url: url });
        } else {
            return res.json({ success: false, url: null });
        }
    } catch (err) {
        res.json({ success: false, error: err.message });
    }
});

const picCache = new Map();
const PIC_TTL = 60 * 60 * 1000; 

function downloadBuffer(url, redirects = 3) {
    return new Promise((resolve, reject) => {
        https.get(url, (r) => {
            if (r.statusCode >= 300 && r.statusCode < 400 && r.headers.location && redirects > 0) {
                r.resume();
                return resolve(downloadBuffer(r.headers.location, redirects - 1));
            }
            if (r.statusCode !== 200) {
                r.resume();
                return reject(new Error('HTTP ' + r.statusCode));
            }
            const chunks = [];
            r.on('data', (c) => chunks.push(c));
            r.on('end', () => resolve({
                buffer: Buffer.concat(chunks),
                type: r.headers['content-type'] || 'image/jpeg'
            }));
            r.on('error', reject);
        }).on('error', reject);
    });
}

app.get('/profile-pic-image', async (req, res) => {
    const id = req.query.id;
    if (!id) return res.status(400).send('ID required');
    if (!isConnected) return res.status(503).send('Client not connected');

    try {
        const cached = picCache.get(id);
        if (cached && Date.now() - cached.time < PIC_TTL) {
            if (!cached.buffer) return res.status(404).send('No profile picture');
            res.set('Content-Type', cached.type);
            res.set('Content-Length', cached.buffer.length);
            return res.send(cached.buffer);
        }

        const url = await client.getProfilePicUrl(id);
        if (!url) {
            picCache.set(id, { time: Date.now(), buffer: null });
            return res.status(404).send('No profile picture');
        }

        const { buffer, type } = await downloadBuffer(url);
        picCache.set(id, { time: Date.now(), buffer, type });

        res.set('Content-Type', type);
        res.set('Content-Length', buffer.length);
        res.send(buffer);
    } catch (err) {
        console.error('Profile pic error:', err);
        res.status(404).send('Profile picture error: ' + err.message);
    }
});

app.get('/get-contacts', async (req, res) => {
    try {
        if (!isConnected) {
            return res.json({ success: false, error: "Client not connected" });
        }

        const version = req.query.v;
        const contacts = await withTimeout(client.getContacts(), 60000, 'getContacts');

        const list = [];
        for (const c of contacts) {
            if (!c || !c.id || c.isMe || c.isGroup || !c.isMyContact) continue;
            if (c.isWAContact === false) continue;
            if (c.id.server !== 'c.us' && c.id.server !== 'lid') continue;

            const rawName = c.name || c.pushname || '';
            if (!rawName) continue;

            let name = processEmojisAndText(rawName, version);
            if (!name || !name.trim()) name = c.number ? '+' + c.number : '';
            if (!name) continue;

            list.push({
                id: c.id._serialized,
                name: name.trim(),
                number: c.number || ''
            });
        }

        res.json({ success: true, contacts: list });
    } catch (err) {
        console.error('Get contacts error:', err.message);
        res.json({ success: false, error: err.message });
    }
});

app.get('/get-chats', async (req, res) => {
    try {
        if (!isConnected) {
            return res.json({ success: false, error: "Client not connected" });
        }

        const version = req.query.v;
        const chats = await client.getChats();
        const formattedChats = chats.slice(0, 20).map(chat => {
            let lastMsgText = chat.lastMessage ? chat.lastMessage.body : "";
            if (chat.lastMessage && chat.lastMessage.hasMedia) {
                if (chat.lastMessage.isGif) {
                    lastMsgText = "[GIF]";
                } else {
                    switch (chat.lastMessage.type) {
                        case 'image': lastMsgText = "[Photo]"; break;
                        case 'sticker': lastMsgText = "[Sticker]"; break;
                        case 'video': lastMsgText = "[Video]"; break;
                        case 'audio':
                        case 'ptt': lastMsgText = "[Audio]"; break;
                        default: lastMsgText = "[Media]";
                    }
                }
            }
            lastMsgText = processEmojisAndText(lastMsgText, version);

            return {
                id: chat.id._serialized,
                name: chat.name || "Unknown",
                lastMessage: lastMsgText || "[Message]"
            };
        });

        res.json({ success: true, chats: formattedChats });
    } catch (err) {
        res.json({ success: false, error: err.message });
    }
});

app.get('/get-messages', async (req, res) => {
    try {
        const chatId = req.query.id;
        const version = req.query.v;

        if (!chatId) {
            return res.json({ success: false, error: "Chat ID required" });
        }

        const chat = await client.getChatById(chatId);
        let limit = parseInt(req.query.limit, 10);
        if (isNaN(limit) || limit < 1) limit = 25;
        if (limit > 1000) limit = 1000;
        const messages = await withTimeout(chat.fetchMessages({ limit }), 80000, 'fetchMessages');

        const formattedMessages = [];

        for (const msg of messages) {
            let bodyText = msg.body || "";
            let mediaType = "none";

            if (msg.hasMedia) {
                if (msg.isGif || msg.type === 'gif') {
                    mediaType = 'gif';
                    bodyText = "";
                } else {
                    switch (msg.type) {
                        case 'image':
                            mediaType = 'image';
                            bodyText = msg.body ? msg.body : "";
                            break;
                        case 'sticker':
                            mediaType = 'sticker';
                            bodyText = "";
                            break;
                        case 'video':
                            mediaType = 'video';
                            bodyText = msg.body ? msg.body : ""; 
                            break;
                        case 'audio':
                        case 'ptt':
                            mediaType = 'audio';
                            bodyText = "[Ses Kaydı]";
                            break;
                        case 'document':
                            mediaType = 'document';
                            bodyText = msg.body ? msg.body : "[Document]";
                            break;
                        default:
                            bodyText = "";
                    }
                }
            } else {
                switch (msg.type) {
                    case 'location': bodyText = "[Konum]"; break;
                    case 'vcard': bodyText = "[Kişi Kartı]"; break;
                    case 'revoked': bodyText = "Bu mesaj silindi"; break;
                }
            }

            bodyText = processEmojisAndText(bodyText, version);

            if (chatId.endsWith('@g.us') && !msg.fromMe) {
                try {
                    const contact = await msg.getContact();
                    const senderName = contact.name || contact.pushname || msg.author.split('@')[0];
                    bodyText = bodyText ? `~ ${senderName} ~\n${bodyText}` : `~ ${senderName} ~`;
                } catch (e) {
                    const fallbackNumber = msg.author ? msg.author.split('@')[0] : "Bilinmeyen";
                    bodyText = bodyText ? `~ ${fallbackNumber} ~\n${bodyText}` : `~ ${fallbackNumber} ~`;
                }
            }

            formattedMessages.push({
                id: msg.id._serialized,
                body: bodyText,
                fromMe: msg.fromMe,
                timestamp: msg.timestamp,
                ack: typeof msg.ack === 'number' ? msg.ack : 0,
                mediaType: mediaType
            });
        }

        res.json({ success: true, messages: formattedMessages });

    } catch (error) {
        res.json({ success: false, error: error.message });
    }
});

const MEDIA_DIR = path.join(__dirname, 'media-cache');
if (!fs.existsSync(MEDIA_DIR)) fs.mkdirSync(MEDIA_DIR, { recursive: true });

let ffmpegAvailable = false;
execFile('ffmpeg', ['-version'], { timeout: 10000 }, (err) => {
    ffmpegAvailable = !err;
    console.log(ffmpegAvailable
        ? 'ffmpeg found: video/audio converter for your server'
        : 'ffmpeg isnt available in your device: audio and video will be sent without converting(Videos and audios may show blank).');
});

function cleanMediaCache() {
    fs.readdir(MEDIA_DIR, (err, files) => {
        if (err) return;
        const limit = Date.now() - 24 * 3600 * 1000;
        files.forEach(f => {
            const p = path.join(MEDIA_DIR, f);
            fs.stat(p, (e, st) => { if (!e && st.mtimeMs < limit) fs.unlink(p, () => {}); });
        });
    });
}
cleanMediaCache();
setInterval(cleanMediaCache, 3600 * 1000);

const MIME_EXT = {
    'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp', 'image/gif': '.gif',
    'audio/ogg': '.ogg', 'audio/mpeg': '.mp3', 'audio/mp4': '.m4a', 'audio/aac': '.aac',
    'audio/amr': '.amr', 'video/mp4': '.mp4', 'video/3gpp': '.3gp', 'video/webm': '.webm'
};

function runFfmpeg(args, timeoutMs) {
    return new Promise((resolve, reject) => {
        execFile('ffmpeg', args, { timeout: timeoutMs, maxBuffer: 10 * 1024 * 1024 }, (err, stdout, stderr) => {
            if (err) {
                const tail = String(stderr || '').trim().split('\n').slice(-3).join(' | ');
                return reject(new Error('ffmpeg failed: ' + (tail || err.message)));
            }
            resolve();
        });
    });
}

function needsCompat(version) {
    const v = parseFloat(version);
    return !isNaN(v) && v < 5.0;
}

const mediaInflight = new Map();

function prepareMedia(msgId, compat, version) {
    const vNum = parseFloat(version);
    const tier = (!isNaN(vNum) && vNum < 4.0) ? 'low' : 'mid';
    const P = tier === 'low'
        ? { aBitrate: '32k', aRate: '22050', vWidth: 320, vFps: '15', vCrf: '30', vMax: '250k', vBuf: '500k', vABitrate: '32k', vARate: '22050' }
        : { aBitrate: '64k', aRate: '44100', vWidth: 480, vFps: '24', vCrf: '28', vMax: '600k', vBuf: '1200k', vABitrate: '64k', vARate: '44100' };
    const key = crypto.createHash('sha1').update(msgId + (compat ? (ffmpegAvailable ? '|cf|' + tier : '|c') : '|o')).digest('hex');
    if (mediaInflight.has(key)) return mediaInflight.get(key);

    const job = (async () => {
        const existing = fs.readdirSync(MEDIA_DIR).find(f => f.startsWith(key + '.') && !f.endsWith('.part'));
        if (existing) {
            const ext = path.extname(existing);
            const mime = Object.keys(MIME_EXT).find(m => MIME_EXT[m] === ext) || 'application/octet-stream';
            return { file: path.join(MEDIA_DIR, existing), mime };
        }

        if (Date.now() < mediaBrokenUntil) throw new Error('Media temporarily unavailable');

        const result = await queueMedia(async () => {
            const msg = await withTimeout(client.getMessageById(msgId), 60000, 'getMessageById');
            if (!msg || !msg.hasMedia) return { notFound: true };
            return { media: await withTimeout(msg.downloadMedia(), 240000, 'downloadMedia') };
        });
        if (result.notFound) return { notFound: true };
        if (!result.media || !result.media.data) throw new Error('Media download failed');

        const baseMime = (result.media.mimetype || 'application/octet-stream').split(';')[0].trim().toLowerCase();
        const buf = Buffer.from(result.media.data, 'base64');
        const origExt = MIME_EXT[baseMime] || '.bin';

        const isAudio = baseMime.startsWith('audio/');
        const isVideo = baseMime.startsWith('video/');

        if (compat && ffmpegAvailable && (isAudio || isVideo)) {
            const src = path.join(MEDIA_DIR, key + '.src' + origExt + '.part');
            fs.writeFileSync(src, buf);
            let outExt = isAudio ? '.mp3' : '.mp4';
            let outMime = isAudio ? 'audio/mpeg' : 'video/mp4';
            let outPart = path.join(MEDIA_DIR, key + outExt + '.part');
            try {
                if (isAudio) {
                    try {
                        await runFfmpeg(['-y', '-i', src, '-vn', '-c:a', 'libmp3lame', '-b:a', P.aBitrate, '-ac', '1',
                            '-ar', P.aRate, '-f', 'mp3', outPart], 120000);
                    } catch (mp3Err) {
                        console.error('MP3 encode failed, trying AAC:', mp3Err.message);
                        try { fs.unlinkSync(outPart); } catch (_) {}
                        outExt = '.m4a';
                        outMime = 'audio/mp4';
                        outPart = path.join(MEDIA_DIR, key + outExt + '.part');
                        await runFfmpeg(['-y', '-i', src, '-vn', '-c:a', 'aac', '-b:a', P.aBitrate, '-ac', '1',
                            '-ar', P.aRate, '-movflags', '+faststart', '-f', 'mp4', outPart], 120000);
                    }
                } else {
                    await runFfmpeg(['-y', '-i', src,
                        '-vf', "scale='min(" + P.vWidth + ",iw)':-2", '-r', P.vFps, '-pix_fmt', 'yuv420p',
                        '-c:v', 'libx264', '-profile:v', 'baseline', '-level', '3.0',
                        '-preset', 'ultrafast', '-crf', P.vCrf, '-maxrate', P.vMax, '-bufsize', P.vBuf,
                        '-c:a', 'aac', '-b:a', P.vABitrate, '-ac', '1', '-ar', P.vARate,
                        '-movflags', '+faststart', '-f', 'mp4', outPart], 600000);
                }
                const finalPath = path.join(MEDIA_DIR, key + outExt);
                fs.renameSync(outPart, finalPath);
                console.log('Media converted for old Android (' + tier + '): ' + baseMime + ' -> ' + outMime);
                return { file: finalPath, mime: outMime };
            } catch (e) {
                console.error('Transcode error (sending original):', e.message);
                try { fs.unlinkSync(outPart); } catch (_) {}
            } finally {
                try { fs.unlinkSync(src); } catch (_) {}
            }
        } else if ((isAudio || isVideo) && compat) {
            console.log('Media NOT converted (' + baseMime + '): ffmpeg is not installed on the server, so old phones may not play it.');
        } else if (isAudio || isVideo) {
            console.log('Media sent as-is (' + baseMime + '): client Android version is 5+ or unknown (v=' + String(version) + ').');
        }

        const finalPath = path.join(MEDIA_DIR, key + origExt);
        const tmp = finalPath + '.part';
        fs.writeFileSync(tmp, buf);
        fs.renameSync(tmp, finalPath);
        return { file: finalPath, mime: baseMime };
    })();

    mediaInflight.set(key, job);
    job.then(() => mediaInflight.delete(key), () => mediaInflight.delete(key));
    return job;
}

app.get('/get-media', async (req, res) => {
    try {
        const msgId = req.query.msgId;
        if (!msgId) {
            return res.status(400).send("Message ID required");
        }

        const compat = needsCompat(req.query.v);

        const cached = await prepareMedia(msgId, compat, req.query.v);

        if (cached.notFound) {
            return res.status(404).send("Media not found");
        }

        if (req.query.prepare) {
            return res.send('ok');
        }

        res.sendFile(cached.file, { headers: { 'Content-Type': cached.mime } }, (err) => {
            if (err && !res.headersSent) res.status(500).send('Send failed');
        });

    } catch (err) {
        console.error("Get media error:", err.message);
        if (/timed out/i.test(err.message)) {
            mediaBrokenUntil = Date.now() + 15000;
        }
        if (!res.headersSent) res.status(500).send("Error fetching media: " + err.message);
    }
});

app.get('/send-message', async (req, res) => {
    try {
        const chatId = req.query.id;
        let messageText = req.query.text;
        const version = req.query.v;

        if (!chatId || !messageText) {
            return res.json({ success: false, error: "Chat ID and text required" });
        }

        if (!isConnected) {
            return res.json({ success: false, error: "Client not connected" });
        }

        if (!(version && parseFloat(version) >= 4.1)) {
            const gidenEmojiMap = {
                ':-D': '😂', ':-)': '🙂', ';-)': '😉', '<3': '❤️',
                ':-*': '😘', ':-(': '😭', ':-P': '😛', ';-P': '😜',
                ':-O': '😮', ':-/': '🤔', ':-|': '😐', ':-@': '😡',
                '(Y)': '👍', '(N)': '👎',
                'B-)': '😎', ':-$': '🤑', 'x-(': '😵', ':-!': '🤐',
                ':-X': '😶', 'o:-)': '🤠', 'o_O': '🤨'
            };

            const escapeRegExp = (string) => string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

            for (const [textEmoticon, emoji] of Object.entries(gidenEmojiMap)) {
                const regex = new RegExp(escapeRegExp(textEmoticon), 'g');
                messageText = messageText.replace(regex, emoji);
            }
        }

        await withTimeout(client.sendMessage(chatId, messageText, { sendSeen: false }), 30000, 'sendMessage');
        res.json({ success: true });

    } catch (error) {
        console.error('Send message error:', error);
        if (!res.headersSent) {
            res.json({ success: false, error: error.message });
        }
    }
});

app.post('/send-file', upload.single('file'), async (req, res) => {
    let filePath = null;
    try {
        const chatId = req.body.id;
        const file = req.file;

        if (!chatId || !file) {
            return res.json({ success: false, error: "Chat ID and file required" });
        }

        filePath = path.resolve(file.path);
        const media = MessageMedia.fromFilePath(filePath);

        await client.sendMessage(chatId, media, { sendMediaAsDocument: true });

        if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
        }

        res.json({ success: true });
    } catch (err) {
        if (filePath && fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
        }
        res.json({ success: false, error: err.message });
    }
});

app.get('/screenshot', async (req, res) => {
    try {
        if (!client.pupBrowser) {
            return res.status(400).send("Browser hasn't been started yet.");
        }

        const pages = await client.pupBrowser.pages();
        if (pages.length === 0) {
            return res.status(400).send("No open browser page was found.");
        }

        const activePage = pages[0];
        const screenshotBuffer = await activePage.screenshot({ type: 'png' });

        res.set('Content-Type', 'image/png');
        res.send(screenshotBuffer);
    } catch (err) {
        res.status(500).send("Ekran görüntüsü alınırken hata oluştu: " + err.message);
    }
});

client.initialize().catch(err => {
    console.log('Puppeteer init error:', err);
});

app.listen(port, () => {
    console.log(`WPL Server running on port ${port}`);
});