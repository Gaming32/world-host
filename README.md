# World Host

Host your singleplayer worlds without having to run a server or setup port forwarding! In the future, we also plan to support Bedrock Edition clients through the use of Geyser.

## Status

Main server: ![Main server uptime (30 days)](https://img.shields.io/uptimerobot/ratio/m793898716-1dc62bc23f759a549159ccb0)

[See the status page](https://stats.uptimerobot.com/KmL5JhDV0X)

## What if my friend doesn't have the mod installed?

If your friend doesn't have the mod installed, you cannot use the friends system with them. However, you can run the command `/worldhost ip` to get an IP that can be used with tunneling. Do note, however, that not using the friends system could increase ping. <!-- If you don't want the ping increase, you can run `/worldhost tempip` to get a *temporary* server IP that lasts for 60 seconds. Clients who connect within the 60 seconds will remain connected. --> If you plan to never use the friends system, you can disable it in the mod settings (requires [Mod Menu](https://modrinth.com/mod/modmenu)).

## How does this mod work?

This mod has a server that your client connects to. This server is used to communicate with other clients using World Host. When your friend wants to join you, your friends client asks the server for an IP address to connect to. The server then asks your client to create a join mode (`JoinType` in the protocol). There are two join mods: UPnP and Proxy. UPnP mode is tried first. Your client tries to open a temporary port forward in your router that your friends client can use to connect to you directly. If this succeeds, your client tells the server the port to use, then the server tells your friend's client your IP and the port number. If UPnP fails, then your client tells the server to use Proxy mode. The server will then give your friend's client the same "proxy IP" as `/worldhost ip` gives. <!-- `/worldhost tempip` tries to do this whole process with trying UPnP first instead of just giving you the proxy IP straightaway. -->
