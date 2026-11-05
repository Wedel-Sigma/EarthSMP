package io.github.wedelsigma.earthsmp.core;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import static org.junit.jupiter.api.Assertions.*;

/** Prevents YAML errors like an unquoted ':' in description from passing the build. */
class PluginYamlTest {
    @Test void pluginYmlIsParseableAndContainsRequiredKeys() {
        try(InputStream in=getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in,"Missing plugin.yml");
            Object doc=new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
            assertInstanceOf(Map.class,doc);
            Map<?,?> map=(Map<?,?>)doc;
            assertEquals("EarthSMPCore",map.get("name"));
            String expectedVersion = System.getProperty("earthsmp.version");
            assertNotNull(expectedVersion, "Gradle must provide earthsmp.version");
            assertEquals(expectedVersion, String.valueOf(map.get("version")));
            assertEquals("io.github.wedelsigma.earthsmp.core.EarthSmpCorePlugin",map.get("main"));
            assertTrue(((Map<?,?>)map.get("commands")).containsKey("earthsmp"));
        }catch(java.io.IOException ex){fail(ex);}
    }
}
