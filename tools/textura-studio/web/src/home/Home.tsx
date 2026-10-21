import { useEffect, useState } from 'react'
import { Plus, RefreshCw } from 'lucide-react'
import { api, type DialogSummary } from '../api'
import { Brand } from '../Brand'
import { navigate } from '../nav'
import { Library } from '../sections/Library'

const ID_PATTERN = /^[a-z0-9_-]{1,64}$/

function NewDialog({ taken, onClose }: { taken: string[]; onClose: () => void }) {
  const [id, setId] = useState('')
  const [base, setBase] = useState('')
  const problem = !id
    ? null
    : !ID_PATTERN.test(id)
      ? 'Use letras minúsculas, números, - e _'
      : taken.includes(id)
        ? 'Já existe uma tela com esse id'
        : null

  const create = () => {
    if (!id || problem) return
    navigate(`#/dialog/${id}?novo${base ? `&de=${base}` : ''}`)
  }

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" onMouseDown={(event) => event.stopPropagation()}>
        <h2>Novo dialog</h2>
        <div className="field">
          <label>Id do arquivo</label>
          <input
            type="text"
            autoFocus
            value={id}
            placeholder="ex.: loja-ferramentas"
            onChange={(event) => setId(event.target.value.trim().toLowerCase())}
            onKeyDown={(event) => event.key === 'Enter' && create()}
          />
          <span className="hint">{problem ?? `Grava em configs/ui/${id || '<id>'}.json quando você salvar.`}</span>
        </div>
        <div className="field">
          <label>Começar de</label>
          <select value={base} onChange={(event) => setBase(event.target.value)}>
            <option value="">Tela em branco</option>
            {taken.map((name) => (
              <option key={name} value={name}>
                Cópia de {name}
              </option>
            ))}
          </select>
        </div>
        <div className="actions">
          <button className="btn ghost" onClick={onClose}>
            Cancelar
          </button>
          <button className="btn primary" disabled={!id || Boolean(problem)} onClick={create}>
            Criar
          </button>
        </div>
      </div>
    </div>
  )
}

export function Home() {
  const [dialogs, setDialogs] = useState<DialogSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [version, setVersion] = useState(() => Date.now())

  const load = () => {
    setError(null)
    api
      .dialogs()
      .then(setDialogs)
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : 'Falha ao listar'))
  }

  useEffect(load, [])

  const reload = async () => {
    await api.reload().catch(() => undefined)
    setVersion(Date.now())
    load()
  }

  return (
    <div className="shell">
      <header className="topbar">
        <Brand />
        <div className="crumbs">
          <span className="top-sep" />
          <span>OceanBlock</span>
          <span>/</span>
          <b>Dialogs</b>
        </div>
        <div className="spacer" />
        <button className="btn ghost" onClick={reload} title="Relê temas, artes e fontes do servidor">
          <RefreshCw /> Recarregar assets
        </button>
        <button className="btn primary" onClick={() => setCreating(true)}>
          <Plus /> Novo dialog
        </button>
      </header>
      <div className="home">
        <Library active="dialogs" />
        <main className="gallery">
          <div className="gallery-head">
            <div>
              <h1>
                Telas do <em>Textura</em>
              </h1>
              <p>
                Cada cartão é um dialog em <code>configs/ui</code>, desenhado pelo próprio motor. Salvar grava direto no servidor; no jogo, <code>/textura reload</code>.
              </p>
            </div>
          </div>
          {error && <div className="notice bad">{error}</div>}
          {!dialogs && !error && <div className="empty">Carregando…</div>}
          {dialogs && (
            <div className="cards">
              {dialogs.map((dialog) => (
                <button key={dialog.id} className="card" onClick={() => navigate(`#/dialog/${dialog.id}`)}>
                  <div className="thumb">
                    <img src={api.thumbUrl(dialog.id, version + dialog.modified)} alt="" loading="lazy" />
                  </div>
                  <div className="info">
                    <b>{dialog.title || dialog.id}</b>
                    <span>
                      <span>{dialog.id}.json</span>
                      <span>{dialog.theme ?? 'sem tema'}</span>
                      {dialog.pages > 0 && <span>{dialog.pages} páginas</span>}
                    </span>
                  </div>
                </button>
              ))}
              <button className="card new" onClick={() => setCreating(true)}>
                <div>
                  <Plus />
                  Novo dialog
                </div>
              </button>
            </div>
          )}
        </main>
      </div>
      {creating && <NewDialog taken={dialogs?.map((dialog) => dialog.id) ?? []} onClose={() => setCreating(false)} />}
    </div>
  )
}
