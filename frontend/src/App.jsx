import React, { useEffect, useRef, useState } from 'react'
import { questions } from './phrasePacks'
import { classifyProcessError } from './failSafe'
import { addPatientStatement, HANDOFF_FIELDS, summarizeStatements } from './handoff'

const languages = { en: 'English', zh: 'Chinese', ru: 'Russian' }
const answerLabels = {
  en: { yes: 'YES', no: 'NO' },
  zh: { yes: '是', no: '不是' },
  ru: { yes: 'ДА', no: 'НЕТ' },
}
const informationLabels = {
  allergy: 'Allergy', medication: 'Medication', symptom: 'Pain location',
  breathing_difficulty: 'Breathing difficulty', bleeding: 'Bleeding',
  loss_of_consciousness: 'Loss of consciousness',
  other_symptom: 'Other stated symptom',
}
const handoffLabels = { ...informationLabels, symptom: 'Pain location' }
const yesNoQuestions = questions.filter((question) => question.id !== 'pain')

function Icon({ name, size = 24 }) {
  const shapes = {
    mark: <><path d="M5 8h14M5 12h9M5 16h14" /><path d="m16 10 3 2-3 2" /></>,
    back: <path d="m15 18-6-6 6-6" />,
    arrow: <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>,
    swap: <><path d="M4 7h16m-4-4 4 4-4 4M20 17H4m4-4-4 4 4 4" /></>,
    question: <><path d="M8 9a4 4 0 0 1 8 0c0 2.5-4 2.5-4 5" /><path d="M12 18h.01" /><circle cx="12" cy="12" r="10" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    mic: <><rect x="9" y="2" width="6" height="12" rx="3" /><path d="M5 10a7 7 0 0 0 14 0M12 17v5m-4 0h8" /></>,
    sound: <><path d="M4 10v4h4l5 4V6l-5 4H4Z" /><path d="M17 9a5 5 0 0 1 0 6M20 6a9 9 0 0 1 0 12" /></>,
    repeat: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.5 9a7 7 0 0 1 12-2L20 12M4 12l2.5 5a7 7 0 0 0 12-2" /></>,
    info: <><circle cx="12" cy="12" r="10" /><path d="M12 11v6m0-10h.01" /></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{shapes[name]}</svg>
}

function StatusBadge({ health }) {
  const ready = health?.ready
  return <div className={`status-badge ${ready ? 'is-ready' : ''}`} role="status">
    <Icon name={ready ? 'check' : 'info'} size={18} />
    <span>{ready ? 'OFFLINE READY' : health ? 'SETUP NEEDED' : 'CHECKING DEVICE'}</span>
  </div>
}

function Header({ health, onBack, title }) {
  return <header className="topbar">
    <div className="topbar-left">
      {onBack && <button className="back-button" type="button" onClick={onBack} aria-label="Back to home"><Icon name="back" /></button>}
      <div className="brand-lockup"><span className="brand-mark"><Icon name="mark" size={22} /></span>
        <span><strong>FieldTalk</strong>{title && <small>{title}</small>}</span></div>
    </div>
    <StatusBadge health={health} />
  </header>
}

function ModeCard({ icon, title, subtitle, featured, onClick }) {
  return <button type="button" className={`mode-card ${featured ? 'featured' : ''}`} onClick={onClick}>
    <span className="mode-icon"><Icon name={icon} size={28} /></span>
    <span className="mode-copy"><strong>{title}</strong><span>{subtitle}</span></span>
    <span className="mode-arrow"><Icon name="arrow" size={22} /></span>
  </button>
}

function Home({ source, target, setSource, setTarget, health, openMode, statementCount }) {
  return <>
    <Header health={health} />
    <main className="home-main">
      <div className="intro"><p className="eyebrow">FIELD COMMUNICATION / LOCAL DEVICE</p>
        <h1>Offline emergency<br />communication.</h1>
        <p>Choose a mode. Keep the patient-facing message clear and direct.</p>
      </div>
      <section className="language-panel" aria-label="Languages">
        <div className="language-field"><label htmlFor="responder-language">RESPONDER LANGUAGE</label>
          <select id="responder-language" value={source} onChange={(event) => {
            const next = event.target.value; setSource(next)
            if (next === target) setTarget(source)
          }}>{Object.entries(languages).map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select></div>
        <button className="swap-button" type="button" aria-label="Swap languages" onClick={() => { setSource(target); setTarget(source) }}><Icon name="swap" /></button>
        <div className="language-field"><label htmlFor="patient-language">PATIENT LANGUAGE</label>
          <select id="patient-language" value={target} onChange={(event) => {
            const next = event.target.value; setTarget(next)
            if (next === source) setSource(target)
          }}>{Object.entries(languages).map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select></div>
      </section>
      <section className="mode-section" aria-label="Communication modes">
        <div className="section-heading"><span className="eyebrow">START HERE</span><span>{languages[source]} → {languages[target]} available</span></div>
        <div className="mode-grid">
          <ModeCard featured icon="question" title="QUICK QUESTIONS" subtitle="Critical predefined questions" onClick={() => openMode('quick')} />
          <ModeCard icon="check" title="YES / NO" subtitle="One question, two large answers" onClick={() => openMode('yesno')} />
          <ModeCard icon="mic" title="FREE CONVERSATION" subtitle="Speech-to-speech translation" onClick={() => openMode('conversation')} />
        </div>
      </section>
      <button type="button" className="handoff-link" onClick={() => openMode('handoff')}>
        <span><strong>VIEW HANDOFF</strong><small>Patient-stated information · {statementCount} {statementCount === 1 ? 'statement' : 'statements'}</small></span>
        <Icon name="arrow" size={22} />
      </button>
      <p className="scope-note"><Icon name="info" size={18} /> For responsive patients. Confirm important details with the patient.</p>
      {(source === 'ru' || target === 'ru') && <p className="review-note"><Icon name="info" size={18} /> Russian wording is pending native-speaker validation.</p>}
    </main>
  </>
}

function QuestionList({ mode, source, onSelect }) {
  const available = mode === 'yesno' ? yesNoQuestions : questions
  return <div className="question-grid">{available.map((question, index) =>
    <button className="question-choice" type="button" key={question.id} onClick={() => onSelect(question)}>
      <span className="question-number">{String(index + 1).padStart(2, '0')}</span>
      <span>{question[source]}</span><Icon name="arrow" size={22} />
    </button>)}</div>
}

function PatientQuestion({ question, source, target, mode, answer, onAnswer, onAnother }) {
  const audioRef = useRef(null)
  const autoPlayed = useRef(false)
  const audioRequest = useRef(`${Date.now()}-${Math.random()}`)
  const [audioState, setAudioState] = useState('preparing')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    autoPlayed.current = false
    setAudioState('preparing')
    return () => audioRef.current?.pause()
  }, [question.id, target, attempt])

  async function playQuestion() {
    const audio = audioRef.current
    if (!audio) return
    try {
      audio.currentTime = 0
      await audio.play()
    } catch (error) {
      setAudioState(error.name === 'NotAllowedError' ? 'tap-to-play' : 'unavailable')
    }
  }

  function repeat() {
    if (audioState === 'unavailable') setAttempt((value) => value + 1)
    else void playQuestion()
  }

  const audioMessage = {
    preparing: 'Preparing local speech…', playing: 'Playing for patient',
    ready: 'Ready to repeat', 'tap-to-play': 'Tap Play to hear the question',
    unavailable: 'Audio unavailable — show the written question',
  }[audioState]

  return <div className="patient-layout">
    <div className="patient-card">
      <div className="patient-card-top"><span className="eyebrow">SHOW TO PATIENT · {languages[target].toUpperCase()}</span>
        <span className={`audio-state ${audioState === 'playing' ? 'is-playing' : ''}`} role="status"><Icon name="sound" size={18} /> {audioMessage}</span></div>
      <p className="patient-question" lang={target}>{question[target]}</p>
      {target === 'ru' && <p className="review-note"><Icon name="info" size={18} /> Russian wording pending native-speaker validation.</p>}
      <audio key={`${question.id}-${target}-${attempt}`} ref={audioRef}
        src={`/quick_question/${question.id}/audio?target_language=${target}&request=${audioRequest.current}-${attempt}`}
        preload="auto" onCanPlay={() => { if (!autoPlayed.current) { autoPlayed.current = true; void playQuestion() } }}
        onPlay={() => setAudioState('playing')} onEnded={() => setAudioState('ready')}
        onError={() => setAudioState('unavailable')} />
      <button type="button" className="primary-button repeat-button" onClick={repeat} disabled={audioState === 'preparing'}>
        <Icon name={audioState === 'unavailable' ? 'repeat' : 'sound'} />
        {audioState === 'unavailable' ? 'RETRY AUDIO' : audioState === 'tap-to-play' ? 'PLAY QUESTION' : 'REPEAT QUESTION'}
      </button>
    </div>
    {mode === 'yesno' && <div className="answer-panel">
      <p className="eyebrow">PATIENT RESPONSE</p>
      <p className="answer-instruction">Ask the patient to tap one answer.</p>
      <div className="answer-grid">{(['yes', 'no']).map((choice) => <button key={choice} type="button"
        className={`answer-button ${answer === choice ? 'is-selected' : ''}`} onClick={() => onAnswer(choice)}
        aria-pressed={answer === choice}>
        <span lang={target}>{answerLabels[target][choice]}</span>
        <small lang={source}>{answerLabels[source][choice]}</small>
      </button>)}</div>
      {answer && <p className="answer-confirmation" role="status"><Icon name="check" size={18} /> {answer.toUpperCase()} selected on this screen.</p>}
    </div>}
    <button type="button" className="text-button another-button" onClick={onAnother}><Icon name="back" size={18} /> ASK ANOTHER QUESTION</button>
  </div>
}

function QuestionsMode({ mode, source, target, health, onHome }) {
  const [selected, setSelected] = useState(null)
  const [answer, setAnswer] = useState(null)
  const title = mode === 'quick' ? 'Quick questions' : 'Yes / No'
  return <>
    <Header health={health} title={title} onBack={onHome} />
    <main className="mode-main">
      {!selected ? <>
        <div className="mode-intro"><p className="eyebrow">{mode === 'quick' ? 'CRITICAL QUESTIONS' : 'ONE QUESTION AT A TIME'}</p>
          <h1>{mode === 'quick' ? 'Choose a question.' : 'Choose a yes / no question.'}</h1>
          <p>Tap once to show the {languages[target]} question to the patient and play it aloud.</p></div>
        {(source === 'ru' || target === 'ru') && <p className="review-note"><Icon name="info" size={18} /> Russian wording pending native-speaker validation.</p>}
        <QuestionList mode={mode} source={source} onSelect={(question) => { setSelected(question); setAnswer(null) }} />
      </> : <PatientQuestion key={selected.id} question={selected} source={source} target={target} mode={mode} answer={answer}
        onAnswer={setAnswer} onAnother={() => { setSelected(null); setAnswer(null) }} />}
    </main>
  </>
}

function Conversation({ source, target, health, onHome, onAddStatement, onViewHandoff }) {
  const [recording, setRecording] = useState(false)
  const [phase, setPhase] = useState('idle')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [errorKind, setErrorKind] = useState('')
  const [audioNotice, setAudioNotice] = useState('')
  const [savedToHandoff, setSavedToHandoff] = useState(false)
  const recorder = useRef(null)
  const recordingRef = useRef(false)
  const startingRef = useRef(false)
  const stream = useRef(null)
  const chunks = useRef([])
  const pressedAt = useRef(null)
  const resultAudio = useRef(null)

  useEffect(() => () => {
    if (recorder.current?.state === 'recording') recorder.current.onstop = null
    recorder.current?.stop?.()
    stream.current?.getTracks().forEach((track) => track.stop())
  }, [])

  useEffect(() => {
    if (!result?.audio_url) return
    const timer = setTimeout(() => {
      resultAudio.current?.play().catch((playError) => {
        setAudioNotice(playError.name === 'NotAllowedError' ? 'Tap Play Translation to hear the audio.' : 'Audio playback failed. The text remains available.')
      })
    }, 80)
    return () => clearTimeout(timer)
  }, [result])

  async function sendAudio(blob, extension) {
    if (!blob.size) { const failure = classifyProcessError('No audio recorded.', 400); setError(failure.text); setErrorKind(failure.kind); setPhase('error'); return }
    setPhase('processing')
    const form = new FormData()
    form.append('audio', blob, `recording.${extension}`)
    form.append('source_language', source)
    form.append('target_language', target)
    try {
      const response = await fetch('/process_audio', { method: 'POST', body: form })
      const data = await response.json().catch(() => { throw new TypeError('Invalid local backend response') })
      if (!response.ok) {
        const failure = classifyProcessError(data?.detail, response.status)
        setError(failure.text); setErrorKind(failure.kind); setPhase('error'); return
      }
      setResult(data)
      setPhase('done')
    } catch (requestError) {
      const failure = classifyProcessError('', 0)
      setError(failure.text); setErrorKind(failure.kind)
      setPhase('error')
    }
  }

  async function beginRecording() {
    if (startingRef.current || recordingRef.current || phase === 'processing') return
    startingRef.current = true
    setError(''); setErrorKind(''); setAudioNotice(''); setResult(null); setSavedToHandoff(false)
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      setError('Audio recording unavailable in this browser. Use Quick Questions or allow microphone access.'); setErrorKind('no-audio'); setPhase('error'); startingRef.current = false; return
    }
    try {
      const media = await navigator.mediaDevices.getUserMedia({ audio: true })
      stream.current = media
      const formats = [['audio/webm;codecs=opus', 'webm'], ['audio/mp4', 'mp4'], ['audio/ogg;codecs=opus', 'ogg']]
      const chosen = formats.find(([mime]) => MediaRecorder.isTypeSupported(mime))
      const instance = chosen ? new MediaRecorder(media, { mimeType: chosen[0] }) : new MediaRecorder(media)
      const extension = chosen?.[1] || (instance.mimeType.includes('mp4') ? 'mp4' : 'webm')
      chunks.current = []
      instance.ondataavailable = (event) => { if (event.data.size) chunks.current.push(event.data) }
      instance.onerror = () => { setError('Recording failed. Please try again.'); setErrorKind('no-audio'); setPhase('error') }
      instance.onstop = () => {
        media.getTracks().forEach((track) => track.stop())
        stream.current = null
        recordingRef.current = false
        setRecording(false)
        void sendAudio(new Blob(chunks.current, { type: instance.mimeType }), extension)
      }
      recorder.current = instance
      instance.start()
      recordingRef.current = true
      setRecording(true)
      setPhase('listening')
    } catch (recordError) {
      stream.current?.getTracks().forEach((track) => track.stop())
      stream.current = null
      setError(recordError.name === 'NotAllowedError' ? 'Microphone permission denied. Allow access and try again.' : `Could not start recording: ${recordError.message}`)
      setErrorKind('no-audio')
      setPhase('error')
    } finally {
      startingRef.current = false
    }
  }

  function finishRecording() {
    if (recorder.current?.state === 'recording') recorder.current.stop()
  }

  function pointerDown(event) {
    if (event.button !== 0 || phase === 'processing') return
    event.currentTarget.setPointerCapture(event.pointerId)
    if (recordingRef.current) { pressedAt.current = null; finishRecording(); return }
    pressedAt.current = performance.now()
    void beginRecording()
  }

  function pointerUp() {
    if (pressedAt.current === null) return
    const held = performance.now() - pressedAt.current > 450
    pressedAt.current = null
    if (held && recordingRef.current) finishRecording()
  }

  function playTranslation() {
    const audio = resultAudio.current
    if (!audio) return
    audio.currentTime = 0
    audio.play().then(() => setAudioNotice('')).catch(() => setAudioNotice('Audio playback failed. The text remains available.'))
  }

  const processing = phase === 'processing'

  return <>
    <Header health={health} title="Free conversation" onBack={recording || processing ? null : onHome} />
    <main className="conversation-main">
      <div className="mode-intro"><p className="eyebrow">SPEECH-TO-SPEECH</p><h1>Speak, then show the result.</h1>
        <p>{languages[source]} to {languages[target]} · Audio stays on this device.</p></div>
      {(source === 'ru' || target === 'ru') && <p className="review-note"><Icon name="info" size={18} /> Russian wording pending native-speaker validation. Confirm critical details with the patient.</p>}
      {!result && <div className="record-panel">
        <button className={`mic-button ${recording ? 'is-listening' : ''}`} type="button"
          aria-label={recording ? 'Finish recording' : 'Hold to speak or tap to start recording'}
          disabled={processing} onPointerDown={pointerDown} onPointerUp={pointerUp} onPointerCancel={pointerUp}
          onKeyDown={(event) => { if ((event.key === 'Enter' || event.key === ' ') && !event.repeat) {
            event.preventDefault(); if (recordingRef.current) finishRecording(); else void beginRecording()
          } }}><Icon name="mic" size={38} /></button>
        <div className="record-copy" aria-live="polite"><strong>{recording ? 'Listening…' : processing ? 'Processing audio…' : 'Hold to speak'}</strong>
          <span>{recording ? 'Release after holding, or tap again to finish.' : processing ? 'Working locally. Keep the device nearby.' : 'Hold and release, or tap once to start and again to finish.'}</span></div>
        {processing && <p className="processing-note" role="status">Recognizing, translating, and preparing speech locally. This may take a moment.</p>}
      </div>}
      {error && <div className={`alert ${errorKind === 'unclear-speech' ? 'alert-danger' : 'alert-neutral'}`} role="alert"><Icon name="info" size={20} />{error}</div>}
      {result && <div className="result-stack" aria-label="Translation result">
        <div className="result-card original-card"><p className="eyebrow">ORIGINAL · {languages[source].toUpperCase()}</p><p className="result-text">{result.original_text}</p></div>
        <div className="result-card translation-card"><p className="eyebrow">TRANSLATION · {languages[target].toUpperCase()}</p>
          <p className="result-text translation-text" lang={target}>{result.translation}</p></div>
        {Object.keys(result.key_information || {}).length > 0 && <div className="information-card">
          <p className="eyebrow">IMPORTANT INFORMATION · PATIENT-STATED</p>
          <ul>{Object.entries(result.key_information).map(([key, value]) =>
            <li key={key}><span>{informationLabels[key] || key}</span><strong>{value}</strong></li>)}</ul>
          <p>Confirm these words with the patient.</p>
        </div>}
        <div className="confidence-row"><span>Recognition confidence</span><strong>{result.confidence == null ? 'Not available' : `${Math.round(result.confidence * 100)}%`}</strong></div>
        {result.warning && <div className={`alert ${result.warning.includes('Low recognition') ? 'alert-danger' : 'alert-neutral'}`} role="alert"><Icon name="info" size={20} />{result.warning}</div>}
        {!result.audio_url && !result.warning?.includes('Speech unavailable') && <div className="alert alert-neutral" role="status"><Icon name="info" size={20} />Audio unavailable. Read the translation on screen.</div>}
        {audioNotice && <div className="alert alert-neutral" role="status"><Icon name="info" size={20} />{audioNotice}</div>}
        {result.audio_url && <audio ref={resultAudio} src={result.audio_url} preload="auto" onError={() => setAudioNotice('Audio playback failed. The text remains available.')} />}
        <div className="handoff-add">
          <p>Only add this result if the patient spoke. Confirm critical details before handoff.</p>
          <button className="secondary-button" type="button" disabled={savedToHandoff} onClick={() => {
            onAddStatement(result, source); setSavedToHandoff(true)
          }}><Icon name="check" /> {savedToHandoff ? 'ADDED TO HANDOFF' : 'ADD PATIENT STATEMENT'}</button>
          {savedToHandoff && <button className="text-button" type="button" onClick={onViewHandoff}>VIEW HANDOFF <Icon name="arrow" size={18} /></button>}
        </div>
        <div className="result-actions"><button className="primary-button" type="button" onClick={playTranslation} disabled={!result.audio_url}><Icon name="sound" /> PLAY TRANSLATION</button>
          <button className="secondary-button" type="button" onClick={() => { setResult(null); setPhase('idle'); setError(''); setErrorKind(''); setAudioNotice(''); setSavedToHandoff(false) }}><Icon name="repeat" /> RECORD AGAIN</button></div>
      </div>}
    </main>
  </>
}

