/** Marca do Textura Studio: um bloco de 2×2 com um quadrante aceso. */
function Mark() {
  return (
    <svg className="brand-mark" viewBox="0 0 20 20" aria-hidden="true">
      <rect x="0.5" y="0.5" width="19" height="19" fill="none" stroke="currentColor" />
      <rect x="4" y="4" width="5" height="5" fill="var(--aqua)" />
      <rect x="11" y="11" width="5" height="5" fill="currentColor" />
    </svg>
  )
}

export function Brand() {
  return (
    <a className="brand" href="#/" title="Galeria">
      <Mark />
      <span className="brand-name">Textura</span>
    </a>
  )
}
