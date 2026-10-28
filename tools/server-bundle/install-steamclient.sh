#!/bin/sh
set -e
cd "$(dirname "$0")"
mkdir -p steamcmd
if [ ! -x steamcmd/steamcmd.sh ]; then
	curl -fsSL https://steamcdn-a.akamaihd.net/client/installer/steamcmd_linux.tar.gz | tar -xz -C steamcmd
fi
steamcmd/steamcmd.sh +quit
mkdir -p "$HOME/.steam/sdk64"
ln -sf "$PWD/steamcmd/linux64/steamclient.so" "$HOME/.steam/sdk64/steamclient.so"
echo "Done: $HOME/.steam/sdk64/steamclient.so"
