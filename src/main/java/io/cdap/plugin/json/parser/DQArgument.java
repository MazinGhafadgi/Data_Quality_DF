package io.cdap.plugin.json.parser;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DQArgument {
    private String name;
    private JsonElement value;
    private String type;

    public DQArgument() {
        type = "string";
    }


    public String getValue() {
        return value.getAsString();
    }

}
