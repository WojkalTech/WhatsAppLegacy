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
doldurracam
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


