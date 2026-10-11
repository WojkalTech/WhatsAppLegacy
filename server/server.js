const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const express = require('express');
const fs = require('fs');
const path = require('path');
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
    puppeteer: {
        headless: true,
        handleSIGINT: false,
        executablePath: chromePath,
        timeout: 120000,
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
        text = text.replace(/[^\x00-\x7F\u0100-\u017F\u0180-\u024F\u1E00-\u1EFF]/g, "∅");
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
        console.log('Yeniden bağlanılmaya çalışılıyor...');
        client.initialize().catch(err => console.log('Re-init error:', err));
    }, 5000);
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
        const messages = await chat.fetchMessages({ limit: 25 });

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
                mediaType: mediaType
            });
        }

        res.json({ success: true, messages: formattedMessages });

    } catch (error) {
        res.json({ success: false, error: error.message });
    }
});

app.get('/get-media', async (req, res) => {
    try {
        const msgId = req.query.msgId;
        if (!msgId) {
            return res.status(400).send("Message ID required");
        }

        const msg = await client.getMessageById(msgId);
        if (!msg || !msg.hasMedia) {
            return res.status(404).send("Media not found");
        }

        const media = await msg.downloadMedia();
        if (!media || !media.data) {
            return res.status(500).send("Media download failed");
        }

        const imgBuffer = Buffer.from(media.data, 'base64');
        const mimeType = media.mimetype || 'application/octet-stream';

        res.set('Content-Type', mimeType);
        res.set('Content-Length', imgBuffer.length);
        res.send(imgBuffer);

    } catch (err) {
        console.error("Get media error:", err);
        res.status(500).send("Error fetching media: " + err.message);
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

        await client.sendMessage(chatId, messageText);
        res.json({ success: true });

    } catch (error) {
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