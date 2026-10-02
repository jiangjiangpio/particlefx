package dev.particlefx;

import dev.particlefx.client.gui.FormulaGenerator;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.storage.EffectValidator;
import dev.particlefx.storage.JsonSupport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormulaGeneratorTest {
    @Test void allTemplatesProduceBoundedPoints() {
        for(var preset:FormulaGenerator.PRESETS) {
            var vertices=FormulaGenerator.generate(preset.x(),preset.y(),preset.z(),144,preset.surface());
            assertEquals(144,vertices.size(),preset.id());
            for(var v:vertices) {
                assertTrue(v.finite(),preset.id());
                assertTrue(Math.abs(v.x())<=128&&Math.abs(v.y())<=128&&Math.abs(v.z())<=128,preset.id());
            }
        }
    }
    @Test void supportedFunctionsAndInvalidInputs() {
        var vertices=FormulaGenerator.generate("pow(t,2)","clamp(sin(t)+cos(t),-1,1)","min(2,max(0,abs(t)))+easeout(t)",2,false);
        assertEquals(1,vertices.getLast().x());
        var allFunctions=FormulaGenerator.generate(
            "tan(t)+asin(t)+acos(1)+atan(t)+sqrt(t)+abs(-t)+pow(t,2)",
            "min(t,1)+max(t,0)+clamp(t,0,1)",
            "0",2,false);
        assertTrue(allFunctions.stream().allMatch(dev.particlefx.math.Vec3::finite));
        assertThrows(IllegalArgumentException.class,()->FormulaGenerator.generate("t","0","0",EffectValidator.MAX_CUSTOM_POINTS+1,false));
        assertThrows(RuntimeException.class,()->FormulaGenerator.generate("sqrt(-1)","0","0",2,false));
        assertThrows(RuntimeException.class,()->FormulaGenerator.generate("unknown(t)","0","0",2,false));
        assertThrows(IllegalArgumentException.class,()->FormulaGenerator.generate("129","0","0",2,false));
    }
    @Test void maximumGeneratedGeometryFitsEffectJsonAndReloads() {
        var effect=new ParticleEffect();
        effect.layers.getFirst().shape.type="custom";
        effect.layers.getFirst().shape.vertices=FormulaGenerator.generate("cos(2*pi*t)","sin(2*pi*t)","t",EffectValidator.MAX_CUSTOM_POINTS,false);
        effect.layers.getFirst().count=EffectValidator.MAX_CUSTOM_POINTS;
        EffectValidator.validate(effect);
        var copy=JsonSupport.copy(effect);
        assertEquals(EffectValidator.MAX_CUSTOM_POINTS,copy.layers.getFirst().shape.vertices.size());
    }
}
