package dev.particlefx.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.network.EditorNetworking;
import dev.particlefx.permission.ParticleFXPermissions;
import dev.particlefx.server.ParticleFXServer;
import dev.particlefx.storage.*;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import static net.minecraft.server.command.CommandManager.*;

public final class ParticleFXCommand {
    @FunctionalInterface private interface Action {int run() throws Exception;}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root=literal("particlefx").executes(ctx->run(ctx,"use",()->{
            feedback(ctx,"ParticleFX: list, group, redstone, create, delete, edit, play, stop, pause, resume, reload, preview, attach, detach, status");return 1;
        }));
        root.then(literal("list").executes(ctx->run(ctx,"use",()->{feedback(ctx,String.join(", ",manager(ctx).names()));return manager(ctx).names().size();})));
        root.then(literal("status").executes(ctx->run(ctx,"use",()->{feedback(ctx,"Effects: "+manager(ctx).names().size()+"; groups: "+manager(ctx).groupNames().size()+"; active effects: "+manager(ctx).activeCount()+"; active groups: "+manager(ctx).activeGroupCount()+"; redstone triggers: "+manager(ctx).redstoneTriggers().size()+"; previous tick packets: "+manager(ctx).lastPackets());return manager(ctx).activeCount()+manager(ctx).activeGroupCount();})));
        root.then(literal("create").then(argument("name",StringArgumentType.word()).executes(ctx->run(ctx,"create",()->{
            String name=name(ctx);EffectValidator.name(name);if(manager(ctx).names().contains(name))throw new IllegalArgumentException("Effect already exists");
            ParticleEffect e=new ParticleEffect();e.name=name;manager(ctx).register(e,true);feedback(ctx,"Created "+name);return 1;
        }))));
        root.then(literal("delete").then(effectArgument().executes(ctx->run(ctx,"delete",()->{manager(ctx).delete(name(ctx));feedback(ctx,"Deleted "+name(ctx));return 1;}))));
        root.then(literal("edit").then(effectArgument()
            .executes(ctx->run(ctx,"edit",()->{
                ParticleEffect e=manager(ctx).definition(name(ctx));var player=ctx.getSource().getPlayer();
                if(player==null||!EditorNetworking.open(player,e))feedback(ctx,"Edit config/particlefx/effects/"+name(ctx)+".json then /particlefx reload; or /particlefx edit "+name(ctx)+" json <complete-json>");
                return 1;
            }))
            .then(literal("json").then(argument("json",StringArgumentType.greedyString()).executes(ctx->run(ctx,"edit",()->{
                manager(ctx).definition(name(ctx));String json=StringArgumentType.getString(ctx,"json");
                if(json.length()>EffectValidator.MAX_JSON_BYTES)throw new IllegalArgumentException("JSON too large");
                ParticleEffect e=JsonSupport.GSON.fromJson(json,ParticleEffect.class);EffectValidator.validate(e);
                EffectValidator.require(e.name.equals(name(ctx)),"Effect name differs");manager(ctx).register(e,true);feedback(ctx,"Saved "+e.name);return 1;
            }))))));
        var play=effectArgument().executes(ctx->play(ctx,false,false,false));
        play.then(literal("viewers").then(argument("viewers",EntityArgumentType.players()).executes(ctx->play(ctx,false,true,false))));
        play.then(literal("at").then(argument("position",Vec3ArgumentType.vec3()).executes(ctx->play(ctx,true,false,false))
            .then(literal("viewers").then(argument("viewers",EntityArgumentType.players()).executes(ctx->play(ctx,true,true,false))))));
        root.then(literal("play").then(play));
        root.then(literal("preview").then(effectArgument().executes(ctx->play(ctx,false,false,true))));
        for(String action:List.of("stop","pause","resume"))root.then(literal(action).then(effectArgument().executes(ctx->run(ctx,"play",()->{
            UUID owner=ParticleFXPermissions.admin(ctx.getSource())?null:owner(ctx);
            int n=manager(ctx).control(name(ctx),owner,action);feedback(ctx,action+": "+n+" instance(s)");return n;
        }))));
        root.then(literal("reload").executes(ctx->run(ctx,"reload",()->{manager(ctx).reload();feedback(ctx,"Reloaded; previous instances stopped");return 1;})));
        root.then(literal("attach").then(effectArgument().then(argument("target",EntityArgumentType.entities()).executes(ctx->run(ctx,"play",()->{
            var targets=EntityArgumentType.getEntities(ctx,"target");
            for(var target:targets) {
                if(!(target instanceof LivingEntity))throw new IllegalArgumentException("Only LivingEntity targets are supported");
                if(!ParticleFXPermissions.admin(ctx.getSource())&&!target.getUuid().equals(owner(ctx)))throw new IllegalArgumentException("Only administrators can attach to other entities");
            }
            List<UUID> started=new ArrayList<>();
            try {for(var target:targets)started.add(manager(ctx).attach(name(ctx),(LivingEntity)target,owner(ctx)));}
            catch(RuntimeException e) {for(UUID id:started)manager(ctx).control(id,"stop");throw e;}
            feedback(ctx,"Attached: "+started.size());return started.size();
        })))));
        root.then(literal("detach").then(argument("target",EntityArgumentType.entities()).executes(ctx->run(ctx,"play",()->{
            int n=0;UUID owner=ParticleFXPermissions.admin(ctx.getSource())?null:owner(ctx);
            for(var target:EntityArgumentType.getEntities(ctx,"target"))n+=manager(ctx).detach(target.getUuid(),owner);feedback(ctx,"Detached: "+n);return n;
        }))));
        var groupRoot=literal("group");
        groupRoot.then(literal("list").executes(ctx->run(ctx,"use",()->{feedback(ctx,String.join(", ",manager(ctx).groupNames()));return manager(ctx).groupNames().size();})));
        groupRoot.then(literal("play").then(groupArgument().executes(ctx->playGroup(ctx,false,false))
            .then(literal("at").then(argument("position",Vec3ArgumentType.vec3()).executes(ctx->playGroup(ctx,true,false))))));
        for(String action:List.of("stop","pause","resume"))groupRoot.then(literal(action).then(groupArgument().executes(ctx->run(ctx,"play",()->{
            UUID owner=ParticleFXPermissions.admin(ctx.getSource())?null:owner(ctx);
            int n=manager(ctx).controlGroup(groupName(ctx),owner,action);feedback(ctx,action+" group: "+n+" instance(s)");return n;
        }))));
        root.then(groupRoot);
        var redstoneRoot=literal("redstone");
        redstoneRoot.then(literal("list").executes(ctx->run(ctx,"redstone",()->{
            for(var trigger:manager(ctx).redstoneTriggers())
                feedback(ctx,trigger.targetType+" "+trigger.target+" @ "+trigger.dimension+" "+trigger.x+" "+trigger.y+" "+trigger.z);
            return manager(ctx).redstoneTriggers().size();
        })));
        redstoneRoot.then(literal("remove").then(argument("position",BlockPosArgumentType.blockPos()).executes(ctx->run(ctx,"redstone",()->{
            boolean removed=manager(ctx).removeRedstoneTrigger(ctx.getSource().getWorld(),BlockPosArgumentType.getBlockPos(ctx,"position"));
            if(!removed)throw new IllegalArgumentException("No redstone trigger at that position");
            feedback(ctx,"Redstone trigger removed");return 1;
        }))));
        var redstoneAdd=literal("add");
        var redstonePosition=argument("position",BlockPosArgumentType.blockPos());
        redstonePosition.then(literal("effect").then(argument("name",StringArgumentType.word())
            .suggests((ctx,builder)->CommandSource.suggestMatching(manager(ctx).names(),builder))
            .executes(ctx->run(ctx,"redstone",()->{
                String target=StringArgumentType.getString(ctx,"name");
                manager(ctx).addRedstoneTrigger(ctx.getSource().getWorld(),BlockPosArgumentType.getBlockPos(ctx,"position"),"effect",target);
                feedback(ctx,"Redstone trigger added for effect "+target);return 1;
            }))));
        redstonePosition.then(literal("group").then(argument("name",StringArgumentType.word())
            .suggests((ctx,builder)->CommandSource.suggestMatching(manager(ctx).groupNames(),builder))
            .executes(ctx->run(ctx,"redstone",()->{
                String target=StringArgumentType.getString(ctx,"name");
                manager(ctx).addRedstoneTrigger(ctx.getSource().getWorld(),BlockPosArgumentType.getBlockPos(ctx,"position"),"group",target);
                feedback(ctx,"Redstone trigger added for group "+target);return 1;
            }))));
        redstoneAdd.then(redstonePosition);
        redstoneRoot.then(redstoneAdd);
        root.then(redstoneRoot);
        dispatcher.register(root);
    }
    private static int play(CommandContext<ServerCommandSource> ctx,boolean at,boolean targeted,boolean preview) {return run(ctx,"play",()->{
        Vec3d pos=at?Vec3ArgumentType.getVec3(ctx,"position"):ctx.getSource().getPosition();
        Set<UUID> audience=targeted?EntityArgumentType.getPlayers(ctx,"viewers").stream().map(p->p.getUuid()).collect(Collectors.toSet()):null;
        if(preview)audience=Set.of(ctx.getSource().getPlayerOrThrow().getUuid());
        UUID id=manager(ctx).play(name(ctx),ctx.getSource().getWorld(),pos,owner(ctx),audience,preview?100:0);feedback(ctx,"Started "+name(ctx)+" ("+id+")");return 1;
    });}
    private static int playGroup(CommandContext<ServerCommandSource> ctx,boolean at,boolean ignored) {return run(ctx,"play",()->{
        Vec3d pos=at?Vec3ArgumentType.getVec3(ctx,"position"):ctx.getSource().getPosition();
        UUID id=manager(ctx).playGroup(groupName(ctx),ctx.getSource().getWorld(),pos,owner(ctx),null);
        feedback(ctx,"Started group "+groupName(ctx)+" ("+id+")");return 1;
    });}
    private static RequiredArgumentBuilder<ServerCommandSource,String> effectArgument() {
        return argument("name",StringArgumentType.word()).suggests((ctx,builder)->CommandSource.suggestMatching(manager(ctx).names(),builder));
    }
    private static RequiredArgumentBuilder<ServerCommandSource,String> groupArgument() {
        return argument("name",StringArgumentType.word()).suggests((ctx,builder)->CommandSource.suggestMatching(manager(ctx).groupNames(),builder));
    }
    private static int run(CommandContext<ServerCommandSource> ctx,String permission,Action action) {
        try {ParticleFXPermissions.require(ctx.getSource(),permission);return action.run();}
        catch(Exception e) {ctx.getSource().sendError(Text.literal("ParticleFX: "+Objects.toString(e.getMessage(),e.getClass().getSimpleName())));return 0;}
    }
    private static String name(CommandContext<ServerCommandSource> ctx){return StringArgumentType.getString(ctx,"name");}
    private static String groupName(CommandContext<ServerCommandSource> ctx){return StringArgumentType.getString(ctx,"name");}
    private static UUID owner(CommandContext<ServerCommandSource> ctx){var p=ctx.getSource().getPlayer();return p==null?null:p.getUuid();}
    private static dev.particlefx.server.EffectManager manager(CommandContext<ServerCommandSource> ctx){return ParticleFXServer.get(ctx.getSource().getServer());}
    private static void feedback(CommandContext<ServerCommandSource> ctx,String message){ctx.getSource().sendFeedback(()->Text.literal("ParticleFX: "+message),false);}
    private ParticleFXCommand() {}
}
