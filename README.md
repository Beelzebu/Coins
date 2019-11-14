# Status: discontinued

This project is replaced in favor of [Coins3](https://github.com/Beelzebu/Coins3)


# Coins

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cebeb3932ff04424acf86554fd1d7eec)](https://www.codacy.com/app/Beelzebu/Coins?utm_source=github.com&utm_medium=referral&utm_content=Beelzebu/Coins&utm_campaign=badger)
![license GNU AGLP v3.0](https://img.shields.io/badge/license-GNU%20AGLP%20v3.0-lightgrey.svg)
![Minecraft Version 1.8.x-1.12.x](https://img.shields.io/badge/supports%20minecraft%20versions-1.8.x--1.12.x-brightgreen.svg)
![latest build](https://img.shields.io/jenkins/s/https/ci.nifheim.net/job/Coins.svg)

Coins is the most complete and efficient plugin to manage a secondary economy in any server, it has a lot of features that you gonna love, everything is configurable, inclusive the command, you can send different messages to players depending on his game language, a multipliers system for networks, a integrated API for developers and much more!

## Features:
 - Customizable messages and command.
 - Multi lang, you can have different messages for any lang, the plugin will use the user's lang to get the corresponding file and if the message doesn't exist in that file the plugin gonna fallback to the messages.yml file.
 - BungeeCord support, you can put it in bungeecord to use the API if you need.
 - Command cost, you can add a cost to use commands.
 - Command Executors, you can create a list of commands that can be executed paying a specific amount of coins, see the config for more information. (you can create these in the BungeeCord config and will be available on every spigot server connected)
 - Sign executors, you can use executors in signs.
 - A internal cache to avoid too many queries to the Database.
 - The cache is updated through BungeeCord or RedisBungee if is present.
 - PlaceholderAPI support.
 - Vault support, the plugin can manage the entire server economy also across servers.
 - Multiplier, you can add a permission to players to get more coins or create multipliers that can be enabled for the entire server for a specific amount of time.
 - Internal log to find errors faster.
 - All tasks run async.


## Commands:
### User commands:
 - /coins help - Show all the commands.
 - /coins - Show the own coins.
 - /coins <player> - Show the coins of other player.
 - /coins pay <player> <amount> - Pay coins to other player.
 - /coins top - Shows the top 10 of players with most coins.
 - /coins multipliers - Open a gui with all your multipliers.
### Admin commands:
 - /coins give <player> <amount> (true) - Give coins to the specified player, if you add true to the end of the command the permission and server multipliers will be used.
 - /coins take <player> <amount> - Take coins from a player, if the amount is higher than the coins that the player has, the coins will be set to 0.
 - /coins set <player> <amount> - Set the coins for a player.
 - /coins reset <player> - Reset the coins for a player with the default amount defined in the config.
 - /coins reload - Reload the config and all loaded messages files.
 - /coins multipliers help - Show some commands about multipliers.
 
## Permissions:
 - coins.admin - Give access to all admin commands.
 - coins.multiplier.x<2-100> - Permission for personal multipliers, this can be between 2 to 100.

## Requisites:
 - Java 8
 - PlaceholderAPI (optional, used for the placeholders).
 - Vault (optional, used if you make this the primary economy).

## Languages:
These languages are built-in, but you can add more if you want, just create a messages\_<lang>.yml and the plugin will use it for players with that language, see [https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) for the lang code (ISO 639-1).

 - English
 - Spanish
 - Chinese
 
If you want to translate the plugin to another language, please send a PR with your translation.

## ToDo:
 - Add a way to import data from similar plugins like PlayerPoints.
 - Add support for Redis storage.
