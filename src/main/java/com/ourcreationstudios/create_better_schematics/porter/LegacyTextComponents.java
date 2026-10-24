package com.ourcreationstudios.create_better_schematics.porter;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Matches 1.20.1's component JSON serializer, including key order for RespectNBT filters. */
public final class LegacyTextComponents {
    private static final List<String> ORDER = List.of("bold", "italic", "underlined", "strikethrough", "obfuscated",
            "color", "insertion", "clickEvent", "hoverEvent", "font", "extra", "text", "translate", "fallback",
            "with", "score", "selector", "keybind", "nbt", "interpret", "separator", "block", "entity", "storage");

    private LegacyTextComponents() {}

    public static String convert(String json) {
        try {
            return component(JsonParser.parseString(json), 0).toString();
        } catch (RuntimeException invalid) {
            return json;
        }
    }

    private static JsonObject component(JsonElement element, int depth) {
        if (depth > 64)
            throw new IllegalArgumentException("Text component is too deeply nested");
        if (element.isJsonPrimitive()) {
            JsonObject text = new JsonObject();
            text.addProperty("text", element.getAsString());
            return text;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonObject first = component(array.get(0), depth + 1);
            JsonArray extra = first.has("extra") ? first.getAsJsonArray("extra") : new JsonArray();
            for (int i = 1; i < array.size(); i++)
                extra.add(component(array.get(i), depth + 1));
            first.add("extra", extra);
            return component(first, depth + 1);
        }
        JsonObject source = element.getAsJsonObject();
        JsonObject result = new JsonObject();
        for (String key : ORDER) {
            if (!source.has(key))
                continue;
            JsonElement value = source.get(key).deepCopy();
            if (key.equals("extra") || key.equals("with")) {
                JsonArray array = new JsonArray();
                for (JsonElement entry : value.getAsJsonArray())
                    array.add(key.equals("with") && entry.isJsonPrimitive() ? entry : component(entry, depth + 1));
                if (array.isEmpty())
                    continue;
                value = array;
            } else if (key.equals("separator")) {
                value = component(value, depth + 1);
            } else if (key.equals("hoverEvent") && value.isJsonObject()) {
                JsonObject hover = value.getAsJsonObject();
                if (hover.has("contents") && hover.get("action").getAsString().equals("show_text"))
                    hover.add("contents", component(hover.get("contents"), depth + 1));
            }
            result.add(key, value);
        }
        return result;
    }
}
