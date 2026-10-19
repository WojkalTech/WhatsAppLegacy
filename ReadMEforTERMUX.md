# Server Setup (Termux)

Follow these steps to setup your server on your android phone using termux

---

## 📋 Step 1: Initial Repository & Mirror Setup
First off all have termux on your phone.You can download it from here:https://f-droid.org/tr/packages/com.termux/

Then, make sure you have a fresh environment. Open Termux and execute:

```bash
termux-change-repo
```
Instructions: Select multiple-mirrors + all mirrors option. Consequently, pressing the Enter key twice in succession will be enough

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
⚙️ Step 2: Update the packages and install Node.js alongside NPM:
```
pkg update
pkg install nodejs npm
```
Troubleshooting: In case the command fails or hangs, repeat it 2 to 3 times. Errors might occasionally occur depending on network states.

🌐 Step 3: Chromium Installation
Install chromium using this command:
```
pkg install chromium -y
```
Troubleshooting: If you get error and fail to install chromium, restart your current Termux session and execute it again.

Create Browser Shortcut Path
```
ln -s $PREFIX/bin/chromium-browser $PREFIX/bin/chromium
```

📁 Step 4: Create a folder where your server will be located and go inside of your folder(SUS)using these commands:
```
mkdir wpl-server && cd wpl-server
```
Use this command afterwards:
```
npm init -y
```

If anything goes wrong during initialization,run these commands:
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
Install mutler:
```
npm install multer
```
Install git:
```
pkg install git
```
Install this fork of whatsappweb.js
```
npm i github:lindionez/whatsapp-web.js
```
Install ffmpeg(optional(not much)):
```
pkg install ffmpeg
```

💾 Step 6:create server.js file
```
nano server.js
```

Actions: Inside this file,Copy-paste the code from "Server" Folder

Save Controls: Press Ctrl + X, followed by Y, and then press Enter to save.
Then start the server
```
node server.js
```

Now the server is working in your network,Currently you cant connect to this server if you are outside of this said network.The tutorial on how to use it without Being required to stay connected to your wifi is down below.

Keep the session server works open.and start another session


🚀 Step 7: Global Traffic Routing (Serveo-to use your server outside of your wi-fi)
Inside the newly opened second active session terminal window, install the required packages:
```
pkg install dropbear
pkg install openssh
```
Option A:Dynamic Link (Not Recommended)
You can use serveo with command given below but I dont recommend it as it will give you random link each time you restart the server
```
ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=5 -R 80:localhost:3000 serveo.net
```
Disadvantage: Since you dont have a still link.everytime you use serveo,it will give you a random link everytime.

Option B: Permanent domain(Optional but Highly Recommended)
Follow these steps if you want a still link(basically everytime you use serveo,You will be given the same link)

1.If you have started serveo,shut it down using Ctrl+c combo

2.run these command after changing it
```
ssh-keygen -t ed25519 -C "your_email@example.com"
```
(Be sure to replace the placeholder with your actual email address).

The script string returns a long, descriptive SHA-256 secure hash fingerprint signature link. Copy this text and paste it onto your Google search bar field to navigate to the provider dashboard.

The site yields a corresponding distinct public security key file output structure. Paste that token into the explicit SHA-256 field form interface workspace shown on the screen panel.

Create a registered account profile inside that cloud terminal web interface, and configure your custom host domain label name. (let's take the word "queen" as an example).

Once your administrative configurations are saved on the portal, return to the second Termux session panel view (where the core main node server is NOT executing). Since you need to boot up both the server in the first window and the persistent SSH bridge pipeline loop inside this second terminal, you shall use this command(you better pin that command because you will need it every time you start the server)
```
while true; do ssh -o ServerAliveInterval=30 -R queen:80:localhost:3000 serveo.net; echo "Connection lost, automatically reconnecting to "queen" link in 3 seconds..."; sleep 3; done
```
**Quick Note:** Guys İf you have any other nickname than queen for your ssh key change change the queen part with that nickname

The tunnel server protocol establishes communications handshake checks and assigns your global network address link:
Forwarding HTTP traffic from _https://queen.serveo.net__ or sum like that use the link whether serveo gives you

* **Note:** You cant use "https://"part of your link delete that part to connect
write your number down and thats it


*  **Note:** Every time you restart the server,its better you close your existing session terminal and remove your device from WhatsApp,you May counter errors otherwise.

