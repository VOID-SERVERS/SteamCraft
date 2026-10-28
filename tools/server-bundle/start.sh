#!/bin/sh
cd "$(dirname "$0")" || exit 1
JAVA="${JAVA:-java}"
MEMORY="${MEMORY:-2G}"

if ! grep -qs '^eula=true' eula.txt; then
	printf 'Accept the Minecraft EULA (https://aka.ms/MinecraftEULA)? [y/N] '
	read -r answer
	case "$answer" in
		y|Y) echo 'eula=true' > eula.txt ;;
		*) echo 'The server will not start without accepting the EULA.'; exit 1 ;;
	esac
fi

if [ ! -e "$HOME/.steam/sdk64/steamclient.so" ]; then
	echo "WARNING: ~/.steam/sdk64/steamclient.so is missing: the server cannot log on to Steam and will refuse Steam players."
	echo "         Run ./install-steamclient.sh (or install Steam)."
fi

exec "$JAVA" -Xmx"$MEMORY" -jar fabric-server-launch.jar nogui
