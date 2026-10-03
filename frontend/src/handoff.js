export const HANDOFF_FIELDS = [
  'symptom', 'breathing_difficulty', 'bleeding', 'loss_of_consciousness',
  'other_symptom', 'allergy', 'medication',
]

export function addPatientStatement(statements, result, patientLanguage, now = new Date()) {
  const original = typeof result?.original_text === 'string' ? result.original_text.trim() : ''
  if (!original) return statements
  const facts = {}
  for (const key of HANDOFF_FIELDS) {
    const value = result.key_information?.[key]
    if (typeof value === 'string' && value.trim()) facts[key] = value.trim()
  }
  return [...statements, {
    original,
    translation: typeof result.translation === 'string' ? result.translation.trim() : '',
    patientLanguage,
    facts,
    updatedAt: now.toISOString(),
  }]
}

export function summarizeStatements(statements) {
  const summary = Object.fromEntries(HANDOFF_FIELDS.map((key) => [key, []]))
  for (const statement of statements) {
    for (const key of HANDOFF_FIELDS) {
      for (const value of (statement.facts[key] || '').split(', ').filter(Boolean)) {
        if (!summary[key].includes(value)) summary[key].push(value)
      }
    }
  }
  return summary
}
