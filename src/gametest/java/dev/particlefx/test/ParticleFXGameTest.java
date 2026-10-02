package dev.particlefx.test;
import dev.particlefx.api.ParticleFXAPI;
import dev.particlefx.effect.*;
import dev.particlefx.math.Vec3;
import dev.particlefx.network.EditorResponse;
import dev.particlefx.server.*;
import dev.particlefx.shape.ShapeRegistry;
import dev.particlefx.storage.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.entity.EntityType;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.*;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

/** Runs in a real headless Minecraft server; not included in the release jar. */
public final class ParticleFXGameTest {
    private static final class CaptureHandler extends ServerPlayNetworkHandler {
        final List<ParticleS2CPacket> particles=new ArrayList<>();
        CaptureHandler(ServerPlayerEntity player){super(player.getEntityWorld().getServer(),new ClientConnection(NetworkSide.SERVERBOUND),player,ConnectedClientData.createDefault(player.getGameProfile(),false));}
        @Override public void sendPacket(Packet<?> packet){if(packet instanceof ParticleS2CPacket p)particles.add(p);}
    }
    @GameTest(maxTicks=100)
    public void serverEngineIntegration(TestContext ctx) throws Exception {
        var world=ctx.getWorld();var server=world.getServer();var manager=ParticleFXServer.get(server);
        manager.clear();ctx.assertTrue(manager.names().containsAll(List.of(EffectStorage.PRESETS))&&manager.groupNames().containsAll(List.of(EffectStorage.GROUP_PRESETS)),"All preset effects and groups load on a server without Mod Menu/Geyser/client classes");
        var dispatcher=server.getCommandManager().getDispatcher();var source=server.getCommandSource().withWorld(world);
        String testName="integration_test";if(manager.names().contains(testName))manager.delete(testName);
        ctx.assertTrue(dispatcher.execute("particlefx create "+testName,source)==1,"Create command persists a definition");
        ParticleEffect effect=manager.definition(testName);effect.mode=PlaybackMode.LOOP;effect.durationTicks=20;effect.viewDistance=16;
        ParticleLayer layer=effect.layers.getFirst();layer.count=8;layer.intervalTicks=1;layer.lifetime=20;layer.shape.type="point";
        manager.register(effect,true);
        var near=ctx.createMockCreativeServerPlayerInWorld();var far=ctx.createMockCreativeServerPlayerInWorld();
        var originalNear=near.networkHandler;var originalFar=far.networkHandler;
        var nearHandler=new CaptureHandler(near);var farHandler=new CaptureHandler(far);near.networkHandler=nearHandler;far.networkHandler=farHandler;
        Vec3d origin=ctx.getAbsolutePos(new net.minecraft.util.math.BlockPos(2,2,2)).toCenterPos();near.setPosition(origin);far.setPosition(origin.add(100,0,0));
        Path effectDir=FabricLoader.getInstance().getConfigDir().resolve("particlefx/effects");
        try {
            ctx.assertFalse(ServerPlayNetworking.canSend(near,EditorResponse.ID),"Vanilla mock player has no ParticleFX receiver");
            UUID handle=ParticleFXAPI.play(testName,world,origin);manager.tick();
            ctx.assertTrue(nearHandler.particles.size()==8,"Unmodded nearby player receives eight vanilla particle packets");
            ctx.assertTrue(farHandler.particles.isEmpty(),"Distant player receives no particles");
            ctx.assertTrue(nearHandler.particles.stream().allMatch(p->p.getCount()==1&&p.getParameters()==ParticleTypes.END_ROD),"Packets use vanilla simple particles");
            nearHandler.particles.clear();
            UUID burst=ParticleFXAPI.play("new_year_burst",world,origin.add(0,70,0));
            manager.tick();
            ctx.assertTrue(nearHandler.particles.stream().anyMatch(p->p.getY()>origin.y+60),
                "Ground-level observer receives the New Year burst 70 blocks above");
            ParticleFXAPI.stop(server,burst);
            nearHandler.particles.clear();
            farHandler.particles.clear();
            ParticleFXAPI.pause(server,handle);nearHandler.particles.clear();manager.tick();ctx.assertTrue(nearHandler.particles.isEmpty(),"Pause suppresses emission");
            ParticleFXAPI.resume(server,handle);manager.tick();ctx.assertTrue(nearHandler.particles.size()==8,"Resume restores emission");
            ParticleFXAPI.stop(server,handle);ctx.assertTrue(manager.activeCount()==0,"Stop removes the instance");
            nearHandler.particles.clear();far.setPosition(origin);
            handle=ParticleFXAPI.play(testName,world,origin,Set.of(near.getUuid()));manager.tick();
            ctx.assertTrue(nearHandler.particles.size()==8&&farHandler.particles.isEmpty(),"Audience filter excludes a nearby untargeted player");ParticleFXAPI.stop(server,handle);
            var nonOp=near.getCommandSource().withPermissions(LeveledPermissionPredicate.ALL);
            ctx.assertTrue(dispatcher.execute("particlefx create denied_effect",nonOp)==0,"Non-OP cannot create effects");
            ctx.assertTrue(dispatcher.execute("particlefx play "+testName,nonOp)==0,"Default player play permission is denied");
            ctx.assertTrue(dispatcher.execute("particlefx list",nonOp)>0,"Default player list permission is allowed");
            effect.mode=PlaybackMode.ONCE;effect.durationTicks=2;manager.register(effect,false);
            handle=ParticleFXAPI.play(testName,world,origin);manager.tick();manager.tick();ctx.assertTrue(ParticleFXAPI.state(server,handle)==EffectState.STOPPED,"Once finishes without residual task");
            effect.mode=PlaybackMode.LOOP;effect.durationTicks=20;manager.register(effect,true);
            var stand=ctx.spawnEntity(EntityType.ARMOR_STAND,2,2,2);handle=ParticleFXAPI.attach(testName,stand);
            stand.setPosition(origin.add(3,0,0));nearHandler.particles.clear();manager.tick();
            ctx.assertTrue(!nearHandler.particles.isEmpty()&&Math.abs(nearHandler.particles.getFirst().getX()-stand.getX())<1e-8,"Attachment follows entity position");
            ParticleFXAPI.pause(server,handle);stand.discard();manager.tick();ctx.assertTrue(manager.activeCount()==0,"Removed target is cleaned even while paused");
            handle=manager.play(testName,world,origin,near.getUuid(),null,0);manager.forget(near.getUuid());ctx.assertTrue(manager.state(handle)==EffectState.STOPPED,"Disconnect cleanup removes player-owned instances");
            handle=manager.play(testName,world,origin,near.getUuid(),null,0);var nether=server.getWorld(net.minecraft.world.World.NETHER);
            if(nether!=null){near.setServerWorld(nether);manager.tick();ctx.assertTrue(manager.state(handle)==EffectState.STOPPED,"Owner dimension change cleans the instance");near.setServerWorld(world);}manager.clear();
            for(String shape:ShapeRegistry.ids()){
                effect.layers.getFirst().shape.type=shape;
                effect.layers.getFirst().shape.vertices=shape.equals("custom")?new ArrayList<>(List.of(Vec3.ZERO)):new ArrayList<>();
                new CompiledEffect(effect,manager.config());
            }
            layer.shape.type="custom";layer.shape.vertices=new ArrayList<>(List.of(new Vec3(-1,0,0),new Vec3(1,0,0)));layer.count=2;
            manager.register(effect,true);
            nearHandler.particles.clear();handle=ParticleFXAPI.play(testName,world,origin);manager.tick();
            ctx.assertTrue(nearHandler.particles.size()==2
                &&Math.abs(nearHandler.particles.get(0).getX()-(origin.x-1))<1e-8
                &&Math.abs(nearHandler.particles.get(1).getX()-(origin.x+1))<1e-8,
                "Client-baked custom vertices persist and emit as vanilla particles");ParticleFXAPI.stop(server,handle);
            layer.shape.type="point";layer.shape.vertices.clear();layer.count=8;manager.register(effect,true);
            handle=ParticleFXAPI.play(testName,world,origin);
            Path broken=effectDir.resolve("broken_test.json");Files.writeString(broken,"{broken");
            boolean rejected=false;try{manager.reload();}catch(Exception expected){rejected=true;}finally{Files.deleteIfExists(broken);}
            ctx.assertTrue(rejected&&manager.state(handle)==EffectState.PLAYING,"Failed reload preserves valid definitions and running effects");
            manager.reload();ctx.assertTrue(manager.activeCount()==0&&manager.names().contains(testName),"Successful reload clears instances and reloads persisted JSON");
            nearHandler.particles.clear();farHandler.particles.clear();
            manager.config().maxPacketsPerTick=3;manager.config().maxPacketsPerPlayerPerTick=2;
            handle=ParticleFXAPI.play(testName,world,origin);manager.tick();
            ctx.assertTrue(nearHandler.particles.size()<=2&&farHandler.particles.size()<=2&&manager.lastPackets()<=3,"Global and per-player packet budgets are enforced");manager.clear();manager.reload();
            manager.config().bedrockDetection="all";
            // Provider integration is checked independently from a live Geyser installation.
            ParticleFXAPI.registerCompatibilityProvider(server,(viewer,original)->ParticleTypes.FLAME);
            handle=ParticleFXAPI.play(testName,world,origin);nearHandler.particles.clear();manager.tick();
            ctx.assertTrue(!nearHandler.particles.isEmpty()&&nearHandler.particles.getFirst().getParameters()==ParticleTypes.FLAME,"Compatibility provider changes vanilla packet particle");manager.clear();
            ctx.assertTrue(dispatcher.execute("particlefx play "+testName,source)==1,"Play command starts");
            ctx.assertTrue(dispatcher.execute("particlefx pause "+testName,source)==1,"Pause command");
            ctx.assertTrue(dispatcher.execute("particlefx resume "+testName,source)==1,"Resume command");
            ctx.assertTrue(dispatcher.execute("particlefx stop "+testName,source)==1,"Stop command");
            ctx.assertTrue(dispatcher.execute("particlefx delete "+testName,source)==1&&!Files.exists(effectDir.resolve(testName+".json")),"Delete stops instances and removes JSON");
            ctx.assertTrue(manager.activeCount()==0,"No active test instances remain");
            ctx.complete();
        } finally {near.networkHandler=originalNear;far.networkHandler=originalFar;manager.clear();if(manager.names().contains(testName))manager.delete(testName);}
    }
}
