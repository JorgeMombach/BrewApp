import { textos } from '../textos';

export function PaginaInicial() {
  return (
    <section>
      <h1>{textos.paginaInicial.titulo}</h1>
      <p>{textos.paginaInicial.descricao}</p>
    </section>
  );
}
