# WhatsAppLegacy
A WhatsApp webview client for Android 2.3.6+ that requires a self-hosted Node.js server to function.

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
```
After that press ctrl+x,pick yes,press enter
6.Start the Server: You can start your server in two ways:

*Default Port(3000)by writing the command
```
node server.js
```
*Custom Port by writing the command
```
PORT=ANY-PORT-YOU-WANT node server.js
```


## Known Issues & Workarounds

* **Phone Number Formatting:** Entering your phone number with a leading `+` sign will cause a connection error. Please enter the number without any symbols.
* **Authentication Loading Indicator:** After you receive and enter the code on your primary device, the application will not display a loading spinner. Please do not panic and wait a moment for the process to complete.
* **Initial Connection Error:** The application may display an `error:null` message upon the first launch. Simply restart the application to resolve this issue.

## Project Status

* **Maintenance:** I will not be updating this application further. I am making it open-source so that anyone who wants to improve or modify the codebase is free to do so.

## Disclaimer

* **Security & Liability:** I assume no responsibility for any potential security risks. Use this software entirely at your own risk.
