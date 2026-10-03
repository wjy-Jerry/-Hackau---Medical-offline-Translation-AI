package org.fieldtalk.backup;

import android.content.res.AssetManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The same local ambulance phrase pack used by the web fallback. */
final class PhrasePack {
    final List<String> order = new ArrayList<>();
    final Map<String, Map<String, String>> phrases = new HashMap<>();

    PhrasePack(AssetManager assets) throws Exception {
        JSONObject manifest = read(assets, "ambulance/manifest.json");
        JSONArray ids = manifest.getJSONArray("question_order");
        for (int i = 0; i < ids.length(); i++) order.add(ids.getString(i));
        for (String language : new String[]{"en", "zh", "ru"}) {
            JSONObject file = read(assets, "ambulance/" + language + ".json");
            if (!language.equals(file.getString("language"))) throw new IllegalStateException("Wrong language pack");
            JSONObject questions = file.getJSONObject("questions");
            Map<String, String> values = new HashMap<>();
            for (String id : order) {
                String wording = questions.getString(id).trim();
                if (wording.isEmpty()) throw new IllegalStateException("Blank phrase: " + id);
                values.put(id, wording);
            }
            phrases.put(language, values);
        }
    }

    String text(String id, String language) { return phrases.get(language).get(id); }

    private static JSONObject read(AssetManager assets, String path) throws Exception {
        try (InputStream input = assets.open(path)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
