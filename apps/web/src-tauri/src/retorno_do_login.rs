// Recebe o retorno do login feito no navegador do sistema, num servidor de loopback (RFC 8252, seção 7.3).
// Só repassa a URL ao app web: a conferência do state e a troca do code (PKCE) ficam com o oidc-client-ts
use std::io::{ErrorKind, Read, Write};
use std::net::{Ipv4Addr, TcpListener, TcpStream};
use std::thread;
use std::time::{Duration, Instant};
use tauri::ipc::Channel;

/// Caminho cadastrado no client brewapp-desktop do Keycloak; a porta muda a cada login
const CAMINHO_DO_RETORNO: &str = "/auth/callback";
/// Quem fecha o navegador sem entrar não deixa o servidor escutando para sempre
const TEMPO_MAXIMO_DE_ESPERA: Duration = Duration::from_secs(5 * 60);
const INTERVALO_ENTRE_TENTATIVAS: Duration = Duration::from_millis(100);
const TEMPO_MAXIMO_DE_LEITURA: Duration = Duration::from_secs(5);

/// Escuta numa porta livre de 127.0.0.1 e avisa a porta pelo canal, para o app montar o redirect_uri e abrir o
/// navegador. Devolve a URL do retorno, com code e state, e responde ao navegador com a página recebida
#[tauri::command]
pub async fn receber_retorno_do_login(
  pagina_de_retorno: String,
  ao_escutar: Channel<u16>,
) -> Result<String, String> {
  tauri::async_runtime::spawn_blocking(move || esperar_retorno(&pagina_de_retorno, &ao_escutar))
    .await
    .map_err(|erro| erro.to_string())?
}

fn esperar_retorno(pagina_de_retorno: &str, ao_escutar: &Channel<u16>) -> Result<String, String> {
  let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).map_err(|erro| erro.to_string())?;
  let porta = listener
    .local_addr()
    .map_err(|erro| erro.to_string())?
    .port();
  // Sem bloqueio no accept, para conferir o tempo máximo de espera entre uma tentativa e outra
  listener
    .set_nonblocking(true)
    .map_err(|erro| erro.to_string())?;
  ao_escutar.send(porta).map_err(|erro| erro.to_string())?;

  let limite = Instant::now() + TEMPO_MAXIMO_DE_ESPERA;
  while Instant::now() < limite {
    match listener.accept() {
      Ok((conexao, _)) => {
        if let Some(caminho) = atender(conexao, pagina_de_retorno) {
          return Ok(format!("http://127.0.0.1:{porta}{caminho}"));
        }
      }
      Err(erro) if erro.kind() == ErrorKind::WouldBlock => {
        thread::sleep(INTERVALO_ENTRE_TENTATIVAS)
      }
      Err(erro) => return Err(erro.to_string()),
    }
  }
  Err("Tempo esgotado esperando o retorno do login".to_owned())
}

/// Responde à conexão e devolve o caminho quando ela é o retorno do login; outras (como o favicon) recebem 404
fn atender(mut conexao: TcpStream, pagina_de_retorno: &str) -> Option<String> {
  conexao.set_nonblocking(false).ok()?;
  conexao
    .set_read_timeout(Some(TEMPO_MAXIMO_DE_LEITURA))
    .ok()?;
  let mut buffer = [0u8; 8192];
  let bytes_lidos = conexao.read(&mut buffer).ok()?;
  let requisicao = String::from_utf8_lossy(&buffer[..bytes_lidos]);
  match caminho_do_retorno(&requisicao) {
    Some(caminho) => {
      responder(&mut conexao, "200 OK", pagina_de_retorno);
      Some(caminho.to_owned())
    }
    None => {
      responder(&mut conexao, "404 Not Found", "");
      None
    }
  }
}

/// Caminho com a query, tirado da linha de requisição; só um GET no caminho do retorno conta
fn caminho_do_retorno(requisicao: &str) -> Option<&str> {
  let mut partes = requisicao.lines().next()?.split(' ');
  let (metodo, caminho) = (partes.next()?, partes.next()?);
  let rota = caminho.split('?').next()?;
  (metodo == "GET" && rota == CAMINHO_DO_RETORNO).then_some(caminho)
}

fn responder(conexao: &mut TcpStream, status: &str, corpo: &str) {
  let resposta = format!(
    "HTTP/1.1 {status}\r\nContent-Type: text/html; charset=utf-8\r\n\
     Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'\r\n\
     Content-Length: {}\r\nConnection: close\r\n\r\n{corpo}",
    corpo.len()
  );
  // Falha ao responder não impede o login: o navegador só fica sem a página de aviso
  let _ = conexao.write_all(resposta.as_bytes());
}

#[cfg(test)]
mod tests {
  use super::caminho_do_retorno;

  #[test]
  fn aceita_get_no_caminho_do_retorno_com_a_query() {
    let requisicao =
      "GET /auth/callback?code=abc&state=xyz HTTP/1.1\r\nHost: 127.0.0.1:50000\r\n\r\n";
    assert_eq!(
      caminho_do_retorno(requisicao),
      Some("/auth/callback?code=abc&state=xyz")
    );
  }

  #[test]
  fn recusa_outro_caminho() {
    assert_eq!(
      caminho_do_retorno("GET /favicon.ico HTTP/1.1\r\n\r\n"),
      None
    );
    assert_eq!(
      caminho_do_retorno("GET /auth/callback-falso?code=abc HTTP/1.1\r\n\r\n"),
      None
    );
  }

  #[test]
  fn recusa_outro_metodo() {
    assert_eq!(
      caminho_do_retorno("POST /auth/callback?code=abc HTTP/1.1\r\n\r\n"),
      None
    );
  }

  #[test]
  fn recusa_requisicao_incompleta() {
    assert_eq!(caminho_do_retorno(""), None);
    assert_eq!(caminho_do_retorno("GET"), None);
  }
}
