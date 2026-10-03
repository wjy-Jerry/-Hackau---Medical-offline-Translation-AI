# FieldTalk: offline communication for high-risk field environments

## One-sentence pitch

FieldTalk helps critical information get through when time, connectivity, and attention are limited.

The MVP is for an ambulance or emergency transport encounter with a conscious patient and a first responder who do not share a language. It is a **communication aid**. It does not diagnose, triage, recommend treatment, or make medical decisions.

## Why the workflow is different

FieldTalk starts with seven locally stored critical questions and large patient-facing prompts. The responder can play a question without waiting for live translation. Yes / No mode needs only a simple patient response. Free Conversation runs local ASR, translation, and speech generation for open statements. When a statement came from the patient, the responder can explicitly add it to an in-memory Handoff Card. Missing facts remain **Unknown / Not stated**. If a model or audio step fails, the interface names the failure and keeps usable text visible.

The ambulance phrase pack is structured for review and extension. **Human review is not yet recorded for the current wording.** Russian phrases are specifically **Pending native-speaker validation**. We should not market the pack as clinically approved or assume that extraction captures every critical detail.

## Positioning beside general-purpose translation

General-purpose translation products are often designed around open conversation. FieldTalk's prototype adds an emergency-specific workflow: predefined questions, large touch targets, explicit uncertainty, patient-stated highlights, and a handoff summary. These are design choices in FieldTalk, not claims that any named competitor lacks a feature. We have not conducted a comparative benchmark of accuracy, speed, offline behavior, or clinical outcomes.

## Product vision: domain packs

The same local communication core could serve other high-risk field environments. A pack could provide reviewed critical phrases, domain vocabulary, information fields, and a task-specific workflow. Candidate packs include:

| Future pack | Example environment | Candidate content to validate with domain experts |
| --- | --- | --- |
| Disaster Response | Earthquake response and relief | Safety, shelter, immediate needs, family contact |
| Search & Rescue | Missing-person and rescue scenes | Location, mobility, hazards, number of people |
| Remote Field | Remote medical or technical field operations | Equipment status, constraints, handoff facts |
| Evacuation | Firefighting and evacuation support | Exit route, smoke exposure, assistance needs |

These are future concepts, not implemented or reviewed packs. Each would need local-language and domain review, usability checks under field conditions, and a deliberately small set of explicit information fields.

## Demonstration claim boundaries

- The current Windows web prototype uses local models after one-time setup and has been technically exercised without non-loopback network access. Physical Airplane Mode and real-microphone field checks remain pending.
- English, Chinese, and Russian conversation paths exist. Russian quality still needs a native speaker's review; Russian structured extraction is limited.
- The Android application is a separate feasibility and implementation phase. Do not present the current browser plus Python backend as a standalone phone app.
