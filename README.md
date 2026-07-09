# WhatsAppLegacy
A WhatsApp webview client for Android 2.3+ that requires a self-hosted Node.js server to function.

## Features

* **Text Messaging:** Fully supports sending and receiving plain text messages.
* **Media Placeholders:** Detects and displays specific media types and unsupported formatting as plain text labels:
  * Photos are displayed as `[photo]`
  * Voice notes are displayed as `[recording]`
  * Unsupported characters or emoticons are displayed as `[unknown character]`
* **Chat History:** Ability to load and view past conversations and previous messages.
* **Group Chats:** Full support for retrieving and displaying group chat sessions.

## 🛠️ Server Setup (Manual Way)

You can manually create the server file on your Linux/macOS machine by following these simple steps:

1. **Prerequisites:** Ensure you have Node.js and npm installed on your system.
you can install them by opening terminal and writing the command:
 ```bash
   sudo apt install nodejs npm
```
2. **Create the Server Directory:** Open your terminal and create a dedicated folder in your home directory:
   ```bash
   mkdir ~/wpl-server
3.Navigate to the Directory:
```bash
cd ~/wpl-server
```
4.Install the Required Modules: You need the WhatsApp library and Express framework. Run this command inside the folder:
```bash
npm install whatsapp-web.js express qrcode-terminal
```
5 Create and Paste the Server Code: Open the Nano text editor to create your server file:
```bash
nano server.js
```
And then copy paste all this code to your server.js
```
const { Client, LocalAuth } = require('whatsapp-web.js');
const express = require('express');

const app = express();
const port = process.env.PORT || 3000;

let isConnected = false;

const client = new Client({
    authStrategy: new LocalAuth(),
    puppeteer: {
        headless: false,
        handleSIGINT: false,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    }
});

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

client.initialize();

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

        const chats = await client.getChats();
        const formattedChats = chats.slice(0, 20).map(chat => {
            let lastMsgText = chat.lastMessage ? chat.lastMessage.body : "";
            if (chat.lastMessage && chat.lastMessage.hasMedia) {
                lastMsgText = "[Media]";
            }

            if (lastMsgText) {
                lastMsgText = lastMsgText.replace(/[^\x00-\x7F\u0100-\u017F\u0180-\u024F\u1E00-\u1EFF]/g, "");
            }

            return {
                id: chat.id._serialized,
                name: chat.name || "Unknown",
                lastMessage: lastMsgText || ""
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
        if (!chatId) {
            return res.json({ success: false, error: "Chat ID required" });
        }

        const chat = await client.getChatById(chatId);
        const messages = await chat.fetchMessages({ limit: 25 });

        const formattedMessages = messages.map(msg => {
            let bodyText = msg.body;

            if (msg.hasMedia) {
                switch (msg.type) {
                    case 'image':
                        bodyText = "[Photo]";
                        break;
                    case 'video':
                        bodyText = "[Video]";
                        break;
                    case 'audio':
                    case 'ptt':
                        bodyText = "[Recording]";
                        break;
                    case 'document':
                        bodyText = "[Document]";
                        break;
                    case 'sticker':
                        bodyText = "[Sticker]";
                        break;
                    default:
                        bodyText = "[Media]";
                }
            } else {
                switch (msg.type) {
                    case 'location':
                        bodyText = "[Location]";
                        break;
                    case 'vcard':
                        bodyText = "[Contact]";
                        break;
                    case 'revoked':
                        bodyText = "This message was deleted";
                        break;
                }
            }

            if (bodyText) {
                bodyText = bodyText.replace(/[^\x00-\x7F\u0100-\u017F\u0180-\u024F\u1E00-\u1EFF]/g, "");
                if (bodyText.trim() === "") {
                    bodyText = "[Unsupported Character]";
                }
            } else {
                bodyText = "...";
            }

            return {
                id: msg.id._serialized,
                body: bodyText,
                fromMe: msg.fromMe,
                timestamp: msg.timestamp
            };
        });

        res.json({ success: true, messages: formattedMessages });

    } catch (error) {
        res.json({ success: false, error: error.message });
    }
});

