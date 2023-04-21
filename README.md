<img alt="WH icon" width="280" align="right" src="https://raw.githubusercontent.com/Gaming32/world-host/main/common/src/main/resources/assets/world-host-common/icon.png">

# World Host

[![Modrinth](https://img.shields.io/modrinth/dt/world-host?color=00AF5C&label=downloads&style=round&logo=modrinth)](https://modrinth.com/mod/world-host)
[![Mod Environment](https://img.shields.io/badge/Enviroment-Client-purple?style=round)]()

[![Server uptime](https://img.shields.io/uptimerobot/ratio/m793898716-1dc62bc23f759a549159ccb0?label=Server%20uptime&color=blue)](https://stats.uptimerobot.com/KmL5JhDV0X)

Host your singleplayer worlds **without having to run a server**
<br /> **or setup port forwarding!** <br />
In the future, we also plan to support Bedrock Edition clients<br />
through the use of Geyser.

## What if my friend doesn't have the mod installed?

If your friend doesn't have the mod installed, you cannot use the friends system with them. However, you can run the command `/worldhost ip` to get an IP that can be used with tunneling. Do note, however, that not using the friends system could increase ping. <!-- If you don't want the ping increase, you can run `/worldhost tempip` to get a *temporary* server IP that lasts for 60 seconds. Clients who connect within the 60 seconds will remain connected. --> If you plan to never use the friends system, you can disable it in the mod settings (requires [Mod Menu](https://modrinth.com/mod/modmenu)).

## How does this mod work?

This mod has a server that your client connects to, this server is used to communicate with other clients using World Host. 

1. When your friend wants to join you, your friend's client asks the server for an IP address to connect to. 
2. The server then asks your client to create a join mode (`JoinType` in the protocol). There are two join mods: UPnP and Proxy. 
3. UPnP mode is tried first. Your client tries to open a temporary port forward in your router that your friends client can use to connect to you directly. 
4. If UPnP succeeds, your client tells the server the port to use, then the server tells your friend's client your IP and the port number. 
5. If UPnP fails, then your client tells the server to use Proxy mode. The server will then give your friend's client the same "proxy IP" as `/worldhost ip` does. 
 
<!-- `/worldhost tempip` tries to do this whole process with trying UPnP first instead of just giving you the proxy IP straightaway. -->

## License

The mod is open source and licensed under the [MIT License](LICENSE).
