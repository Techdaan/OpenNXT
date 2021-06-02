# OpenNXT - RS3 919

A RS3 RSPS framework targeted at NXT, the goal is to stay up-to-date with RS3

# Discord

We have a Discord server you can join! https://discord.gg/u5p4w3zjjx

# Tooling

One of the goals of this project is to have all necessary tools built-in. This includes the client downloader, client
and launcher patcher, cache downloaders et cetera.

Tools can be executed through the command line with the following parameters: `run-tool <tool-name> [--help]`

You can create your tools easily by creating a new class in `com.opennxt.tools.impl`. Your class must extend from base
class `com.opennxt.tools.Tool`. Tools are registered automatically using classpath scanning.

# Updating
To update OpenNXT to a new version:

1. Download the latest clients using `run-tool client-downloader`
2. Download the latest cache using `run-tool cache-downloader`
3. Patch the latest clients using `run-tool client-patcher`
4. Update the `build` field in `./data/config/server.toml`

If the version you are updating to is not yet supported by OpenNXT OR you want to contribute to the project's networking-related code and implementations, it is highly recommended you also fulfil these steps:
1. In `com.opennxt.net.login.LoginEncoder`, replace `RS3_MODULUS` with the `old login` key printed by the patcher.
2. Create a new directory: `./data/prot/[new version]/`, replacing `[new version]` with the server version.
3. Open the win64.exe client in Ghidra, and run the [Ghidra NXT Auto Refactoring Script](https://github.com/Techdaan/rs3nxt-ghidra-scripts). For more information on how to install and use this tool you can visit [my Rune-Server thread.](https://www.rune-server.ee/runescape-development/rs-503-client-server/downloads/698604-nxt-win64-ghidra-refactoring-script.html)
4. Run the script and use the data it prints to the console to populate the files in `./data/prot/[version]/*.toml`. The tool does not print `clientProtNames`. Those, you will have to do manually.
5. Populate the packet fields using files in the `./data/prot/[version]/[(client/server)prot]` directories

# Setup

To set the project up:

1. Generate your server's RSA keys: `run-tool rsa-key-generator`
2. Download the latest RS clients: `run-tool client-downloader`
   
   :warning: Latest clients might not be compatible with this repository. Please ensure this repository version matches
   the version of your clients.
3. Put the original launcher in: `./data/launcers/win/origina.exe` (can be found
   at `C:\Program Files\Jagex\RuneScape Launcher\RuneScape.exe`)
4. Create a configuration file `./data/config/server.toml`. Configure the following fields:
   ```toml
   hostname = "127.0.0.1"
   configUrl = "http://127.0.0.1/jav_config.ws?binaryType=2"
   ```
   `configUrl` is the URL the launcher will get the `jav_config.ws` from.
   `hostname` is the IP your server runs on 
5. Patch the client and launchers using `run-tool client-patcher`
6. Download the latest cache using `run-tool cache-downloader`
7. Wait until this framework progresses further