app.get('/send-message', async (req, res) => {
    try {
        const chatId = req.query.id;
        const messageText = req.query.text;

        if (!chatId || !messageText) {
            return res.json({ success: false, error: "Chat ID and text required" });
        }

        if (!isConnected) {
            return res.json({ success: false, error: "Client not connected" });
        }

        await client.sendMessage(chatId, messageText);
        res.json({ success: true });

    } catch (error) {
        res.json({ success: false, error: error.message });
    }
});

app.listen(port, () => {
    console.log(`WPL Server running on port ${port}`);
});
```
After that press CTRL+X,Pick Y,Press Enter
6.Start the Server: You can start your server in two ways:

*Default Port(3000)by writing the command
```
node server.js
```
*Custom Port by writing the command
```
PORT=ANY-PORT-YOU-WANT node server.js
```

### Method 1: Port Forwarding (Direct Public IP - Best Performance)
*Recommended if you have a Static IP and your internet service provider does not put you behind a CGNAT pool.*

If you want to access your server from outside your home network (e.g., using mobile data), you need to open a port on your router.

1. **Find your Local IP:** Open your terminal and check your local IP address (e.g., `192.168.1.128`) by running `hostname -I` or `ifconfig`.
2. **Log into Your Router:** Open your web browser, go to your router's gateway (usually `192.168.1.1 or 192.168.1.254`), and log in.
3. **Navigate to Port Forwarding:** Find the **Port Forwarding / NAT Forwarding / Port Mapping** section (usually under Advanced Settings or Security).
4. **Create a New Rule:** Add a new rule with the following details:
   - **Service Name:** `WPL-Server`
   - **Protocol:** `TCP` (or TCP/UDP)
   - **External Port:** The exact port you used to start your server (e.g., `12345`).
   - **Internal Port:** Same as the external port (e.g., `12345`).
   - **Internal/Local IP:** Your computer's local IP address that you found in Step 1 (e.g., `192.168.1.128`).
5. **Save the Settings:** Apply and save the rule on your router.
6. **Get Your Public IP:** Find your public internet IP address by running this command in your terminal:
   ```bash
   curl ifconfig.me
   ```

   (It will show an IP like 67.67.xx.xx).

   
* Connect the App: Open the WhatsAppLegacy app on your phone, and in the Server Address input field, type your Public IP and Port like this:
* 67.67.xx.xx:12345

**Note**:if you want to test this in your wifi and doesnt need to access this outside of your network you can simply write your Ip adress of your pc and port(eg. '192.168.1.20:12345')

### Method 2: Tunneling via ngrok (Best for CGNAT / No Port Forwarding)
*Recommended if you are behind a CGNAT pool, cannot log into your router, or want a quick setup without changing router settings.*

1. **Download ngrok:** Get ngrok for your OS from the [Official ngrok Website](https://ngrok.com/).
2. **Get your Free Authtoken:** Sign up for a free ngrok account. Go to your dashboard, copy your **Authtoken**, and run this command in your terminal to connect your machine:
   ```bash
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```
This step is completely free and prevents ngrok from blocking your API requests with warning screens

Start Your WPL Server

ngrok https yourporthere

if you are using and testing the app with 2.2 use http instead

Get the Public URL: ngrok will generate a forwarding address that looks like this: https://xxxx-xx-xx.ngrok-free.app
⚠️ CRITICAL SECURITY NOTE: This generated link is your direct gateway. DO NOT SHARE THIS LINK WITH ANYONE. Anyone who has this link can access your local server API. From now on, you will use this private link to connect your app.

Connect the App: Open the WhatsAppLegacy app on your phone, and in the Ip:port field, paste your unique ngrok URL without the https:// prefix, like this:
```
xxxx-xx-xx.ngrok-free.app
```
and thats it

## Known Issues & Workarounds

* **Phone Number Formatting:** Entering your phone number with a leading `+` sign will cause a connection error. Please enter the number without any symbols.
* **Authentication Loading Indicator:** After you receive and enter the code on your primary device, the application will not display a loading spinner. Please do not panic and wait a moment for the process to complete.
* **Initial Connection Error:** The application may display an `error:null` message upon the first launch. Simply restart the application to resolve this issue.

## Project Status

* **Maintenance:** I will not be updating this application further. I am making it open-source so that anyone who wants to improve or modify the codebase is free to do so.

## Disclaimer

* **Security & Liability:** I assume no responsibility for any potential security risks. Use this software entirely at your own risk.
