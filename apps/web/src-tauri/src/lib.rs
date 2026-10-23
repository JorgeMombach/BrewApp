// Janela com o app web e o que o login precisa de nativo: navegador do sistema, loopback e cofre (BREW-23)
mod cofre;
mod retorno_do_login;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .plugin(tauri_plugin_opener::init())
    .invoke_handler(tauri::generate_handler![
      cofre::guardar_credenciais,
      cofre::ler_credenciais,
      cofre::apagar_credenciais,
      retorno_do_login::receber_retorno_do_login,
    ])
    .run(tauri::generate_context!())
    .expect("Falha ao iniciar a aplicação desktop");
}
