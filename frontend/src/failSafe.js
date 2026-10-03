/** Present a safe, actionable message without exposing model internals. */
export function classifyProcessError(detail = '', status = 0) {
  const message = String(detail).toLowerCase()
  if (/no audio|empty audio|no recording/.test(message)) {
    return { kind: 'no-audio', text: 'No audio captured. Record again.' }
  }
  if (/no speech|recognition|asr/.test(message)) {
    return { kind: 'unclear-speech', text: 'Speech unclear. Please ask the patient to repeat.' }
  }
  if (/unsupported|language pair|translation pair/.test(message)) {
    return { kind: 'unsupported-language', text: 'Unsupported language pair. Choose two available languages.' }
  }
  if (/translation|argos/.test(message)) {
    return { kind: 'translation-unavailable', text: 'Translation unavailable. Ask again or use a Quick Question.' }
  }
  if (status === 0) {
    return { kind: 'backend-unavailable', text: 'Local backend unavailable. Check the device connection.' }
  }
  return { kind: 'processing-failed', text: 'Could not process the recording. Please try again.' }
}
