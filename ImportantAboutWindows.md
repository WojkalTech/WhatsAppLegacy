# 
## 🚫 Critical Technical Info for Windows 8.1 and Lower Sudo-Environments

**The core functionalities of this project (sending/receiving messages) work perfectly on local networks. However, public port forwarding/tunneling features face severe operating system constraints on Windows 8.1 and older (Windows 7, etc.) 32-bit/64-bit systems.**

> ⚠️ **Note:** This project has **NOT** been tested on **Windows 10 and Windows 11** environments yet. It is currently unknown whether modern Windows network stacks natively handle these tunneling behaviors out of the box.

> 🚫 Public Port Forwarding & Tunneling Limitations
Unfortunately, there is no further progression beyond this point regarding external network visibility. Neither Ngrok nor LocalTunnel routes external requests successfully to the server deployment. Both tools have been installed locally, but external incoming traffic fails to drop into these generated tunnel links.
>
>The exact technical root cause remains unidentified. If anyone attempts this setup and successfully bypasses these public tunneling bottlenecks, please provide feedback to update this documentation.
>
>For those who still wish to test and debug the tunneling methods locally, the configuration steps performed during testing are documented below:<

## 🚀 Installation Guide for Curious (Tutorial)

### 1. Prerequisites & Environment Setup
1. Go to the official Node.js website, download the installer, and install it on your computer.
2. Create a new folder (preferably on your desktop). You can name it anything; this guide will proceed using the folder name `wpl-server`.
3. Create the `server.js` file:
   * Go inside the `wpl-server` folder, right-click on an empty space, select **New**, and choose **Text Document**.
   * 
   * You need to delete the `.txt` extension completely.
   * 
   * *If you cannot see file extensions:* In the folder window, click on the **View** menu at the very top, click on **Show**, and check the **File name extensions** option.
   * 
   * Now, delete the `.txt` extension and rename the file exactly to `server.js`.
   * 
4. Open the terminal inside your project folder:
   * While inside the `wpl-server` folder, click once with your mouse on the address bar at the top (the long white bar showing the folder path like `C:\Users\...`).
   * Delete all the text inside it, type `cmd` directly, and press **Enter**.

### 2. Installing Dependencies (Packages)
In the opened black terminal window, type the following commands one by one and press **Enter** after each (the download might take 1–2 minutes depending on your computer and internet speed):

```bash
npm init -y
npm install whatsapp-web.js express puppeteer-core
```
3. Setting Up the Server Code & Chrome Files
After the installation is complete, type notepad server.js into the command prompt, paste your server source code inside the file(you can find the server code in mainbranch,name of the file is UniversalServerCode.txt, and save it.

Next, you must move your local Chrome files into the wpl-server folder:

First, make sure Google Chrome is installed on your system.

If it is installed and you have a shortcut on your desktop, right-click the shortcut and select Open file location to navigate to the source files.

Copy all the files and folders inside that directory.

Create a new folder named chrome-win32 inside your wpl-server directory, and paste all the copied Chrome files into it.

5. Running the Server
   
In your CMD window, run the following command to start the server:
```
node server.js
```
The server is ready and fully active when you see the following two log lines on your terminal screen:

WPL Server running on port 3000
WhatsApp client is ready

📌 Important Note: If you do not see the second log (WhatsApp client is ready) after restarting the server, it is highly recommended to restart your computer and launch the server again. Once active, you can connect and log in to your WhatsApp via your local Wi-Fi network.

## 🧪 LocalTunnel Experimental Guide(Method 1)
**The following configurations were applied to overcome runtime constraints:**

1.Pressed Windows + R, navigated to %appdata%, and manually created a folder named npm to resolve global path execution errors.

**2.Installed LocalTunnel globally:**
```
npm install -g localtunnel
```

**3.Activated the public tunnel via:**
```
lt --port 3000
```

## 🔍 Ngrok Experimental Guide
**For those trying to implement Ngrok forwarding, the following sequence was followed:**

*Download and Installation:*
Visit ngrok.com, register for a free account (or log in directly using a Google account).

Download the official archive for Windows from the dashboard panel (Select Windows 32-bit .zip if running a legacy 32-bit operating system).

Extract the ngrok.exe binary directly into the wpl-server root directory or onto your desktop.

**Account Authentication (AuthToken):**

Authenticate your local binary with your developer dashboard profile using the unique token provided on your account setup page.

Open a new CMD screen in the exact directory where ngrok.exe is located, paste the following command structure, and press Enter:
```
ngrok config add-authtoken <your_secret_token>
```
_(This permanently saves the token configuration to your system; you do not need to repeat this step)._

**Firing Up the Tunnel:**

Since the Express server listens natively on port 3000, initiate the public HTTP forwarder by running:
```
ngrok http 3000
```
Similarly, entering the generated public URL (excluding the http:// prefix) into your client application's destination IP:Port configuration fields should initiate a remote link, but connection handshakes fail to execute properly.

