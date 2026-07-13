# 
## 🚫 Critical Technical Info for Windows 8.1 and Lower Sudo-Environments

**The core functionalities of this project (sending/receiving messages) work perfectly on local networks. However, public port forwarding/tunneling features face severe operating system constraints on Windows 8.1 and older (Windows 7, etc.) 32-bit/64-bit systems.**

> ⚠️ **Note:** This project has **NOT** been tested on **Windows 10 and Windows 11** environments yet. It is currently unknown whether modern Windows network stacks natively handle these tunneling behaviors out of the box.

### Technical Breakdown of the Tunneling Failure on Windows 8.1:
* **Network Stack Packet Dropping (Connection Error):** Even if external tunneling tools (Ngrok, LocalTunnel) successfully bind a public URL, the legacy TCP/IP stack of Windows 8.1 fails to bridge incoming dynamic traffic to the active internal Node.js process (`127.0.0.1:3000`). Packets get dropped inside the OS layer without hitting the Node server, resulting in a connection error on the client side.
* **Host-Header & IPv6 Resolution Conflict:** Older Windows architectures aggressively force `localhost` resolution through the IPv6 loopback (`::1`). Since the Node.js server instances spin up on IPv4, reverse proxy handshakes often drop silently.
* **Global `npm` Directory Faults (`ENOENT`):** Attempting to execute runtime binaries via `npx` might instantly crash the terminal because the OS fail to automatically map or provision directory environments like `AppData\Roaming\npm`.
* **Missing Native `ssh` Client:** Windows 8.1 and lower lack a pre-installed OpenSSH client stack by default, ruling out quick native SSH tunneling alternatives.

---

## 🚀 Installation & Guide For Curious (Tutorial)

### 1. Local Deployment on Windows (LAN Only)
To deploy and test the WhatsApp API natively on your local machine:

1. Download a compatible **Chromium** build for your environment and drop it directly into the project root directory under the folder name `chrome-win32`.
2. Ensure your folder layout mirrors this exact structure:
   ```text
   wpl-server/
   ├── chrome-win32/ (Must contain the raw browser executable, e.g., chrome.exe)
   ├── node_modules/
   ├── server.js
   └── package.json
   
