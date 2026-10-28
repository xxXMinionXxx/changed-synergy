package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.dialogue.NpcDialogue;

final class SocialAddressRegression {
    static void run(MinecraftServer server)throws Exception {
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"AddressCheck"));
        var uses=NpcDialogue.class.getDeclaredMethod("usesSocialAddress",NpcDialogue.Cue.class);uses.setAccessible(true);
        var address=NpcDialogue.class.getDeclaredMethod("withSocialAddress",net.ltxprogrammer.changed.entity.ChangedEntity.class,
                ServerPlayer.class,NpcDialogue.Cue.class,Object[].class);address.setAccessible(true);
        int checks=0;
        try {
            for(int scenario=0;scenario<4;scenario++) {
                var creature=scenario==3?ChangedEntities.WHITE_LATEX_WOLF_MALE.get().create(server.overworld())
                        :ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld());
                try {
                    if(scenario==1||scenario==2)((net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity)creature).tame(player);
                    if(scenario==2||scenario==3)LatexSocialMemory.addBond(creature,player);
                    String expected="address.changed_synergy."+(scenario==0?"friend":scenario==3?"partner":"dark_partner");
                    for(var cue:NpcDialogue.Cue.values())if((boolean)uses.invoke(null,cue)) {
                        Object[] args=(Object[])address.invoke(null,creature,player,cue,new Object[]{"payload"});
                        if(args.length!=3||!((TranslatableContents)((Component)args[0]).getContents()).getKey().equals(expected)
                                ||!((Component)args[1]).getString().equals(player.getDisplayName().getString()))
                            throw new AssertionError("Wrong social title or shifted player placeholder: "+scenario+"/"+cue);
                        if(cue!=NpcDialogue.Cue.RELATIONSHIP_ESTABLISHED&&cue!=NpcDialogue.Cue.RELATIONSHIP_DIET_ESTABLISHED
                                &&!args[2].equals("payload"))throw new AssertionError("Cue-specific arguments shifted: "+cue);
                        checks++;
                    }
                } finally {creature.discard();}
            }
            System.out.println("SOCIAL_ADDRESS_REGRESSION_PASS: "+checks+" complete social-cue argument contracts for strangers, native pets, native-pet bonds and other bonded factions");
        } finally {player.discard();}
    }
}
