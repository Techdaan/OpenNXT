# OpenNXT - RS3 918

A RS3 RSPS framework targeted at NXT, the goal is to stay up-to-date with RS3

# Discord

We have a Discord server you can join! https://discord.gg/vFgmFhUt

# Tooling

One of the goals of this project is to have all necessary tools built-in. This includes the client downloader, client
and launcher patcher, cache downloaders et cetera.

Tools can be executed through the command line with the following parameters: `run-tool <tool-name> [--help]`

You can create your tools easily by creating a new class in `com.opennxt.tools.impl`. Your class must extend from base
class `com.opennxt.tools.Tool`. Tools are registered automatically using classpath scanning.

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
