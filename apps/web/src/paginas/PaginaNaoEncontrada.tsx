import { Link } from 'react-router';
import { textos } from '../textos';

export function PaginaNaoEncontrada() {
  return (
    <section>
      <h1>{textos.paginaNaoEncontrada.titulo}</h1>
      <Link to="/">{textos.paginaNaoEncontrada.voltarAoInicio}</Link>
    </section>
  );
}
