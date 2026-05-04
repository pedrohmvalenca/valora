import { createContext, useContext } from "react";

/**
 * STUB — Story 1.7 implementa lógica completa:
 *  - 3 modos: fixed (Coord 1 curso) / restricted (Coord N cursos) / all (Admin)
 *  - Persistência URL ?cursoId=X primeiro, fallback localStorage `valora.cursoIdAtivo`
 *  - Hook useCursoAtual() retorna curso atual + setter
 */
const CursoContext = createContext({
  cursoAtual: null,
  cursos: [],
  setCursoAtual: () => {},
});

export function CursoProvider({ children }) {
  return (
    <CursoContext.Provider value={{ cursoAtual: null, cursos: [], setCursoAtual: () => {} }}>
      {children}
    </CursoContext.Provider>
  );
}

export const useCursoAtual = () => useContext(CursoContext);
