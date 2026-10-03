# Emergency phrase pack review

The ambulance pack in `data/phrase_packs/ambulance/` is the runtime source for the seven Quick Questions. Its manifest fixes the order and each language file supplies the local wording. Both the browser and the backend read this pack; `data/emergency_questions.json` remains a compatibility snapshot and a test checks that it stays in sync.

The pack is ready for team review, but no human or clinical validation is recorded. English source text and Chinese wording are marked pending team review. Russian is explicitly marked **Pending native-speaker validation**. A reviewer should compare the displayed phrase and spoken audio against the intended emergency question, record their name/date and any changes in a follow-up review record, and then update the status. The app must not claim review is complete before that happens.

Quick Questions continue to display a large patient-language sentence and play local Piper audio. They do not use live machine translation or a cloud service.
