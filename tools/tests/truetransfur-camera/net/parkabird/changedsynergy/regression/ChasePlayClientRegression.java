package net.parkabird.changedsynergy.regression;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;
import net.parkabird.changedsynergy.client.ChasePlayClient;
import net.parkabird.changedsynergy.network.ChasePlayStatePacket;
import static net.parkabird.changedsynergy.network.ChasePlayStatePacket.*;

final class ChasePlayClientRegression {
    private static int checks;
    static void run(Minecraft mc) throws Exception {
        boolean toggle = mc.options.toggleCrouch().get(), shift = mc.options.keyShift.isDown(), jump = mc.options.keyJump.isDown();
        Method poll = ChasePlayClient.class.getDeclaredMethod("freshGesture", boolean.class, boolean.class);poll.setAccessible(true);
        try {
            mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(true);mc.options.keyJump.setDown(false);
            ChasePlayClient.receive(new ChasePlayStatePacket(9101,INVITED,200,false,1));
            check((int)poll.invoke(null,true,false)==-1,"Crouch already held when invited does not choose a role");
            check((int)poll.invoke(null,false,false)==-1,"Releasing the old crouch does not count");
            check((int)poll.invoke(null,true,false)==0,"Fresh hold-crouch chooses chasing");
            check((int)poll.invoke(null,true,false)==-1,"Holding crouch does not send repeated role choices");
            ChasePlayClient.receive(new ChasePlayStatePacket(9101,CHASING,300,true,1));
            check((int)poll.invoke(null,true,false)==-1,"Stage update does not reinterpret role crouch as cancellation");
            poll.invoke(null,false,false);
            check((int)poll.invoke(null,true,false)==-1,"Active-game exits count physical presses instead of toggled-state edges");
            ChasePlayClient.receive(new ChasePlayStatePacket(9101,CLEAR,0,false,1));

            mc.options.keyShift.setDown(false);mc.options.toggleCrouch().set(true);mc.options.keyShift.setDown(true);
            ChasePlayClient.receive(new ChasePlayStatePacket(9102,INVITED,200,false,1));
            check((int)poll.invoke(null,mc.options.keyShift.isDown(),false)==-1,"Pre-existing toggle-crouch is also ignored");
            mc.options.keyShift.setDown(true);poll.invoke(null,mc.options.keyShift.isDown(),false);
            mc.options.keyShift.setDown(true);
            check((int)poll.invoke(null,mc.options.keyShift.isDown(),false)==0,"A fresh toggled crouch chooses chasing");
            check(mc.options.toggleCrouch().get(),"Game input never changes the user's toggle preference");
            ChasePlayClient.receive(new ChasePlayStatePacket(9102,CLEAR,0,false,1));

            mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(false);mc.options.keyJump.setDown(true);
            ChasePlayClient.receive(new ChasePlayStatePacket(9103,INVITED,200,false,1));
            check((int)poll.invoke(null,false,true)==-1,"Jump already held when invited does not choose running");
            poll.invoke(null,false,false);
            check((int)poll.invoke(null,false,true)==1,"Fresh jump chooses running");
            ChasePlayClient.receive(new ChasePlayStatePacket(9103,CHASING,300,false,1));
            poll.invoke(null,false,false);
            check((int)poll.invoke(null,false,true)==-1,"Ordinary jumps during the chase are not role choices");
            ChasePlayClient.receive(new ChasePlayStatePacket(9102,CLEAR,0,false,1));
            var state=ChasePlayClient.class.getDeclaredField("current");state.setAccessible(true);
            check(state.get(null)!=null,"Stale end packets do not clear a different invitation");
            for(String key:new String[]{"choose","ready","head_start_run","wait","run","chase","hug","comfort","unavailable","pat","ended","false_start"}) {
                String id="message.changed_synergy.chase."+key;
                check(I18n.exists(id),"Chase prompt is translated: "+key);
                check(!Component.translatable(id,"Shift","Space").getString().contains("%s"),"Chase prompt arguments render: "+key);
            }
            System.out.println("CHASE_PLAY_CLIENT_REGRESSION_PASS: "+checks+" checks; real hold/toggle bindings, fresh edges, role/cancel consumption and translated prompts");
        } finally {
            ChasePlayClient.receive(new ChasePlayStatePacket(9103,CLEAR,0,false,1));
            mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(shift);mc.options.keyJump.setDown(jump);mc.options.toggleCrouch().set(toggle);
        }
    }
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}
