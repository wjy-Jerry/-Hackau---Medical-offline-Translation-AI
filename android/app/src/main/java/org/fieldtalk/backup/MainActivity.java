package org.fieldtalk.backup;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService speechWorker = Executors.newSingleThreadExecutor();
    private volatile long modelContext;
    private volatile boolean modelLoading;
    private volatile boolean recording;
    private Button recordButton;
    private int screenVersion;
    private TextToSpeech tts;
    private boolean ttsReady;
    private String source = "en";
    private String target = "zh";
    private String currentScreen = "home";
    private String selectedQuestion;
    private String selectedMode;
    private String lastOriginal;
    private String lastTranslation;
    private Map<String, String> lastFacts = new LinkedHashMap<>();
    private boolean recordingPatient = true;
    private boolean lastStatementAdded;
    private final ArrayList<PatientStatement> statements = new ArrayList<>();
    private String asrState = "Loading", translationState = "Loading", ttsState = "Loading";
    private String systemMessage = "Checking local capabilities…";
    private TextView asrStatusView, translationStatusView, ttsStatusView, systemMessageView;

    private static final class PatientStatement {
        final String original, translation, language;
        final Map<String, String> facts;
        PatientStatement(String original, String translation, String language) {
            this.original = original; this.translation = translation; this.language = language;
            this.facts = CriticalInformation.extract(original, language);
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
        tts = new TextToSpeech(this, status -> runOnUiThread(() -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            refreshTtsStatus();
        }));
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
        screenVersion++;
        if (!"conversation".equals(name)) recording = false;
        currentScreen = name;
        asrStatusView = translationStatusView = ttsStatusView = systemMessageView = null;
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

    private void systemStatusArea() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(10));
        panel.setBackground(background(Color.WHITE, Color.rgb(207, 221, 230)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(20);
        content.addView(panel, params);
        TextView heading = new TextView(this);
        heading.setText("LOCAL MODEL STATUS");
        heading.setTextColor(MUTED);
        heading.setTextSize(12);
        heading.setTypeface(null, Typeface.BOLD);
        panel.addView(heading);
        asrStatusView = statusRow(panel, "ASR");
        translationStatusView = statusRow(panel, "Translation");
        ttsStatusView = statusRow(panel, "TTS");
        systemMessageView = new TextView(this);
        systemMessageView.setTextColor(MUTED);
        systemMessageView.setTextSize(13);
        systemMessageView.setPadding(0, dp(8), 0, 0);
        panel.addView(systemMessageView);
        renderSystemStatus();
    }

    private TextView statusRow(LinearLayout panel, String title) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView name = new TextView(this);
        name.setText(title); name.setTextSize(15); name.setTextColor(INK);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView state = new TextView(this);
        state.setTextSize(15); state.setTypeface(null, Typeface.BOLD);
        row.addView(state);
        panel.addView(row);
        return state;
    }

    private void renderSystemStatus() {
        if (asrStatusView == null) return;
        setStatusText(asrStatusView, asrState);
        setStatusText(translationStatusView, translationState);
        setStatusText(ttsStatusView, ttsState);
        systemMessageView.setText(systemMessage);
    }

    private void setStatusText(TextView view, String state) {
        view.setText(state);
        view.setTextColor("Ready".equals(state) ? Color.rgb(25, 112, 71) : MUTED);
    }

    private void refreshTtsStatus() {
        ttsState = "Error";
        if (ttsReady && tts != null) {
            Set<Voice> voices = tts.getVoices();
            if (voices != null) for (Voice voice : voices) {
                if (!voice.isNetworkConnectionRequired() && conversationTo().equals(voice.getLocale().getLanguage())) {
                    ttsState = "Ready";
                    break;
                }
            }
        }
        renderSystemStatus();
    }

    private void refreshTranslationStatus() {
        String from = TranslateLanguage.fromLanguageTag(source);
        String to = TranslateLanguage.fromLanguageTag(target);
        translationState = "Loading";
        renderSystemStatus();
        if (from == null || to == null) { translationState = "Error"; renderSystemStatus(); return; }
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class)
            .addOnSuccessListener(models -> {
                boolean haveSource = false, haveTarget = false;
                for (TranslateRemoteModel model : models) {
                    if (from.equals(model.getLanguage())) haveSource = true;
                    if (to.equals(model.getLanguage())) haveTarget = true;
                }
                translationState = haveSource && haveTarget ? "Ready" : "Error";
                renderSystemStatus();
            }).addOnFailureListener(error -> { translationState = "Error"; renderSystemStatus(); });
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

    private String conversationFrom() { return recordingPatient ? target : source; }
    private String conversationTo() { return recordingPatient ? source : target; }

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
        label("PATIENT-STATED INFORMATION", 27, INK, true);
        note("Only information the responder explicitly added. This is not a diagnosis.");
        label("Patient language: " + (statements.isEmpty() ? "Unknown / Not stated" : languageName(statements.get(statements.size() - 1).language)), 16, INK, true);
        Map<String, String> confirmed = new LinkedHashMap<>();
        for (PatientStatement statement : statements) {
            for (Map.Entry<String, String> entry : statement.facts.entrySet()) {
                if (!confirmed.containsKey(entry.getKey())) confirmed.put(entry.getKey(), entry.getValue());
                else if (!confirmed.get(entry.getKey()).contains(entry.getValue()))
                    confirmed.put(entry.getKey(), confirmed.get(entry.getKey()) + ", " + entry.getValue());
            }
        }
        for (String field : new String[]{"allergy", "medication", "pain_location", "symptom", "breathing_difficulty", "bleeding", "loss_of_consciousness"}) {
            label(CriticalInformation.label(field).toUpperCase(Locale.ROOT), 12, MUTED, true);
            label(confirmed.getOrDefault(field, "Unknown / Not stated"), 18,
                confirmed.containsKey(field) ? INK : MUTED, false);
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
        recording = false;
        screen("conversation", "Free Conversation · local capability check");
        label("Speak in " + languageName(conversationFrom()) + ".", 28, INK, true);
        label(recordingPatient ? "Patient → Responder" : "Responder → Patient", 15, MUTED, true);
        systemStatusArea();
        asrState = modelContext != 0 ? "Ready" : "Loading";
        systemMessage = modelContext != 0 ? "Ready to record a short statement." : "Loading bundled speech model…";
        refreshTtsStatus();
        refreshTranslationStatus();
        renderSystemStatus();
        prepareAsr();
        button("SWITCH SPEAKER / DIRECTION", false, () -> {
            recordingPatient = !recordingPatient;
            showConversation();
        });
        button("PREPARE TRANSLATION MODELS · CONNECTED SETUP", false, this::prepareModels);
        recordButton = button(recordingPatient ? "RECORD PATIENT SPEECH" : "RECORD RESPONDER SPEECH", true, this::startRecognition);
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
                translationState = "Loading";
                showError("Downloading local translation models…");
                RemoteModelManager manager = RemoteModelManager.getInstance();
                DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
                TranslateRemoteModel sourceModel = new TranslateRemoteModel.Builder(from).build();
                TranslateRemoteModel targetModel = new TranslateRemoteModel.Builder(to).build();
                manager.download(sourceModel, conditions).addOnSuccessListener(ignored ->
                    manager.download(targetModel, conditions).addOnSuccessListener(done ->
                        { translationState = "Ready"; showError("Local translation models ready. Free Conversation can be tried offline."); })
                        .addOnFailureListener(error -> { translationState = "Error"; showError("Target model unavailable. Connect to Wi-Fi and retry setup."); }))
                    .addOnFailureListener(error -> { translationState = "Error"; showError("Source model unavailable. Connect to Wi-Fi and retry setup."); });
            }).show();
    }

    private void showError(String message) { systemMessage = message; renderSystemStatus(); }

    private void prepareAsr() {
        if (modelContext != 0 || modelLoading) return;
        modelLoading = true;
        asrState = "Loading";
        renderSystemStatus();
        speechWorker.execute(() -> {
            long loaded = 0;
            try { loaded = WhisperBridge.nativeLoad(verifiedModelPath()); }
            catch (Exception | LinkageError error) { android.util.Log.e("FieldTalk", "Local ASR initialization failed", error); }
            modelContext = loaded;
            modelLoading = false;
            runOnUiThread(() -> {
                asrState = modelContext != 0 ? "Ready" : "Error";
                showError(modelContext != 0 ? "Local speech model ready." : "Local speech model unavailable. Use Quick Questions.");
            });
        });
    }

    private String verifiedModelPath() throws Exception {
        final String name = "ggml-base-q5_1.bin";
        final String expected = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898";
        File model = new File(getFilesDir(), name);
        if (model.isFile() && expected.equals(sha256(model))) return model.getAbsolutePath();
        File partial = new File(getFilesDir(), name + ".part");
        try (InputStream input = getAssets().open("models/" + name);
             FileOutputStream output = new FileOutputStream(partial)) {
            byte[] buffer = new byte[32768]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        if (!expected.equals(sha256(partial))) throw new IOException("Bundled speech model checksum mismatch");
        if (model.exists() && !model.delete()) throw new IOException("Cannot replace old speech model");
        if (!partial.renameTo(model)) throw new IOException("Cannot install bundled speech model");
        return model.getAbsolutePath();
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[32768]; int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return hex.toString();
    }

    private void startRecognition() {
        if (recording) {
            recording = false;
            showError("Recognizing locally…");
            return;
        }
        if (modelContext == 0) {
            showError(modelLoading ? "Speech model is loading. Please wait." : "Local speech model unavailable. Use Quick Questions.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION); return;
        }
        recording = true;
        final String spokenLanguage = conversationFrom();
        final Button button = recordButton;
        final int requestScreen = screenVersion;
        button.setText("STOP RECORDING");
        asrState = "Loading";
        showError("Listening… Tap Stop Recording when the patient finishes.");
        speechWorker.execute(() -> {
            float[] samples = captureSpeech();
            runOnUiThread(() -> {
                button.setText(recordingPatient ? "RECORD PATIENT SPEECH" : "RECORD RESPONDER SPEECH");
                button.setEnabled(false);
                if (samples != null && requestScreen == screenVersion) showError("Recognizing locally…");
            });
            String text = null;
            try {
                if (samples != null) text = WhisperBridge.nativeTranscribe(modelContext, samples, spokenLanguage);
            } catch (Exception | LinkageError error) {
                android.util.Log.e("FieldTalk", "Local transcription failed", error);
            }
            final String result = text == null ? "" : text.trim();
            runOnUiThread(() -> {
                button.setEnabled(true);
                if (requestScreen != screenVersion) return;
                if (result.isEmpty()) { asrState = "Error"; showError("Speech could not be recognized. Please repeat."); return; }
                asrState = "Ready";
                translateLocally(result);
            });
        });
    }

    private float[] captureSpeech() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recording = false;
            return null;
        }
        final int sampleRate = 16000;
        int minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuffer <= 0) { recording = false; return null; }
        AudioRecord microphone = null;
        try {
            microphone = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBuffer, 4096));
            if (microphone.getState() != AudioRecord.STATE_INITIALIZED) return null;
            float[] samples = new float[12 * sampleRate];
            short[] chunk = new short[2048];
            int used = 0; double energy = 0;
            microphone.startRecording();
            while (recording && used < samples.length) {
                int count = microphone.read(chunk, 0, Math.min(chunk.length, samples.length - used));
                if (count <= 0) break;
                for (int i = 0; i < count; i++) {
                    float value = chunk[i] / 32768f;
                    samples[used++] = value;
                    energy += value * value;
                }
            }
            if (used < sampleRate || Math.sqrt(energy / used) < 0.002) return null;
            return Arrays.copyOf(samples, used);
        } catch (RuntimeException error) {
            android.util.Log.e("FieldTalk", "Microphone capture failed", error);
            return null;
        } finally {
            recording = false;
            if (microphone != null) {
                try { microphone.stop(); } catch (IllegalStateException ignored) { }
                microphone.release();
            }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == RECORD_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startRecognition();
            else showError("Microphone permission denied. Use Quick Questions.");
        }
    }

    private void translateLocally(String original) {
        final int requestScreen = screenVersion;
        String from = TranslateLanguage.fromLanguageTag(conversationFrom());
        String to = TranslateLanguage.fromLanguageTag(conversationTo());
        if (from == null || to == null) { showError("Unsupported translation language."); return; }
        translationState = "Loading";
        showError("Checking installed translation models…");
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class)
            .addOnSuccessListener(models -> {
                if (requestScreen != screenVersion) return;
                boolean haveSource = false, haveTarget = false;
                for (TranslateRemoteModel model : models) {
                    if (from.equals(model.getLanguage())) haveSource = true;
                    if (to.equals(model.getLanguage())) haveTarget = true;
                }
                if (!haveSource || !haveTarget) {
                    translationState = "Error";
                    showOriginalOnly(original, "Translation unavailable offline. Prepare both local models while connected.");
                    return;
                }
                TranslatorOptions options = new TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build();
                Translator translator = Translation.getClient(options);
                showError("Translating locally…");
                translator.translate(original).addOnSuccessListener(translated -> {
                    translator.close();
                    if (requestScreen != screenVersion) return;
                    translationState = "Ready";
                    showConversationResult(original, translated);
                }).addOnFailureListener(error -> {
                    translator.close();
                    if (requestScreen != screenVersion) return;
                    translationState = "Error";
                    showOriginalOnly(original, "Translation unavailable. The original remains visible.");
                });
            }).addOnFailureListener(error -> {
                if (requestScreen != screenVersion) return;
                translationState = "Error";
                showOriginalOnly(original, "Cannot verify local translation models.");
            });
    }

    private void showOriginalOnly(String original, String message) {
        screen("result", "Free Conversation · translation unavailable");
        systemStatusArea();
        showError(message);
        label("ORIGINAL · " + languageName(conversationFrom()).toUpperCase(Locale.ROOT), 13, MUTED, true);
        label(original, 22, INK, true);
        button("RECORD AGAIN", false, this::showConversation);
    }

    private void showConversationResult(String original, String translated) {
        lastOriginal = original; lastTranslation = translated; lastStatementAdded = false;
        lastFacts = recordingPatient ? CriticalInformation.extract(original, conversationFrom()) : new LinkedHashMap<>();
        screen("result", recordingPatient ? "Free Conversation · patient-stated result" : "Free Conversation · responder message");
        systemStatusArea();
        label("ORIGINAL · " + languageName(conversationFrom()).toUpperCase(Locale.ROOT), 13, MUTED, true);
        label(original, 22, INK, true);
        space(14);
        label("TRANSLATION · " + languageName(conversationTo()).toUpperCase(Locale.ROOT), 13, MUTED, true);
        label(translated, 30, INK, true);
        if (!lastFacts.isEmpty()) {
            space(12);
            label("CRITICAL INFORMATION · VERIFY WITH PATIENT", 13, MUTED, true);
            for (Map.Entry<String, String> fact : lastFacts.entrySet()) {
                label(CriticalInformation.label(fact.getKey()).toUpperCase(Locale.ROOT), 12, MUTED, true);
                label(fact.getValue(), 20, INK, true);
            }
        }
        showError(recordingPatient ? "Confirm the original and translation with the patient." : "Check the message and translation before playback.");
        boolean spoken = speakOffline(translated, conversationTo());
        if (!spoken) { ttsState = "Error"; showError("Audio unavailable. The translated text remains visible."); }
        button("PLAY TRANSLATION", true, () -> {
            if (!speakOffline(lastTranslation, conversationTo())) showError("Embedded voice unavailable. Read the translation on screen.");
        });
        if (recordingPatient) {
            Button add = button("ADD TO HANDOFF · CONFIRM PATIENT WORDS", false, () -> {
                if (!lastStatementAdded) {
                    statements.add(new PatientStatement(lastOriginal, lastTranslation, conversationFrom()));
                    lastStatementAdded = true;
                    showError("Patient statement added to Handoff.");
                }
            });
        }
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
        recording = false;
        speechWorker.execute(() -> { if (modelContext != 0) WhisperBridge.nativeClose(modelContext); });
        speechWorker.shutdown();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
