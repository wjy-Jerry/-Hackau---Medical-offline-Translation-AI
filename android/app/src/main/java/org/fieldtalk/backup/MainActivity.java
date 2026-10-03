package org.fieldtalk.backup;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/** Standalone offline emergency phrase workflow; conversation is capability-gated. */
public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(23, 59, 87);
    private static final int INK = Color.rgb(23, 48, 70);
    private static final int MUTED = Color.rgb(82, 107, 125);
    private static final int PALE = Color.rgb(243, 246, 248);
    private static final String[] CODES = {"en", "zh", "ru"};
    private static final String[] NAMES = {"English", "Chinese", "Russian"};
    private static final String[] YES = {"YES", "是", "ДА"};
    private static final String[] NO = {"NO", "不是", "НЕТ"};
    private static final int RECORD_PERMISSION = 10;

    private PhrasePack pack;
    private LinearLayout content;
    private MediaPlayer player;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady;
    private String source = "en";
    private String target = "zh";
    private String currentScreen = "home";
    private String selectedQuestion;
    private String selectedMode;
    private String lastOriginal;
    private String lastTranslation;
    private boolean lastStatementAdded;
    private final ArrayList<PatientStatement> statements = new ArrayList<>();

    private static final class PatientStatement {
        final String original, translation, language;
        PatientStatement(String original, String translation, String language) {
            this.original = original; this.translation = translation; this.language = language;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        try { pack = new PhrasePack(getAssets()); }
        catch (Exception error) {
            TextView failure = new TextView(this);
            failure.setText("Emergency phrase pack unavailable. Reinstall FieldTalk.");
            setContentView(failure);
            return;
        }
        tts = new TextToSpeech(this, status -> ttsReady = status == TextToSpeech.SUCCESS);
        showHome();
    }

    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }

    private GradientDrawable background(int color, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1), border);
        return shape;
    }

    private void screen(String name, String subtitle) {
        currentScreen = name;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PALE);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(20), dp(18), dp(36));
        scroll.addView(content);
        setContentView(scroll);
        if (!"home".equals(name)) button("‹  BACK TO HOME", false, this::showHome);
        label("FieldTalk", 24, INK, true);
        label(subtitle, 13, MUTED, false);
        space(28);
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        content.addView(view, params);
        return view;
    }

    private void space(int height) {
        View blank = new View(this);
        content.addView(blank, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private Button button(String text, boolean primary, Runnable action) {
        Button view = new Button(this);
        view.setText(text); view.setTextSize(16); view.setAllCaps(false);
        view.setTypeface(null, Typeface.BOLD);
        view.setTextColor(primary ? Color.WHITE : NAVY);
        view.setGravity(Gravity.CENTER);
        view.setBackground(background(primary ? NAVY : Color.WHITE, primary ? NAVY : Color.rgb(198, 215, 225)));
        view.setMinHeight(dp(62));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        content.addView(view, params);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void note(String text) {
        TextView view = label(text, 14, MUTED, false);
        view.setPadding(dp(14), dp(13), dp(14), dp(13));
        view.setBackground(background(Color.WHITE, Color.rgb(207, 221, 230)));
    }

    private String languageName(String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return NAMES[i];
        return code;
    }

    private int languageIndex(String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return i;
        return 0;
    }

    private void chooseLanguage(boolean responder) {
        new AlertDialog.Builder(this).setTitle(responder ? "Responder language" : "Patient language")
            .setItems(NAMES, (dialog, which) -> {
                String next = CODES[which];
                if (responder) { if (next.equals(target)) target = source; source = next; }
                else { if (next.equals(source)) source = target; target = next; }
                showHome();
            }).show();
    }

    private void showHome() {
        stopQuestionAudio();
        screen("home", "Offline emergency communication");
        label("Critical information, clearly communicated.", 29, INK, true);
        note("✓ OFFLINE READY · Quick Questions and Handoff work without a network.");
        space(16);
        button("Responder language  ·  " + languageName(source), false, () -> chooseLanguage(true));
        button("Patient language  ·  " + languageName(target), false, () -> chooseLanguage(false));
        button("SWAP LANGUAGES", false, () -> { String old = source; source = target; target = old; showHome(); });
        space(22);
        button("QUICK QUESTIONS\nFast critical communication", true, () -> showQuestions("quick"));
        button("YES / NO\nFor limited patient response", false, () -> showQuestions("yesno"));
        button("FREE CONVERSATION\nDevice models required", false, this::showConversation);
        button("VIEW HANDOFF\nPatient-stated information", false, this::showHandoff);
        if ("ru".equals(source) || "ru".equals(target)) note("Russian wording: Pending native-speaker validation.");
        note("Communication aid only. Confirm critical details with the patient.");
    }

    private void showQuestions(String mode) {
        selectedMode = mode;
        screen(mode, mode.equals("quick") ? "Critical local questions" : "One question at a time");
        label(mode.equals("quick") ? "Choose a question." : "Choose a Yes / No question.", 27, INK, true);
        note("Tap to show and play the patient's language. No live translation is used.");
        if ("ru".equals(target)) note("Russian wording: Pending native-speaker validation.");
        space(10);
        for (String id : pack.order) {
            if (mode.equals("yesno") && "pain".equals(id)) continue;
            button(pack.text(id, source), false, () -> showPatientQuestion(id));
        }
    }

    private void showPatientQuestion(String id) {
        selectedQuestion = id;
        stopQuestionAudio();
        screen("patient-question", selectedMode.equals("quick") ? "Quick question" : "Yes / No");
        label("SHOW TO PATIENT · " + languageName(target).toUpperCase(Locale.ROOT), 13, MUTED, true);
        space(18);
        label(pack.text(id, target), 34, INK, true);
        if ("ru".equals(target)) note("Pending native-speaker validation.");
        space(18);
        TextView playback = label("Playing local question audio…", 14, MUTED, false);
        playQuestion(id, playback);
        button("REPEAT QUESTION", true, () -> playQuestion(id, playback));
        if (selectedMode.equals("yesno")) {
            space(18);
            label("Patient response · tap one", 18, INK, true);
            TextView selected = label("No answer selected", 14, MUTED, false);
            button(YES[languageIndex(target)] + "  /  " + YES[languageIndex(source)], false,
                () -> selected.setText("YES selected on this screen"));
            button(NO[languageIndex(target)] + "  /  " + NO[languageIndex(source)], false,
                () -> selected.setText("NO selected on this screen"));
        }
        button("ASK ANOTHER QUESTION", false, () -> showQuestions(selectedMode));
    }

    private void playQuestion(String id, TextView status) {
        stopQuestionAudio();
        try {
            String path = "audio/" + id + "_" + target + ".wav";
            File local = new File(getCacheDir(), id + "_" + target + ".wav");
            if (!local.isFile()) {
                try (InputStream input = getAssets().open(path); FileOutputStream output = new FileOutputStream(local)) {
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
            }
            player = new MediaPlayer();
            player.setDataSource(local.getAbsolutePath());
            player.setOnCompletionListener(done -> status.setText("Ready to repeat"));
            player.prepare(); player.start();
            status.setText("Playing for patient");
        } catch (Exception error) {
            status.setText("Audio unavailable. Show the written question.");
            stopQuestionAudio();
        }
    }

    private void stopQuestionAudio() {
        if (player != null) { player.release(); player = null; }
    }

    private void showHandoff() {
        screen("handoff", "Emergency handoff");
        label("Patient-stated information.", 29, INK, true);
        note("Only information the responder explicitly added. This is not a diagnosis.");
        label("Patient language: " + (statements.isEmpty() ? "Unknown / Not stated" : languageName(statements.get(statements.size() - 1).language)), 16, INK, true);
        for (String field : new String[]{"Pain location", "Breathing difficulty", "Bleeding", "Loss of consciousness", "Other stated symptom", "Allergy", "Medication"}) {
            label(field.toUpperCase(Locale.ROOT), 12, MUTED, true);
            label("Unknown / Not stated", 18, MUTED, false);
        }
        if (!statements.isEmpty()) {
            space(12);
            label("ADDED PATIENT STATEMENTS", 13, MUTED, true);
            for (PatientStatement statement : statements) {
                label(statement.original, 18, INK, true);
                if (!statement.translation.isEmpty()) label("Translation: " + statement.translation, 15, MUTED, false);
                space(8);
            }
        }
        space(14);
        label("Add a patient statement manually", 17, INK, true);
        EditText input = new EditText(this);
        input.setHint("Patient's exact words"); input.setMinHeight(dp(56));
        content.addView(input, new LinearLayout.LayoutParams(-1, -2));
        button("ADD PATIENT STATEMENT", true, () -> {
            String words = input.getText().toString().trim();
            if (words.isEmpty()) { input.setError("Enter the patient's words"); return; }
            statements.add(new PatientStatement(words, "", target));
            showHandoff();
        });
        button("CLEAR SESSION", false, () -> new AlertDialog.Builder(this)
            .setMessage("Clear all patient-stated information from this session?")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("CLEAR", (dialog, which) -> { statements.clear(); showHandoff(); }).show());
    }

    private void showConversation() {
        screen("conversation", "Free Conversation · local capability check");
        label("Speak, then show the result.", 28, INK, true);
        note("This works only when the device has on-device speech recognition for " + languageName(source)
            + ", downloaded local translation models, and an embedded " + languageName(target) + " voice. No cloud inference is used.");
        button("PREPARE TRANSLATION MODELS · CONNECTED SETUP", false, this::prepareModels);
        button("RECORD PATIENT SPEECH", true, this::startRecognition);
        note("If any stage is missing, use Quick Questions or Yes / No. Russian phrases still need native-speaker review.");
    }

    private void prepareModels() {
        String from = TranslateLanguage.fromLanguageTag(source);
        String to = TranslateLanguage.fromLanguageTag(target);
        if (from == null || to == null) { showError("Unsupported translation language."); return; }
        new AlertDialog.Builder(this)
            .setMessage("Download local translation models over Wi-Fi for " + languageName(source) + " and " + languageName(target) + "? This setup needs a connection; later inference is local.")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("DOWNLOAD", (dialog, which) -> {
                showError("Downloading local translation models…");
                RemoteModelManager manager = RemoteModelManager.getInstance();
                DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
                TranslateRemoteModel sourceModel = new TranslateRemoteModel.Builder(from).build();
                TranslateRemoteModel targetModel = new TranslateRemoteModel.Builder(to).build();
                manager.download(sourceModel, conditions).addOnSuccessListener(ignored ->
                    manager.download(targetModel, conditions).addOnSuccessListener(done ->
                        showError("Local translation models ready. Free Conversation can be tried offline."))
                        .addOnFailureListener(error -> showError("Target model unavailable. Connect to Wi-Fi and retry setup.")))
                    .addOnFailureListener(error -> showError("Source model unavailable. Connect to Wi-Fi and retry setup."));
            }).show();
    }

    private void showError(String message) { note(message); }

    private void startRecognition() {
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            showError("On-device speech recognition unavailable. Use Quick Questions."); return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION); return;
        }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { showError("Listening… Speak one short patient statement."); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { showError("Recognizing locally…"); }
            @Override public void onError(int error) { showError("Speech unclear or on-device language unavailable. Ask the patient to repeat."); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> recognized = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (recognized == null || recognized.isEmpty() || recognized.get(0).trim().isEmpty()) {
                    showError("Speech unclear. Ask the patient to repeat."); return;
                }
                translateLocally(recognized.get(0).trim());
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, source.equals("zh") ? "zh-CN" : source.equals("ru") ? "ru-RU" : "en-US");
        recognizer.startListening(intent);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == RECORD_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startRecognition();
            else showError("Microphone permission denied. Use Quick Questions.");
        }
    }

    private void translateLocally(String original) {
        String from = TranslateLanguage.fromLanguageTag(source);
        String to = TranslateLanguage.fromLanguageTag(target);
        if (from == null || to == null) { showError("Unsupported translation language."); return; }
        showError("Checking installed translation models…");
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class)
            .addOnSuccessListener(models -> {
                boolean haveSource = false, haveTarget = false;
                for (TranslateRemoteModel model : models) {
                    if (from.equals(model.getLanguage())) haveSource = true;
                    if (to.equals(model.getLanguage())) haveTarget = true;
                }
                if (!haveSource || !haveTarget) {
                    showError("Translation unavailable offline. Prepare both local models while connected. Original: " + original);
                    return;
                }
                TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build();
                Translator translator = Translation.getClient(options);
                showError("Translating locally…");
                translator.translate(original).addOnSuccessListener(translated -> {
                    translator.close();
                    showConversationResult(original, translated);
                }).addOnFailureListener(error -> {
                    translator.close();
                    showError("Translation unavailable. Original: " + original);
                });
            }).addOnFailureListener(error -> showError("Cannot verify local translation models. Original: " + original));
    }

    private void showConversationResult(String original, String translated) {
        lastOriginal = original; lastTranslation = translated; lastStatementAdded = false;
        screen("result", "Free Conversation · patient-stated result");
        label("ORIGINAL · " + languageName(source).toUpperCase(Locale.ROOT), 13, MUTED, true);
        label(original, 22, INK, true);
        space(14);
        label("TRANSLATION · " + languageName(target).toUpperCase(Locale.ROOT), 13, MUTED, true);
        label(translated, 30, INK, true);
        note("Important information extraction is not available in this Android prototype. Confirm the original and translation with the patient.");
        boolean spoken = speakOffline(translated, target);
        if (!spoken) note("Audio unavailable. The translated text remains visible.");
        button("PLAY TRANSLATION", true, () -> {
            if (!speakOffline(lastTranslation, target)) showError("Embedded voice unavailable. Read the translation on screen.");
        });
        Button add = button("ADD PATIENT STATEMENT", false, () -> {
            if (!lastStatementAdded) {
                statements.add(new PatientStatement(lastOriginal, lastTranslation, source));
                lastStatementAdded = true;
                showError("Added to the in-memory Handoff Card.");
            }
        });
        button("VIEW HANDOFF", false, this::showHandoff);
        button("RECORD AGAIN", false, this::showConversation);
    }

    private boolean speakOffline(String text, String language) {
        if (!ttsReady) return false;
        Set<Voice> voices = tts.getVoices();
        if (voices == null) return false;
        for (Voice voice : voices) {
            if (!voice.isNetworkConnectionRequired() && language.equals(voice.getLocale().getLanguage())) {
                if (tts.setVoice(voice) != TextToSpeech.SUCCESS) return false;
                return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fieldtalk-translation") == TextToSpeech.SUCCESS;
            }
        }
        return false;
    }

    @Override public void onBackPressed() { showHome(); }

    @Override protected void onDestroy() {
        stopQuestionAudio();
        if (recognizer != null) recognizer.destroy();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
