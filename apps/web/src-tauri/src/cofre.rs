// Credenciais de renovação da sessão no cofre do sistema operacional (spec 9.5): Credential Manager no Windows,
// Keychain no macOS e Secret Service no Linux. O token de acesso nunca vem para cá: fica só na memória do app web
use keyring::Entry;
use serde::{Deserialize, Serialize};

const SERVICO: &str = "dev.brewapp.desktop";
// Uma entrada por token: o Credential Manager limita cada segredo a 2560 bytes, e os dois juntos chegam perto disso
const CONTA_DO_REFRESH_TOKEN: &str = "refresh_token";
const CONTA_DO_ID_TOKEN: &str = "id_token";

/// Mesmo formato do CredenciaisDeRenovacao do app web (src/autenticacao/sessao.ts)
#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CredenciaisDeRenovacao {
  refresh_token: String,
  id_token: String,
}

// Comandos assíncronos: rodam fora da thread principal, e o Secret Service (D-Bus) pode demorar a responder

#[tauri::command]
pub async fn guardar_credenciais(credenciais: CredenciaisDeRenovacao) -> Result<(), String> {
  gravar(CONTA_DO_REFRESH_TOKEN, &credenciais.refresh_token)?;
  gravar(CONTA_DO_ID_TOKEN, &credenciais.id_token)
}

/// Só devolve credenciais quando as duas entradas existem; uma sozinha não serve para renovar
#[tauri::command]
pub async fn ler_credenciais() -> Result<Option<CredenciaisDeRenovacao>, String> {
  let refresh_token = ler(CONTA_DO_REFRESH_TOKEN)?;
  let id_token = ler(CONTA_DO_ID_TOKEN)?;
  Ok(
    refresh_token
      .zip(id_token)
      .map(|(refresh_token, id_token)| CredenciaisDeRenovacao {
        refresh_token,
        id_token,
      }),
  )
}

#[tauri::command]
pub async fn apagar_credenciais() -> Result<(), String> {
  apagar(CONTA_DO_REFRESH_TOKEN)?;
  apagar(CONTA_DO_ID_TOKEN)
}

fn entrada(conta: &str) -> Result<Entry, String> {
  Entry::new(SERVICO, conta).map_err(|erro| erro.to_string())
}

// Bytes UTF-8 (set_secret), e não set_password: no Windows, a senha é gravada em UTF-16, e o limite de 2560 bytes
// cairia para 1280 caracteres, pouco acima do id token do Keycloak
fn gravar(conta: &str, valor: &str) -> Result<(), String> {
  entrada(conta)?
    .set_secret(valor.as_bytes())
    .map_err(|erro| erro.to_string())
}

fn ler(conta: &str) -> Result<Option<String>, String> {
  match entrada(conta)?.get_secret() {
    Ok(bytes) => String::from_utf8(bytes)
      .map(Some)
      .map_err(|erro| erro.to_string()),
    Err(keyring::Error::NoEntry) => Ok(None),
    Err(erro) => Err(erro.to_string()),
  }
}

fn apagar(conta: &str) -> Result<(), String> {
  match entrada(conta)?.delete_credential() {
    Ok(()) | Err(keyring::Error::NoEntry) => Ok(()),
    Err(erro) => Err(erro.to_string()),
  }
}
