import test from 'node:test'
import assert from 'node:assert/strict'
import { classifyProcessError } from './failSafe.js'

test('errors identify a safe next action without model details', () => {
  assert.deepEqual(classifyProcessError('No audio recorded. Please record again.', 400),
    { kind: 'no-audio', text: 'No audio captured. Record again.' })
  assert.equal(classifyProcessError('No speech recognized.', 400).kind, 'unclear-speech')
  assert.equal(classifyProcessError('Speech recognition failed: internal path', 500).kind, 'unclear-speech')
  assert.equal(classifyProcessError('Translation failed: internal path', 500).kind, 'translation-unavailable')
  assert.equal(classifyProcessError('Unsupported language pair', 400).kind, 'unsupported-language')
  assert.equal(classifyProcessError('', 0).kind, 'backend-unavailable')
  assert.equal(classifyProcessError('unknown error', 500).kind, 'processing-failed')
})
