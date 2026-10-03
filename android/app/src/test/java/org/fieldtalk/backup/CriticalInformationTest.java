package org.fieldtalk.backup;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class CriticalInformationTest {
    @Test public void englishAllergyAndPainAreExplicit() {
        Map<String, String> facts = CriticalInformation.extract("I am allergic to penicillin and my chest hurts.", "en");
        assertEquals("Penicillin", facts.get("allergy"));
        assertEquals("Chest", facts.get("pain_location"));
        assertEquals("Chest pain", facts.get("symptom"));
    }

    @Test public void breathingMedicationBleedingAndFainting() {
        assertEquals("Difficulty breathing", CriticalInformation.extract("I cannot breathe normally.", "en").get("breathing_difficulty"));
        assertEquals("Medication for asthma", CriticalInformation.extract("I am taking medication for asthma.", "en").get("medication"));
        assertEquals("Bleeding", CriticalInformation.extract("I am bleeding.", "en").get("bleeding"));
        assertEquals("Reported loss of consciousness", CriticalInformation.extract("I lost consciousness.", "en").get("loss_of_consciousness"));
    }

    @Test public void chineseExplicitFacts() {
        Map<String, String> facts = CriticalInformation.extract("我对青霉素过敏，而且胸口很痛。我不能正常呼吸。", "zh");
        assertEquals("青霉素", facts.get("allergy"));
        assertEquals("Chest pain", facts.get("symptom"));
        assertEquals("Difficulty breathing", facts.get("breathing_difficulty"));
    }

    @Test public void russianExplicitFacts() {
        Map<String, String> facts = CriticalInformation.extract("У меня аллергия на пенициллин. У меня болит грудь. Я не могу дышать.", "ru");
        assertEquals("Пенициллин", facts.get("allergy"));
        assertEquals("Chest pain", facts.get("symptom"));
        assertEquals("Difficulty breathing", facts.get("breathing_difficulty"));
    }

    @Test public void questionsNegationAndUncertaintyProduceNoFacts() {
        assertTrue(CriticalInformation.extract("Are you allergic to penicillin?", "en").isEmpty());
        assertTrue(CriticalInformation.extract("I am not allergic to penicillin.", "en").isEmpty());
        assertTrue(CriticalInformation.extract("I do not have trouble breathing.", "en").isEmpty());
        assertTrue(CriticalInformation.extract("I think I might have chest pain.", "en").isEmpty());
        assertTrue(CriticalInformation.extract("He is bleeding.", "en").isEmpty());
        assertTrue(CriticalInformation.extract("我没有出血。", "zh").isEmpty());
        assertTrue(CriticalInformation.extract("I nearly fainted.", "en").isEmpty());
    }

    @Test public void unrelatedSpeechAndUnsupportedLanguageStayEmpty() {
        assertTrue(CriticalInformation.extract("Hello, where is the hospital?", "en").isEmpty());
        assertTrue(CriticalInformation.extract("I am allergic to penicillin", "fr").isEmpty());
        assertTrue(CriticalInformation.extract("", "en").isEmpty());
    }
}
