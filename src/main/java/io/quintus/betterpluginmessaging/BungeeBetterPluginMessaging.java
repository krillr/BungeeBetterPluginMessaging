package io.quintus.betterpluginmessaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ConnectedPlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.Util;

public class BungeeBetterPluginMessaging extends Plugin implements Listener {

    private ProxyServer proxy;

    @Override
    public void onEnable() {
        proxy = getProxy();
        proxy.getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("BungeeCord")) { return; }

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        String[] components = subchannel.split("::", 2);
        if (components.length < 2) { return; }

        Server server = (Server)event.getSender();

        String command = components[0];
        String requestId = components[1];
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String target;
        ServerInfo targetServer;
        ProxiedPlayer targetPlayer;

        switch (command) {
            case "IP":
                out.writeUTF(subchannel);
                out.writeUTF(server.getAddress().getHostString());
                out.writeShort(server.getAddress().getPort());
                break;

            case "PlayerCount":
                target = in.readUTF();
                out.writeUTF(subchannel);
                out.writeUTF(target);

                if (target.equals("ALL")) {
                    out.writeInt(proxy.getOnlineCount());
                } else {
                    targetServer = proxy.getServerInfo(target);
                    if (targetServer == null) { return; }
                    out.writeInt(targetServer.getPlayers().size());
                }
                break;

            case "PlayerList":
                target = in.readUTF();
                out.writeUTF(subchannel);
                out.writeUTF(target);

                if (target.equals("ALL")) {
                    out.writeUTF(Util.csv(proxy.getPlayers()));
                } else {
                    targetServer = proxy.getServerInfo(target);
                    if (targetServer == null) { return; }
                    out.writeUTF(Util.csv(targetServer.getPlayers()));
                }
                break;

            case "GetServers":
                out.writeUTF(subchannel);
                out.writeUTF(Util.csv(proxy.getServers().keySet()));
                break;

            case "GetServer":
                out.writeUTF(subchannel);
                out.writeUTF(server.getInfo().getName());
                break;

            case "UUID":
                targetPlayer = (ConnectedPlayer)event.getReceiver();
                out.writeUTF(subchannel);
                out.writeUTF(targetPlayer.getUniqueId().toString());
                break;

            case "UUIDOther":
                String playerName = in.readUTF();
                targetPlayer = proxy.getPlayer(playerName);
                out.writeUTF(subchannel);
                out.writeUTF(targetPlayer.getName());
                out.writeUTF(targetPlayer.getUniqueId().toString());
                break;

            case "ServerIP":
                target = in.readUTF();
                targetServer = proxy.getServerInfo(target);
                out.writeUTF(subchannel);
                out.writeUTF(target);
                out.writeUTF(targetServer.getAddress().getHostString());
                out.writeShort(targetServer.getAddress().getPort());
                break;

            default: return;
        }

        byte[] b = out.toByteArray();
        if (b.length == 0) { return; }
        server.sendData("BungeeCord", b);
    }

}
