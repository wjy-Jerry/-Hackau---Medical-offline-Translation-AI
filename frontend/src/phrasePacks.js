import manifest from '../../data/phrase_packs/ambulance/manifest.json'
import en from '../../data/phrase_packs/ambulance/en.json'
import zh from '../../data/phrase_packs/ambulance/zh.json'
import ru from '../../data/phrase_packs/ambulance/ru.json'

export const ambulancePack = { manifest, languages: { en, zh, ru } }

export const questions = manifest.question_order.map((id) => ({
  id,
  en: en.questions[id],
  zh: zh.questions[id],
  ru: ru.questions[id],
  ru_review_status: ru.review_status,
}))
