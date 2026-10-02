package dev.particlefx;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.particlefx.animation.*;
import dev.particlefx.effect.*;
import dev.particlefx.math.Vec3;
import dev.particlefx.server.*;
import dev.particlefx.shape.*;
import dev.particlefx.storage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @TempDir Path temp;
    @ParameterizedTest @ValueSource(strings={"point","line","circle","ring","sphere","dome","cylinder","cone","helix","spiral","cube","plane","star","heart"})
    void everyShapeIsFiniteAndDeterministic(String id) {
        var shape=ShapeRegistry.get(id);var s=new ShapeSettings();
        for(int n:new int[]{1,2,7,128,5000})for(int i=0;i<n;i++){Vec3 p=shape.sample(i,n,s);assertTrue(p.finite(),id);assertEquals(p,shape.sample(i,n,s));}
    }
    @Test void sphereAndCircleRespectRadius() {
        var s=new ShapeSettings();s.radius=3;
        for(String id:List.of("sphere","circle","dome"))for(int i=0;i<100;i++){Vec3 p=ShapeRegistry.get(id).sample(i,100,s);assertEquals(9,p.x()*p.x()+p.y()*p.y()+p.z()*p.z(),1e-9);}
    }
    @Test void ringStaysInAnnulus() {
        var s=new ShapeSettings();for(int i=0;i<1000;i++){Vec3 p=new RingShape().sample(i,1000,s);double r=Math.hypot(p.x(),p.z());assertTrue(r>=s.innerRadius&&r<=s.radius);}
    }
    @Test void playbackModes() {
        assertEquals(1,PlaybackMode.ONCE.phase(200,100));assertEquals(.5,PlaybackMode.LOOP.phase(150,100));
        assertEquals(.5,PlaybackMode.PING_PONG.phase(150,100));assertEquals(0,PlaybackMode.PING_PONG.phase(200,100));
        assertThrows(IllegalArgumentException.class,()->PlaybackMode.LOOP.phase(0,0));
    }
    @Test void pauseResumeStopAndOnceCleanup() {
        var c=new EffectClock();c.advance(PlaybackMode.ONCE,2);c.pause();c.advance(PlaybackMode.ONCE,2);assertEquals(1,c.age());
        c.resume();c.advance(PlaybackMode.ONCE,2);assertEquals(EffectState.STOPPED,c.state());c.resume();assertEquals(EffectState.STOPPED,c.state());
    }
    @Test void pingPongNeverLeavesValidFrameRange() {
        var c=new EffectClock();List<Long> frames=new ArrayList<>();
        for(int i=0;i<7;i++){frames.add(c.localTick(PlaybackMode.PING_PONG,3));c.advance(PlaybackMode.PING_PONG,3);}
        assertEquals(List.of(0L,1L,2L,1L,0L,1L,2L),frames);assertEquals(0,c.localTick(PlaybackMode.PING_PONG,1));
    }
    @Test void rotationTranslationAndScale() {
        var l=new ParticleLayer();l.rotation=new Vec3(0,90,0);l.scale=new Vec3(2,2,2);l.offset=new Vec3(1,2,3);
        var f=new AnimationFrame();f.evaluate(l,0);double[] out=new double[3];f.transform(new Vec3(1,0,0),out);assertArrayEquals(new double[]{1,2,1},out,1e-9);
    }
    @Test void animatedDensityLifetimeAndSpeed() {
        var l=new ParticleLayer();var density=new Animation();density.type="density";density.from=1;density.to=2;density.mode=PlaybackMode.ONCE;
        var lifetime=new Animation();lifetime.type="lifetime";lifetime.from=.5;lifetime.to=.5;l.animations=List.of(density,lifetime);
        var f=new AnimationFrame();f.evaluate(l,50);assertEquals(1.5,f.density);assertEquals(50,f.lifetime);
        var rotation=new Animation();rotation.speed=2.0;assertEquals(100,rotation.value(50));
    }
    @Test void defaultsAndDefensiveCopy() {
        var e=JsonSupport.GSON.fromJson("{\"name\":\"example\"}",ParticleEffect.class);EffectValidator.validate(e);
        var copy=JsonSupport.copy(e);copy.layers.getFirst().count=3;assertEquals(32,e.layers.getFirst().count);
    }
    @ParameterizedTest @ValueSource(strings={"../outside","CON.","bad/name","UPPER","","x\\y","con","nul","com1","lpt9"})
    void rejectsUnsafeNames(String name) {assertThrows(IllegalArgumentException.class,()->new EffectStorage(temp).path(name));}
    @Test void validatesNumbersAndSchema() {
        var e=new ParticleEffect();e.layers.getFirst().density=Double.NaN;assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(e));
        var invalid=new ParticleEffect();invalid.schemaVersion=2;assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(invalid));
    }
    @Test void customGeometryIsBoundedAndRoundTrips() {
        var e=new ParticleEffect();var s=e.layers.getFirst().shape;
        s.type="custom";s.vertices=new ArrayList<>(List.of(new Vec3(-1,2,0),new Vec3(3,0,4)));
        e.layers.getFirst().count=4;EffectValidator.validate(e);
        var copy=JsonSupport.copy(e);assertEquals(new Vec3(-1,2,0),ShapeRegistry.get("custom").sample(0,4,copy.layers.getFirst().shape));
        assertEquals(new Vec3(3,0,4),ShapeRegistry.get("custom").sample(3,4,copy.layers.getFirst().shape));
        s.vertices.add(new Vec3(Double.NaN,0,0));assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(e));
        s.vertices.removeLast();s.vertices.add(new Vec3(129,0,0));assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(e));
        s.vertices.removeLast();s.vertices.clear();assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(e));
        s.vertices=new ArrayList<>(Collections.nCopies(385,Vec3.ZERO));assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(e));
        s.type="circle";s.vertices=List.of(Vec3.ZERO);assertThrows(IllegalArgumentException.class,()->EffectValidator.validate(e));
    }
    @Test void installsPresetEffectsAndGroupsWithoutResurrectingDeletedEffects() throws Exception {
        var storage=new EffectStorage(temp);storage.initialize();
        var effects=storage.loadEffects();assertEquals(EffectStorage.PRESETS.length,effects.size());storage.loadConfig().validate();
        assertEquals(128,storage.loadConfig().maxViewDistance);
        assertEquals(EffectStorage.GROUP_PRESETS.length,storage.loadGroups(effects.keySet()).size());
        storage.delete("healing");storage.initialize();assertEquals(EffectStorage.PRESETS.length-1,storage.loadEffects().size());
    }
    @Test void migratesOldFireworksWithoutOverwritingCustomPresets() throws Exception {
        Files.createDirectories(temp.resolve("effects"));
        Files.writeString(temp.resolve("config.json"),"{\"maxViewDistance\":64,\"otherModKey\":\"kept\"}");
        String packaged;
        try(var in=CoreTest.class.getResourceAsStream("/data/particlefx/presets/new_year_burst.json")) {
            packaged=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);
        }
        JsonObject old=JsonParser.parseString(packaged).getAsJsonObject();
        old.addProperty("viewDistance",64);
        old.getAsJsonArray("layers").remove(2);
        old.getAsJsonArray("layers").remove(2);
        Files.writeString(temp.resolve("effects/new_year_burst.json"),JsonSupport.GSON.toJson(old));
        JsonObject custom=old.deepCopy();
        custom.addProperty("name","new_year_launch");
        custom.addProperty("myCustomField","unchanged");
        Files.writeString(temp.resolve("effects/new_year_launch.json"),JsonSupport.GSON.toJson(custom));

        var storage=new EffectStorage(temp);storage.initialize();
        JsonObject config=JsonParser.parseString(Files.readString(temp.resolve("config.json"))).getAsJsonObject();
        assertEquals(128,config.get("maxViewDistance").getAsDouble());
        assertEquals("kept",config.get("otherModKey").getAsString());
        JsonObject migrated=JsonParser.parseString(Files.readString(storage.path("new_year_burst"))).getAsJsonObject();
        assertEquals(128,migrated.get("viewDistance").getAsDouble());
        assertEquals(7,migrated.getAsJsonArray("layers").size());
        JsonObject preserved=JsonParser.parseString(Files.readString(storage.path("new_year_launch"))).getAsJsonObject();
        assertEquals("unchanged",preserved.get("myCustomField").getAsString());
        assertEquals(128,preserved.get("viewDistance").getAsDouble());
        storage.initialize();
        assertEquals(preserved,JsonParser.parseString(Files.readString(storage.path("new_year_launch"))).getAsJsonObject());
    }
    @Test void keepsManuallyConfiguredViewDistance() throws Exception {
        Files.writeString(temp.resolve("config.json"),"{\"maxViewDistance\":96}");
        var storage=new EffectStorage(temp);storage.initialize();
        assertEquals(96,storage.loadConfig().maxViewDistance);
    }
    @Test void atomicRoundTripAndFailureLeavesExistingFile() throws Exception {
        var storage=new EffectStorage(temp);storage.initialize();var e=new ParticleEffect();e.name="test";storage.save(e);assertEquals(EffectStorage.PRESETS.length+1,storage.loadEffects().size());
        String before=Files.readString(storage.path("test"));e.layers.getFirst().count=-1;assertThrows(IllegalArgumentException.class,()->storage.save(e));assertEquals(before,Files.readString(storage.path("test")));
        try(var files=Files.list(temp.resolve("effects"))){assertFalse(files.anyMatch(p->p.toString().endsWith(".tmp")));}
    }
    @Test void rejectsMalformedAndMismatchedFiles() throws Exception {
        var storage=new EffectStorage(temp);storage.initialize();Files.writeString(storage.path("wrong"),"{\"name\":\"other\"}");assertThrows(java.io.IOException.class,storage::loadEffects);
        Files.writeString(storage.path("wrong"),"{broken");assertThrows(java.io.IOException.class,storage::loadEffects);
    }
    @Test void rejectsOversizedFiles() throws Exception {
        var storage=new EffectStorage(temp);storage.initialize();Files.writeString(storage.path("huge")," ".repeat(30001));assertThrows(java.io.IOException.class,storage::loadEffects);
    }
    @Test void budgetsBoundParticlesAndPerPlayerPackets() {
        var c=new EngineConfig();c.maxParticlesPerTick=2;c.maxPacketsPerTick=3;c.maxPacketsPerPlayerPerTick=2;
        var b=new EmissionBudget();var p=UUID.randomUUID();var q=UUID.randomUUID();
        assertTrue(b.particle(c));assertTrue(b.particle(c));assertFalse(b.particle(c));
        assertTrue(b.packet(p,c));assertTrue(b.packet(p,c));assertFalse(b.packet(p,c));assertTrue(b.packet(q,c));assertFalse(b.packet(q,c));
        b.reset();assertEquals(0,b.packets());assertTrue(b.packet(p,c));
    }
}
