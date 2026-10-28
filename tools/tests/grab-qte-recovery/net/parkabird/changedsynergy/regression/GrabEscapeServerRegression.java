package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.DEDICATED_SERVER)
public final class GrabEscapeServerRegression {
    @SubscribeEvent public static void start(ServerStartedEvent event){
        try{
            var player=new ServerPlayer(event.getServer(),event.getServer().overworld(),new GameProfile(UUID.randomUUID(),"QteFixture"));
            player.connection=new ServerGamePacketListenerImpl(event.getServer(),new Connection(PacketFlow.SERVERBOUND),player);
            player.setPos(0,100,0);
            GrabQteRegression.runServer(player);
            System.out.println("GRAB_ESCAPE_SERVER_PASS");
        }catch(Throwable failure){System.err.println("GRAB_ESCAPE_SERVER_FAIL");failure.printStackTrace();}
        finally{event.getServer().halt(false);}
    }
}
