# WhatsAppLegacy
A WhatsApp webview client for Android 2.3+ that requires a self-hosted Node.js server to function.

## Features

* **Text Messaging:** Fully supports sending and receiving plain text messages.
  
* **Media:** Supports showing videos and photos(doesnt support HD videos and photos)
  
* **Chat History:** Ability to load and view past conversation.
  
* **Group Chats:** Full support for retrieving and displaying group chat sessions.

* **Emoji placeholders** Depending on your android version,you will receive emojis as emoticons(eg.😀->:D)

## 🛠️ Server Setup(for debian based linux distros)

**Note:** If you want to set the server up in your android phone,check ReadMeforTermux.md

**Note:** If you want to use this on Windows,use a linux emulator


You can create the server file on your Linux pc by following these steps:

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
```
npm install whatsapp-web.js express qrcode-terminal
```


Install git:
```
sudo apt install git -y
```
then,install this fork of whatsapp web libraries
```
npm i github:lindionez/whatsapp-web.js
```

5 Copy and paste the "server.js" file from the server folder
          
After that
6.Start the Server: You can start your server in two ways:

*Default Port(3000)by writing the command
```
node server.js
```
*Custom Port by writing the command
```
PORT=ANY-PORT-YOU-WANT node server.js
```

**Note**:if you want to test this in your wifi and doesnt need to access this outside of your network you can simply write your Ip adress of your pc and port(eg. '192.168.1.20:12345')

### Method 1: Tunneling via ngrok


1. **Download ngrok:** Get ngrok for your OS from the [Official ngrok Website](https://ngrok.com/).
2. 
3. **Get your Free Authtoken:** Sign up for a free ngrok account. Go to your dashboard, copy your **Authtoken**, and run this command in your terminal to connect your machine:
   ```bash
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```

Start Your WPL Server

After starting it open another shell and type this command
```
ngrok https yourporthere(eg.3000)
```

Get the Public URL: ngrok will generate a forwarding address that looks like this: https://xxxx-xx-xx.ngrok-free.app


⚠️ NOTE: This generated link is only meant for your use.DO NOT SHARE THIS LINK WITH ANYONE.


Connect the App: Open the WhatsAppLegacy app on your phone, and in the Ip:port field, paste your ngrok URL without the https:// prefix, like this:
```
xxxx-xx-xx.ngrok-free.app
```
and thats it

## Known Issues & Workarounds

* **Phone Number Formatting:** Entering your phone number with a leading `+` sign will cause a connection error. Please enter the number without any symbols.
* **Authentication Loading Indicator:** After you receive and enter the code on your primary device, the application will not display a loading spinner. Please do not panic and wait a moment for the process to complete.
* **Initial Connection Error:** The app may display an `error:null` message after the first launch. Simply open the app again and it will be fixed

## Project Status

* **Maintenance:** No maintenance

## Disclaimer

* **Security & Liability:** I assume no responsibility for any potential security risks. Use this software entirely at your own risk.
