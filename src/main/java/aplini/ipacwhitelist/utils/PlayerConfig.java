package aplini.ipacwhitelist.utils;

import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

public class PlayerConfig {

    public String yamlString = "";
    public Map<String, Object> data = new HashMap<>();


    public PlayerConfig setYamlStr(String str){
        this.yamlString = str;
        if(!this.yamlString.isEmpty()){
            this.data = new Yaml().load(this.yamlString);
        }
        return this;
    }

    public PlayerConfig putAll(Map<String, Object> map){
        this.data.putAll(map);
        return this;
    }

    public String getYamlStr(){
        if(data.isEmpty()){
            return "";
        }
        return new Yaml().dump(this.data);
    }
}
