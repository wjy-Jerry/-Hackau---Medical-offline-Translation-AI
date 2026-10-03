package org.fieldtalk.backup;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative highlighting of details in the patient's own words; never a diagnosis. */
final class CriticalInformation {
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern UNCERTAIN = Pattern.compile(
        "\\b(?:he|she|they|you|if|maybe|perhaps|might|possibly|think|suspect|almost|nearly|used to|stopped|no longer|my mother|my father|my friend|my child|он|она|они|ты|вы|если|возможно|раньше)\\b|"
        + "如果|可能|也许|也許|差点|差點|你|他|她|他们|他們", FLAGS);
    private static final Pattern NEGATIVE = Pattern.compile(
        "\\b(?:no|not|never|without|deny|denies|don't|doesn't|didn't|isn't|haven't|do not|does not|have no|нет|не|без)\\b|没有|沒有|没|沒|不|无|無|否认|否認", FLAGS);
    private static final Pattern ALLERGY_EN = Pattern.compile("\\ballergic to\\s+([a-z][a-z -]{0,35})", FLAGS);
    private static final Pattern ALLERGY_ZH = Pattern.compile("[对對]([\\p{IsHan}]{1,12})[过過]敏");
    private static final Pattern ALLERGY_RU = Pattern.compile("(?:у меня )?аллергия на\\s+([\\p{IsCyrillic} -]{2,35})", FLAGS);
    private static final Pattern MED_EN = Pattern.compile("\\b(?:i\\s+(?:am\\s+)?|i'm\\s+)?(?:taking|take|using|use)\\s+(?!no\\b)(medication(?:\\s+for\\s+[a-z -]{1,25})?|medicine|pills?|metformin|insulin|aspirin|warfarin|albuterol|penicillin)\\b", FLAGS);
    private static final Pattern MED_ZH = Pattern.compile("我(?:正在|在)?(?:服用|吃|用)([\\p{IsHan}]{1,14})");
    private static final Pattern MED_RU = Pattern.compile("(?:я\\s+)?принимаю\\s+([\\p{IsCyrillic} -]{2,35})", FLAGS);
    private static final String LOC_EN = "left arm|right arm|left leg|right leg|chest|head|stomach|abdomen|back|neck|arm|leg";
    private static final Pattern PAIN_EN_1 = Pattern.compile("\\b(?:my\\s+)?(" + LOC_EN + ")\\s+(?:hurts|aches|is painful|is hurting|pain)\\b", FLAGS);
    private static final Pattern PAIN_EN_2 = Pattern.compile("\\b(?:pain|ache)\\s+(?:in|on)\\s+(?:my\\s+|the\\s+)?(" + LOC_EN + ")\\b", FLAGS);
    private static final Pattern PAIN_ZH = Pattern.compile("(胸口|胸部|胸|头|頭|肚子|腹部|背部|背|颈部|頸部|左手臂|右手臂|左臂|右臂|手臂|左腿|右腿|腿)(?:很|非常|有点|有點|有|偶)?(?:疼|痛)");
    private static final Pattern PAIN_RU = Pattern.compile("(?:болит\\s+(грудь|голова|живот|спина|шея|рука|нога)|боль\\s+в\\s+(груди|голове|животе|спине|шее|руке|ноге))", FLAGS);
    private static final Pattern BREATH_EN = Pattern.compile("\\b(?:can'?t|cannot|unable to)\\s+breathe\\b|\\b(?:difficulty|trouble)\\s+breathing\\b|\\bshort(?:ness)? of breath\\b", FLAGS);
    private static final Pattern BREATH_ZH = Pattern.compile("(?:不能|无法|無法|难以|難以)(?:正常)?呼吸|呼吸困难|呼吸困難|喘不过气|喘不過氣");
    private static final Pattern BREATH_RU = Pattern.compile("не могу дышать|трудно дышать|тяжело дышать|затруднен[оы]? дыхание", FLAGS);
    private static final Pattern BLEED_EN = Pattern.compile("\\bbleed(?:ing|s)?\\b", FLAGS);
    private static final Pattern BLEED_ZH = Pattern.compile("流血|出血");
    private static final Pattern BLEED_RU = Pattern.compile("кровотечение|истекаю кровью|идет кровь", FLAGS);
    private static final Pattern FAINT_EN = Pattern.compile("\\b(?:fainted|passed out|blacked out|lost consciousness)\\b", FLAGS);
    private static final Pattern FAINT_ZH = Pattern.compile("晕倒|暈倒|昏倒|失去意识|失去意識|昏过去|昏過去");
    private static final Pattern FAINT_RU = Pattern.compile("потерял[а]? сознание|упал[а]? в обморок", FLAGS);

