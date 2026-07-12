# Server Setup (Termux)

Follow these exact steps to deploy the server architecture on Termux.

---

## 📋 Step 1: Initial Repository & Mirror Setup
First, make sure you have a fresh environment. Open Termux and execute:

```bash
termux-change-repo
```
Instructions: Select multiple-mirrors + all mirrors option. Consequently, pressing the Enter key twice in succession will be sufficient to complete this stage.
Next, add the user repository required for the Chromium build:
```
pkg install tur-repo -y
pkg update -y
```
Download the graphical environment repository as well:
```
pkg install x11-repo -y
pkg update -y
```
⚙️ Step 2: Runtime Dependencies Installation
Update the index configurations and install Node.js alongside NPM:
```
pkg update
pkg install nodejs npm
```
Troubleshooting: In case the command fails or hangs, repeat it 2 to 3 times. Errors might occasionally occur depending on network states.

🌐 Step 3: Chromium Installation
Deploy the core browser component for the headless process:
```
pkg install chromium -y
```
Troubleshooting: If this command encounters an execution block, restart (close and reopen) your current Termux session and execute it again.

Create Browser Shortcut Path
```
ln -s $PREFIX/bin/chromium-browser $PREFIX/bin/chromium
```

📁 Step 4: Server Workspace Allocation
Create the deployment directory and navigate inside it:
```
mkdir wpl-server && cd wpl-server
```
Initialize the main runtime registry configuration file:
```
npm init -y
```

Critical Environment Fixes
If anything goes wrong during initialization, or as a highly recommended preventative measure even if no errors show up, execute the following block:
```
pkg upgrade -y
pkg install openssl libandroid-support nodejs-lts -y
```

Instructions: During this execution, you will be prompted with two system queries. Press Enter for both prompts to pass through using defaults.
📦 Step 5: Module Dependencies Implementation
Download the direct operational packages skipping standard standalone binary downloads to save system memory constraints:
```
PUPPETEER_SKIP_DOWNLOAD=1 npm install whatsapp-web.js express puppeteer-core --no-optional
```

Approve the package binary installation execution scripts:
```
npm install-scripts approve <pkg>
```

💾 Step 6: Code Implementation & Initial Launch
Create the primary application entry point file:
```
nano server.js
```

Actions: Inside this file,Copy and Paste these codes:
```
const { Client, LocalAuth } = require('whatsapp-web.js');
const express = require('express');
const fs = require('fs');

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
    puppeteer: {
        headless: true,
        handleSIGINT: false,
        executablePath: chromePath,
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
            '--disable-accelerated-2d-canvas',
            '--no-first-run',
            '--no-zygote',
            '--single-process',
            '--disable-gpu'
        ]
    }
});

function processEmojisAndText(text, version) {
    if (!text) return "...";
    const isAndroid41OrHigher = version && parseFloat(version) >= 4.1;

    if (!isAndroid41OrHigher) {
        const emojiMap = {
            '😂': ':-D', '😀': ':-)', '😁': ':-D', '🙂': ':-)', '😊': ':-)',
            '😉': ';-)', '😍': '<3', '🥰': '<3', '😘': ':-*', '❤️': '<3',
            '💕': '<3', '😭': ':-(', '😢': ':-(', '☹️': ':-(', '👍': '(Y)',
            '👎': '(N)', '😮': ':-O', '😲': ':-O', '🤔': ':-/', '😐': ':-|',
            '😛': ':-P', '😜': ';-P', '😋': ':-P', '😡': ':-@'
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
        return "∅";
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

client.on('disconnected', () => {
    console.log('Client disconnected');
    isConnected = false;
});

app.get('/screenshot', async (req, res) => {
    try {
        if (!client.pupBrowser) {
            return res.status(400).send("Browser henüz başlatılmadı.");
        }

        const pages = await client.pupBrowser.pages();
        if (pages.length === 0) {
            return res.status(400).send("Açık tarayıcı sayfası bulunamadı.");
        }

        const activePage = pages[0];
        const screenshotBuffer = await activePage.screenshot({ type: 'png' });

        res.set('Content-Type', 'image/png');
        res.send(screenshotBuffer);
    } catch (err) {
        res.status(500).send("Ekran görüntüsü alınırken hata oluştu: " + err.message);
    }
});

client.initialize()
    .then(() => {
        if (client.pupBrowser) {
            client.pupBrowser.pages().then(pages => {
                if (pages.length > 0) {
                    pages[0].on('console', msg => console.log('BROWSER LOG:', msg.text()));
                }
            }).catch(() => {});
        }
    })
    .catch(err => {
        console.log('Puppeteer init hatası:', err);
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
                lastMsgText = "[Media]";
            }
            lastMsgText = processEmojisAndText(lastMsgText, version);

            return {
                id: chat.id._serialized,
                name: chat.name || "Unknown",
                lastMessage: lastMsgText
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
            let bodyText = msg.body;

            if (msg.hasMedia) {
                switch (msg.type) {
                    case 'image': bodyText = "[Photo]"; break;
                    case 'video': bodyText = "[Video]"; break;
                    case 'audio':
                    case 'ptt': bodyText = "[Recording]"; break;
                    case 'document': bodyText = "[Document]"; break;
                    case 'sticker': bodyText = "[Sticker]"; break;
                    default: bodyText = "[Media]";
                }
            } else {
                switch (msg.type) {
                    case 'location': bodyText = "[Location]"; break;
                    case 'vcard': bodyText = "[Contact]"; break;
                    case 'revoked': bodyText = "This message was deleted"; break;
                }
            }

            bodyText = processEmojisAndText(bodyText, version);

            if (chatId.endsWith('@g.us') && !msg.fromMe) {
                try {
                    const contact = await msg.getContact();
                    const senderName = contact.name || contact.pushname || msg.author.split('@')[0];
                    bodyText = `~ ${senderName} ~\n${bodyText}`;
                } catch (e) {
                    const fallbackNumber = msg.author ? msg.author.split('@')[0] : "Bilinmeyen";
                    bodyText = `~ ${fallbackNumber} ~\n${bodyText}`;
                }
            }

            formattedMessages.push({
                id: msg.id._serialized,
                body: bodyText,
                fromMe: msg.fromMe,
                timestamp: msg.timestamp
            });
        }

        res.json({ success: true, messages: formattedMessages });

    } catch (error) {
        res.json({ success: false, error: error.message });
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
        res.json({ success: true });

        if (!(version && parseFloat(version) >= 4.1)) {
            const gidenEmojiMap = {
                ':-D': '😂', ':-)': '🙂', ';-)': '😉', '<3': '❤️',
                ':-*': '😘', ':-(': '😭', ':-P': '😛', ';-P': '😜',
                ':-O': '😮', ':-/': '🤔', ':-|': '😐', ':-@': '😡',
                '(Y)': '👍', '(N)': '👎'
            };

            const escapeRegExp = (string) => string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

            for (const [textEmoticon, emoji] of Object.entries(gidenEmojiMap)) {
                const regex = new RegExp(escapeRegExp(textEmoticon), 'g');
                messageText = messageText.replace(regex, emoji);
            }
        }
        await client.sendMessage(chatId, messageText);

    } catch (error) {
        if (!res.headersSent) {
            res.json({ success: false, error: error.message });
        }
    }
});

app.listen(port, () => {
    console.log(`WPL Server running on port ${port}`);
});

```

