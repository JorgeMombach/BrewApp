// Só a janela com o app web: comandos e plugins nativos entram quando uma história precisar deles
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .run(tauri::generate_context!())
    .expect("Falha ao iniciar a aplicação desktop");
}
