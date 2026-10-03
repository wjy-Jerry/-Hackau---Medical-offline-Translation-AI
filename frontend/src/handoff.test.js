import test from 'node:test'
import assert from 'node:assert/strict'
import { addPatientStatement, summarizeStatements } from './handoff.js'

test('conversation facts reach the handoff only after explicit add', () => {
  const result = { original_text: 'My chest hurts.', translation: '我胸口疼。',
    key_information: { symptom: 'Chest pain' } }
  const empty = []
  assert.deepEqual(summarizeStatements(empty).allergy, [])
  const saved = addPatientStatement(empty, result, 'en', new Date('2026-10-03T10:42:00Z'))
  assert.equal(saved[0].patientLanguage, 'en')
  assert.equal(saved[0].original, 'My chest hurts.')
  assert.deepEqual(summarizeStatements(saved).symptom, ['Chest pain'])
  assert.deepEqual(summarizeStatements(saved).medication, [])
  assert.deepEqual(empty, [])
  assert.deepEqual(summarizeStatements([]).symptom, [])
})

test('duplicate facts are merged and unknown model fields are excluded', () => {
  const a = { original_text: 'I feel dizzy.', key_information: { other_symptom: 'Dizziness', diagnosis: 'Stroke' } }
  const once = addPatientStatement([], a, 'en')
  const twice = addPatientStatement(once, a, 'en')
  assert.deepEqual(summarizeStatements(twice).other_symptom, ['Dizziness'])
  assert.equal(once[0].facts.diagnosis, undefined)
  assert.equal(addPatientStatement(once, { original_text: '' }, 'en'), once)
})