function HandoffCard({ statements, health, onHome, onClear }) {
  const facts = summarizeStatements(statements)
  const latest = statements.at(-1)
  const language = latest?.patientLanguage ? languages[latest.patientLanguage] : 'Unknown / Not stated'
  return <>
    <Header health={health} title="Emergency handoff" onBack={onHome} />
    <main className="handoff-main">
      <div className="mode-intro"><p className="eyebrow">EMERGENCY HANDOFF</p><h1>Patient-stated information.</h1>
        <p>Show this card to the next responder. Confirm every critical detail with the patient.</p></div>
      <section className="handoff-card" aria-label="Patient-stated information">
        <div className="handoff-meta"><span>Patient language</span><strong>{language}</strong></div>
        <div className="handoff-grid">{HANDOFF_FIELDS.map((key) => <div className="handoff-field" key={key}>
          <span>{handoffLabels[key]}</span><strong className={facts[key].length ? '' : 'unknown'}>{facts[key].length ? facts[key].join('; ') : 'Unknown / Not stated'}</strong>
        </div>)}</div>
        <div className="handoff-meta"><span>Last updated</span><strong>{latest ? new Date(latest.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Unknown / Not stated'}</strong></div>
        <p className="handoff-disclaimer">Based on statements manually added on this device. This is a communication aid, not a diagnosis.</p>
      </section>
      <button className="text-button handoff-clear" type="button" disabled={!statements.length} onClick={() => {
        if (window.confirm('Clear all patient-stated information from this session?')) onClear()
      }}>CLEAR SESSION</button>
    </main>
  </>
}

export default function App() {
  const [source, setSource] = useState('en')
  const [target, setTarget] = useState('zh')
  const [screen, setScreen] = useState('home')
  const [health, setHealth] = useState(null)
  const [statements, setStatements] = useState([])

  useEffect(() => {
    fetch('/health').then(async (response) => {
      if (!response.ok) throw new Error('Health check failed')
      setHealth(await response.json())
    }).catch(() => setHealth({ ready: false, components: { backend: 'missing' } }))
  }, [])

  return <div className="app-shell">
    {screen === 'home' && <Home source={source} target={target} setSource={setSource} setTarget={setTarget} health={health} openMode={setScreen} statementCount={statements.length} />}
    {(screen === 'quick' || screen === 'yesno') && <QuestionsMode key={screen} mode={screen} source={source} target={target} health={health} onHome={() => setScreen('home')} />}
    {screen === 'conversation' && <Conversation source={source} target={target} health={health} onHome={() => setScreen('home')}
      onAddStatement={(result, patientLanguage) => setStatements((current) => addPatientStatement(current, result, patientLanguage))}
      onViewHandoff={() => setScreen('handoff')} />}
    {screen === 'handoff' && <HandoffCard statements={statements} health={health} onHome={() => setScreen('home')} onClear={() => setStatements([])} />}
  </div>
}