Save Controls: Press Ctrl + X, followed by Y, and then press Enter to write files securely onto the directory workspace.
Initialize and test the primary engine operation within your home Wi-Fi local zone layout:
```
node server.js
```

The server is currently operational under your local Wi-Fi domain network configuration. Keep this current active shell instance session open, and establish a new second Termux session to upgrade global access.

🚀 Step 7: Global Traffic Routing (Serveo Deployment)
Inside the newly opened second active session terminal window, install the required secure access management utilities:
```
pkg install dropbear
pkg install openssh
```
Option A: Dynamic Dynamic Link Allocation (Not Recommended)
You can test out immediate proxy forwarding endpoints using the standard line:
```
ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=5 -R 80:localhost:3000 serveo.net
```
Disadvantage: The provided link domain path maps to completely randomized characters and shifts on every script refresh cycle. Therefore, it is not recommended for production setups.*

Option B: Persistent Reserved Address Configuration (Optional but Highly Recommended)
Follow these sub-steps if you strictly desire a solid, stationary domain host URL routing link:

1.If you previously launched a standard Serveo tunnel process, shut it down inside the terminal window layout using the Ctrl + C keys shortcut combo.

2.Execute the public/private cryptography key-pair generator script:
```
ssh-keygen -t ed25519 -C "your_email@example.com"
```
(Be sure to replace the placeholder parameter line text with your actual email address structure).

The script string returns a long, descriptive SHA-256 secure hash fingerprint signature link output block. Copy this text sequence, and paste it onto your Google search bar field to navigate to the provider dashboard.

The site yields a corresponding distinct public security key file output structure. Paste that token into the explicit SHA-256 field form interface workspace shown on the screen panel.

Create a registered account profile inside that cloud terminal web interface, and configure your absolute custom host domain label name. (For instance: let's assign the namespace flag to queen).

🔒 Step 8: Permanent Tunnel Loop Initiation
Once your administrative configurations are saved on the portal, return to the second Termux session panel view (where the core main node server is NOT executing). Since you need to boot up both the server engine in the first window and the persistent SSH bridge pipeline loop inside this second terminal, you will rely on this command sequence continuously:
```
while true; do ssh -o ServerAliveInterval=30 -R queen:80:localhost:3000 serveo.net; echo "Connection lost, automatically reconnecting to "queen" link in 3 seconds..."; sleep 3; done
```
**Quick Note:** Guys İf you have any other nickname than queen for your ssh key change change the queen part with that nickname

The tunnel server protocol establishes communications handshake checks and assigns your global network address link string payload:
Forwarding HTTP traffic from _https://queen.serveo.net__

📲 Step 9: Legacy System Client Authentication
To wire up your newly generated backend platform server node instance with your target application stack configs:
Navigate to your application dashboard forms, find the targeted IP:Port network variables configurations, and map the value straight to queen.serveo.net (or whatever custom domain token text string your profile generated).
Crucial Rule: Ensure you provide only the pure domain path link WITHOUT appending the https:// secure protocol headers text onto the front element box wrapper.
Finalize the synchronization steps by entering your authentic active target phone number parameter fields.
And voila! Your automated WhatsApp system interface runs perfectly in production deployment mode!