    private CriticalInformation() { }

    static Map<String, String> extract(String text, String language) {
        Map<String, String> facts = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty() || !("en".equals(language) || "zh".equals(language) || "ru".equals(language))) return facts;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).replace('’', '\'');
        for (String sentence : normalized.split("(?<=[.!?。！？;；])|[,，]")) {
            if (sentence.contains("?") || sentence.contains("？")) continue;
            for (String clause : sentence.split("(?i)\\b(?:and|but|и|но)\\b|而且|但是|并且|还有|還有")) {
                clause = clause.trim();
                if (clause.isEmpty() || UNCERTAIN.matcher(clause).find()) continue;
                Matcher match = first(clause, ALLERGY_EN, ALLERGY_ZH, ALLERGY_RU);
                if (match != null && !negatedBefore(clause, match.start())) add(facts, "allergy", tidy(match.group(1)));
                match = first(clause, MED_EN, MED_ZH, MED_RU);
                if (match != null && !negatedBefore(clause, match.start())) add(facts, "medication", tidy(match.group(1)));
                match = first(clause, PAIN_EN_1, PAIN_EN_2, PAIN_ZH, PAIN_RU);
                if (match != null && !negatedBefore(clause, match.start())) {
                    String location = match.group(1) != null ? match.group(1) : match.group(2);
                    String locationName = locationName(location);
                    if (locationName != null) {
                        add(facts, "pain_location", locationName);
                        add(facts, "symptom", locationName + " pain");
                    }
                }
                match = first(clause, BREATH_EN, BREATH_ZH, BREATH_RU);
                if (match != null && (explicitCannot(match.group()) || !negatedBefore(clause, match.start()))) add(facts, "breathing_difficulty", "Difficulty breathing");
                match = first(clause, BLEED_EN, BLEED_ZH, BLEED_RU);
                if (match != null && !negatedBefore(clause, match.start())) add(facts, "bleeding", "Bleeding");
                match = first(clause, FAINT_EN, FAINT_ZH, FAINT_RU);
                if (match != null && !negatedBefore(clause, match.start())) add(facts, "loss_of_consciousness", "Reported loss of consciousness");
            }
        }
        return facts;
    }

    private static Matcher first(String clause, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher match = pattern.matcher(clause);
            if (match.find()) return match;
        }
        return null;
    }

    private static boolean negatedBefore(String clause, int start) {
        return NEGATIVE.matcher(clause.substring(Math.max(0, start - 35), start)).find();
    }

    private static boolean explicitCannot(String phrase) {
        return Pattern.compile("can'?t|cannot|unable to|不能|无法|無法|难以|難以|喘不|не могу", FLAGS).matcher(phrase).find();
    }

    private static String tidy(String value) {
        if (value == null) return "";
        String cleaned = value.trim().replaceAll("(?i)\\b(?:because|since|when|after|which|that)\\b.*$", "").trim();
        if (cleaned.isEmpty() || cleaned.matches("(?i)no|none|not|unknown")) return "";
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private static String locationName(String raw) {
        if (raw == null) return null;
        switch (raw.toLowerCase(Locale.ROOT)) {
            case "chest": case "胸口": case "胸部": case "胸": case "грудь": case "груди": return "Chest";
            case "head": case "头": case "頭": case "голова": case "голове": return "Head";
            case "stomach": case "肚子": case "живот": case "животе": return "Stomach";
            case "abdomen": case "腹部": return "Abdomen";
            case "back": case "背部": case "背": case "спина": case "спине": return "Back";
            case "neck": case "颈部": case "頸部": case "шея": case "шее": return "Neck";
            case "left arm": case "左手臂": case "左臂": return "Left arm";
            case "right arm": case "右手臂": case "右臂": return "Right arm";
            case "arm": case "手臂": case "рука": case "руке": return "Arm";
            case "left leg": case "左腿": return "Left leg";
            case "right leg": case "右腿": return "Right leg";
            case "leg": case "腿": case "нога": case "ноге": return "Leg";
            default: return null;
        }
    }

    private static void add(Map<String, String> facts, String key, String value) {
        if (value == null || value.isEmpty()) return;
        String old = facts.get(key);
        if (old == null) facts.put(key, value);
        else if (!old.contains(value)) facts.put(key, old + ", " + value);
    }

    static String label(String key) {
        switch (key) {
            case "allergy": return "Allergy";
            case "medication": return "Medication";
            case "pain_location": return "Pain location";
            case "symptom": return "Reported symptom";
            case "breathing_difficulty": return "Breathing difficulty";
            case "bleeding": return "Bleeding";
            case "loss_of_consciousness": return "Loss of consciousness";
            default: return key;
        }
    }
}
