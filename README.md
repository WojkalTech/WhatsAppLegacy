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

## Known Issues & Workarounds

* **Phone Number Formatting:** Entering your phone number with a leading `+` sign will cause a connection error. Please enter the number without any symbols.
* **Authentication Loading Indicator:** After you receive and enter the code on your primary device, the application will not display a loading spinner. Please do not panic and wait a moment for the process to complete.
* **Initial Connection Error:** The application may display an `error:null` message upon the first launch. Simply restart the application to resolve this issue.

## Project Status

* **Maintenance:** I will not be updating this application further. I am making it open-source so that anyone who wants to improve or modify the codebase is free to do so.

## Disclaimer

* **Security & Liability:** I assume no responsibility for any potential security risks. Use this software entirely at your own risk.
